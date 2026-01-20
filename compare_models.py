#!/usr/bin/env python3
"""
Compare original .params MXNet models with converted PyTorch models
Verifies accuracy and consistency of conversion
"""

import torch
import mxnet as mx
import numpy as np
from pathlib import Path
import json

def load_mxnet_model(symbol_file, params_file):
    """Load MXNet model"""
    try:
        # Load symbol
        symbol = mx.symbol.load(symbol_file)
        # Load parameters
        _, arg_params, aux_params = mx.model.load_checkpoint(params_file.replace('.params', ''), 0)
        return symbol, arg_params, aux_params
    except Exception as e:
        print(f"Error loading MXNet model: {e}")
        return None, None, None

def create_test_inputs(num_samples=100):
    """Create random test inputs"""
    return np.random.randn(num_samples, 4).astype(np.float32)

def inference_mxnet(symbol, arg_params, aux_params, inputs):
    """Run inference with MXNet model"""
    try:
        # Create executor
        executor = symbol.simple_bind(
            ctx=mx.cpu(),
            data=inputs.shape,
            grad_req='null'
        )

        # Set parameters
        executor.arg_dict['data'][:] = inputs
        for name, param in arg_params.items():
            if name in executor.arg_dict:
                executor.arg_dict[name][:] = param

        for name, param in aux_params.items():
            if name in executor.aux_dict:
                executor.aux_dict[name][:] = param

        # Forward pass
        executor.forward()
        return executor.outputs[0].asnumpy()
    except Exception as e:
        print(f"Error in MXNet inference: {e}")
        return None

def inference_pytorch(model_path, inputs):
    """Run inference with PyTorch model"""
    try:
        model = torch.jit.load(model_path)
        model.eval()

        # Convert numpy to torch
        inputs_torch = torch.from_numpy(inputs).float()

        # Run inference
        with torch.no_grad():
            outputs = model(inputs_torch)

        return outputs.numpy()
    except Exception as e:
        print(f"Error in PyTorch inference: {e}")
        return None

