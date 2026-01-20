#!/usr/bin/env python3
"""
DJL Parameter Loader

Parses DJL .params files and extracts trained model weights.
"""

import struct
import numpy as np
from collections import OrderedDict


def read_string(f):
    """Read a length-prefixed UTF-8 string"""
    length = struct.unpack('>H', f.read(2))[0]
    if length == 0:
        return ""
    return f.read(length).decode('utf-8')


def read_ndarray(f):
    """Read an NDArray from DJL format"""
    # Read array header
    header = f.read(4).decode('ascii')
    if header != 'NDAR':
        raise ValueError(f"Expected NDAR header, got {header}")

    # Read version
    version = struct.unpack('>I', f.read(4))[0]

    # Read flags (sparse/dense)
    flags = struct.unpack('B', f.read(1))[0]

    # Read name
    name = read_string(f)

    # Read encoding type
    encoding_len = struct.unpack('>H', f.read(2))[0]
    encoding = f.read(encoding_len).decode('utf-8')

    # Read dtype
    dtype_len = struct.unpack('>H', f.read(2))[0]
    dtype_str = f.read(dtype_len).decode('utf-8')

    # Map DJL dtype to numpy
    dtype_map = {
        'FLOAT32': np.float32,
        'FLOAT64': np.float64,
        'INT32': np.int32,
        'INT64': np.int64,
    }
    dtype = dtype_map.get(dtype_str, np.float32)

    # Read shape
    ndim = struct.unpack('>I', f.read(4))[0]
    shape = []
    for _ in range(ndim):
        dim = struct.unpack('>q', f.read(8))[0]  # signed long
        shape.append(dim)

    # Skip some metadata bytes
    f.read(6)  # Unknown metadata

    # Read actual data
    num_elements = int(np.prod(shape))
    data = np.fromfile(f, dtype=dtype, count=num_elements)

    return name, data.reshape(shape)


def load_djl_params(filename):
    """
    Load parameters from a DJL .params file.

    Returns:
        OrderedDict mapping parameter names to numpy arrays
    """
    params = OrderedDict()

    with open(filename, 'rb') as f:
        # Read magic bytes
        magic = f.read(4).decode('ascii')
        if magic != 'DJL@':
            raise ValueError(f"Invalid magic bytes: {magic}")

        # Read version
        version = struct.unpack('>I', f.read(4))[0]
        print(f"DJL version: {version}")

        # Read format name length and name
        format_len = struct.unpack('>H', f.read(2))[0]
        format_name = f.read(format_len).decode('utf-8')
        print(f"Format: {format_name}")

        # Try to read all NDArrays
        param_count = 0
        while True:
            try:
                # Look for NDAR marker
                pos = f.tell()
                chunk = f.read(4)
                if len(chunk) < 4:
                    break

                if chunk.decode('ascii', errors='ignore') == 'NDAR':
                    # Found an array, go back and read it
                    f.seek(pos)
                    name, array = read_ndarray(f)
                    params[name] = array
                    param_count += 1
                    print(f"Loaded parameter '{name}': shape {array.shape}, dtype {array.dtype}")
                else:
                    # Not an array marker, continue searching
                    f.seek(pos + 1)
            except Exception as e:
                # Reached end or encountered error
                break

        print(f"\nTotal parameters loaded: {param_count}")

    return params


def djl_to_pytorch_state_dict(djl_params, model_structure):
    """
    Convert DJL parameters to PyTorch state dict format.

    Args:
        djl_params: OrderedDict from load_djl_params()
        model_structure: List of layer specs like [('0.weight', [128, 4]), ('0.bias', [128]), ...]

    Returns:
        PyTorch state dict
    """
    import torch

    state_dict = OrderedDict()

    # DJL stores parameters with names like 'weight', 'bias', etc.
    # We need to map them to PyTorch's '0.weight', '1.weight', etc.

    djl_weights = [v for k, v in djl_params.items() if 'weight' in k.lower()]
    djl_biases = [v for k, v in djl_params.items() if 'bias' in k.lower()]

    weight_idx = 0
    bias_idx = 0

    for param_name, expected_shape in model_structure:
        if 'weight' in param_name:
            if weight_idx < len(djl_weights):
                param = djl_weights[weight_idx]
                # Transpose if needed (DJL vs PyTorch convention)
                if len(param.shape) == 2 and param.shape != tuple(expected_shape):
                    param = param.T
                state_dict[param_name] = torch.from_numpy(param)
                weight_idx += 1
        elif 'bias' in param_name:
            if bias_idx < len(djl_biases):
                state_dict[param_name] = torch.from_numpy(djl_biases[bias_idx])
                bias_idx += 1
        elif 'running_mean' in param_name or 'running_var' in param_name:
            # BatchNorm running stats - initialize with defaults
            state_dict[param_name] = torch.zeros(expected_shape) if 'mean' in param_name else torch.ones(expected_shape)
        elif 'num_batches_tracked' in param_name:
            state_dict[param_name] = torch.tensor(0)

    return state_dict


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Usage: python djl_loader.py <params_file>")
        sys.exit(1)

    params = load_djl_params(sys.argv[1])

    print("\n" + "="*60)
    print("Parameter Summary:")
    print("="*60)
    for name, array in params.items():
        print(f"{name:20s} : shape={str(array.shape):15s} dtype={array.dtype}")