def compare_models():
    """Main comparison function"""
    print("\n" + "=" * 80)
    print("MXNET .params vs PyTorch .pt MODEL COMPARISON")
    print("=" * 80)

    # Model pairs to compare
    models = [
        {
            'name': '21KC11KP',
            'params': 'src/main/resources/resources/liquidbounce/models/21kc11kp.params',
            'pt': 'converted_models/21kc11kp.pt'
        },
        {
            'name': '19KC8KP',
            'params': 'src/main/resources/resources/liquidbounce/models/19kc8kp.params',
            'pt': 'converted_models/19kc8kp.pt'
        }
    ]

    # Generate test inputs
    print("\nGenerating test inputs...")
    test_inputs = create_test_inputs(num_samples=100)
    print(f"✓ Generated {len(test_inputs)} test samples with shape {test_inputs.shape}")

    # Compare each model
    results = {}

    for model_info in models:
        model_name = model_info['name']
        print(f"\n" + "-" * 80)
        print(f"Comparing {model_name}")
        print("-" * 80)

        params_file = model_info['params']
        pt_file = model_info['pt']

        # Check file existence
        if not Path(params_file).exists():
            print(f"❌ .params file not found: {params_file}")
            results[model_name] = {'error': 'params file not found'}
            continue

        if not Path(pt_file).exists():
            print(f"❌ .pt file not found: {pt_file}")
            results[model_name] = {'error': 'pt file not found'}
            continue

        print(f"✓ .params file: {params_file}")
        print(f"✓ .pt file: {pt_file}")

        # Try MXNet inference
        print(f"\nLoading MXNet model...")
        symbol, arg_params, aux_params = load_mxnet_model(
            params_file.replace('.params', '.symbol'),
            params_file
        )

        if symbol is None:
            print(f"⚠️  Could not load MXNet model - likely due to missing .symbol file")
            print(f"    Note: MXNet models require both .symbol and .params files")
            print(f"    The .symbol file defines the network architecture")
            results[model_name] = {'error': 'symbol file not found'}
            continue

        print(f"✓ MXNet model loaded")

        # Run MXNet inference
        print(f"Running MXNet inference...")
        mxnet_outputs = inference_mxnet(symbol, arg_params, aux_params, test_inputs)

        if mxnet_outputs is None:
            print(f"❌ MXNet inference failed")
            results[model_name] = {'error': 'mxnet inference failed'}
            continue

        print(f"✓ MXNet output shape: {mxnet_outputs.shape}")
        print(f"  Min: {mxnet_outputs.min():.6f}, Max: {mxnet_outputs.max():.6f}")
        print(f"  Mean: {mxnet_outputs.mean():.6f}, Std: {mxnet_outputs.std():.6f}")

        # Run PyTorch inference
        print(f"\nRunning PyTorch inference...")
        pytorch_outputs = inference_pytorch(pt_file, test_inputs)

        if pytorch_outputs is None:
            print(f"❌ PyTorch inference failed")
            results[model_name] = {'error': 'pytorch inference failed'}
            continue

        print(f"✓ PyTorch output shape: {pytorch_outputs.shape}")
        print(f"  Min: {pytorch_outputs.min():.6f}, Max: {pytorch_outputs.max():.6f}")
        print(f"  Mean: {pytorch_outputs.mean():.6f}, Std: {pytorch_outputs.std():.6f}")

        # Compare outputs
        print(f"\nComparing outputs...")

        if mxnet_outputs.shape != pytorch_outputs.shape:
            print(f"❌ Output shapes don't match: {mxnet_outputs.shape} vs {pytorch_outputs.shape}")
            results[model_name] = {
                'error': 'shape mismatch',
                'mxnet_shape': mxnet_outputs.shape,
                'pytorch_shape': pytorch_outputs.shape
            }
            continue

        # Calculate differences
        diff = np.abs(mxnet_outputs - pytorch_outputs)
        abs_diff_mean = diff.mean()
        abs_diff_max = diff.max()
        abs_diff_min = diff.min()
        rel_diff_mean = np.abs((mxnet_outputs - pytorch_outputs) / (np.abs(mxnet_outputs) + 1e-8)).mean()

        print(f"✓ Shapes match: {mxnet_outputs.shape}")
        print(f"\nDifference Statistics:")
        print(f"  Absolute diff - Min: {abs_diff_min:.8f}")
        print(f"  Absolute diff - Mean: {abs_diff_mean:.8f}")
        print(f"  Absolute diff - Max: {abs_diff_max:.8f}")
        print(f"  Relative diff - Mean: {rel_diff_mean:.8f}")

        # Assess compatibility
        if abs_diff_max < 0.1:
            status = "✅ EXCELLENT - Nearly identical"
        elif abs_diff_max < 1.0:
            status = "✅ GOOD - Minor differences"
        elif abs_diff_max < 10.0:
            status = "⚠️  MODERATE - Noticeable differences"
        else:
            status = "❌ POOR - Significant differences"

        print(f"\nCompatibility Assessment: {status}")

        results[model_name] = {
            'success': True,
            'mxnet_shape': mxnet_outputs.shape,
            'pytorch_shape': pytorch_outputs.shape,
            'abs_diff_mean': float(abs_diff_mean),
            'abs_diff_max': float(abs_diff_max),
            'rel_diff_mean': float(rel_diff_mean),
            'status': status
        }

    # Summary
    print(f"\n" + "=" * 80)
    print("COMPARISON SUMMARY")
    print("=" * 80)

    for model_name, result in results.items():
        print(f"\n{model_name}:")
        if 'error' in result:
            print(f"  Status: ❌ Error - {result['error']}")
        else:
            if result.get('success'):
                print(f"  Status: {result.get('status', 'N/A')}")
                print(f"  Output shape: {result['mxnet_shape']}")
                print(f"  Abs diff mean: {result['abs_diff_mean']:.8f}")
                print(f"  Abs diff max: {result['abs_diff_max']:.8f}")

    # Save results
    with open('model_comparison_results.json', 'w') as f:
        json.dump(results, f, indent=2)
    print(f"\n✓ Results saved to: model_comparison_results.json")

    print("\n" + "=" * 80)
    print("NOTE: .symbol files are required to load MXNet models")
    print(".symbol files should be at:")
    print("  - src/main/resources/resources/liquidbounce/models/21kc11kp.symbol")
    print("  - src/main/resources/resources/liquidbounce/models/19kc8kp.symbol")
    print("=" * 80 + "\n")

if __name__ == '__main__':
    compare_models()
