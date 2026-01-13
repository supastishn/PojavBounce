#!/usr/bin/env python3
"""Convert DJL params dump files (heuristically parsed) into an ONNX model for the MLP architecture.

This script is heuristic and targets the Minarai MLP architecture used in the repo:
layers: Linear(?,128) -> BatchNorm -> ReLU -> Linear(128,64) -> BN -> ReLU -> Linear(64,32) -> BN -> ReLU -> Linear(32,outputs)

Steps:
- Parse params dump (binary) for tokens (weight,bias,gamma,beta,runningMean,runningVar)
- Group tokens into layer chunks and pick the smaller float array for each token (heuristic)
- Infer input dimension from first weight array length and known output units (128)
- Build ONNX graph programmatically using initializers and ops (MatMul, Add, BatchNormalization, Relu)
- Generate ONNX model with IR version 9 and opset 9 for Android compatibility

This is a best-effort converter for inclusion in CI; if it fails we will iterate.
"""

import sys
import struct
import re
from pathlib import Path
import onnx
import onnx.helper as helper
import onnx.numpy_helper as numpy_helper
import numpy as np

KNOWN = [b'weight', b'bias', b'gamma', b'beta', b'runningMean', b'runningVar']


def find_tokens(data):
    tokens = []
    for pat in KNOWN:
        for m in re.finditer(re.escape(pat), data):
            tokens.append((m.start(), pat.decode()))
    tokens.sort()
    return tokens


def extract_entries(data):
    tokens = find_tokens(data)
    entries = []
    for i, (pos, name) in enumerate(tokens):
        dense_pos = data.find(b'DENSE', pos)
        if dense_pos == -1:
            continue
        float_pos = data.find(b'FLOAT32', dense_pos)
        if float_pos == -1:
            continue
        start = float_pos + len(b'FLOAT32')
        # next token pos
        next_token_pos = tokens[i+1][0] if i+1 < len(tokens) else len(data)
        # crude end bound
        end = next_token_pos
        raw = data[start:end]
        raw = raw[:len(raw) - (len(raw) % 4)]
        count = len(raw) // 4
        vals = None
        if count > 0:
            vals = np.frombuffer(raw, dtype=np.float32).copy()
        entries.append({'name': name, 'pos': pos, 'count': count, 'vals': vals})
    return entries


def group_layers(entries):
    # group by encountering 'weight' tokens as layer starts
    layers = []
    cur = None
    for e in entries:
        if e['name'] == 'weight':
            if cur is not None:
                layers.append(cur)
            cur = {'weight': [], 'bias': [], 'gamma': [], 'beta': [], 'runningMean': [], 'runningVar': []}
            cur['weight'].append(e)
        else:
            if cur is None:
                # skip leading entries
                continue
            cur[e['name']].append(e)
    if cur is not None:
        layers.append(cur)
    return layers


def pick_best(arr_list):
    # choose the smallest nonzero array as the most likely correct atomic param
    best = None
    for a in arr_list:
        if a['vals'] is None:
            continue
        if best is None or a['count'] < best['count']:
            best = a
    return best


def build_onnx(layers, outputs, out_path):
    # Filter out empty placeholder layers which can appear when a 'weight' token
    # is present but no following FLOAT32 data block was found. Keep only layers
    # that contain a usable weight and bias array.
    original_count = len(layers)
    layers = [layer for layer in layers if pick_best(layer['weight']) is not None and pick_best(layer['bias']) is not None]
    filtered = original_count - len(layers)
    if filtered > 0:
        print(f'Filtered out {filtered} empty layer placeholder(s)')

    if not layers:
        raise RuntimeError('no usable layers found after filtering empty placeholders')

    # infer input dimension from first layer weight
    first_w = pick_best(layers[0]['weight'])
    if first_w is None:
        raise RuntimeError('first layer weight not found')
    # first_w.vals length should be input_dim * 128 (approx)
    wlen = first_w['count']
    out1 = 128
    if wlen % out1 != 0:
        # try the larger weight if this fails
        pass
    in_dim = wlen // out1
    print('Inferred input dim', in_dim)

    # create graph
    nodes = []
    initializers = []
    inputs = [helper.make_tensor_value_info('input', onnx.TensorProto.FLOAT, ['batch', in_dim])]
    prev = 'input'
    node_count = 0

    for idx, layer in enumerate(layers):
        layer_name = f'layer{idx+1}'
        # Choose weight and bias entries more robustly by matching counts.
        w_candidates = [a for a in layer['weight'] if a['vals'] is not None]
        b_candidates = [a for a in layer['bias'] if a['vals'] is not None]
        if not w_candidates or not b_candidates:
            print('Missing weight or bias for layer', idx+1)
            continue
        # Choose a bias entry that is consistent with batch-norm params when possible.
        bn_entries = {k: pick_best(layer[k]) for k in ('gamma','beta','runningMean','runningVar')}
        bn_counts = [e['count'] for e in bn_entries.values() if e is not None]
        if bn_counts:
            # pick the most common count among BN params and prefer it as chosen_units
            from collections import Counter
            mode_count = Counter(bn_counts).most_common(1)[0][0]
            chosen_units = mode_count
            # find the bias candidate closest to mode_count
            b_entry = min(b_candidates, key=lambda bc: abs(bc['count'] - mode_count))
            # If bias is smaller than the mode, we can't reconcile; skip layer
            if b_entry['count'] < chosen_units:
                print(f"Bias count {b_entry['count']} smaller than BN mode {chosen_units}; skipping layer {idx+1}")
                continue
            # Trim bias down if it's larger than chosen_units
            if b_entry['count'] > chosen_units:
                trim = b_entry['count'] - chosen_units
                print(f"Trimming bias from {b_entry['count']} to {chosen_units} (trim {trim}) to match BN mode)")
                b_entry['vals'] = b_entry['vals'][:chosen_units]
                b_entry['count'] = chosen_units
        else:
            b_entry = pick_best(b_candidates)

        # Now pick the best matching weight candidate that is divisible by the chosen bias length
        best_w = None
        best_rem = None
        for w in w_candidates:
            if b_entry['count'] > 0:
                rem = w['count'] % b_entry['count']
            else:
                rem = w['count']
            if best_w is None or rem < best_rem:
                best_w = w
                best_rem = rem
        w_entry = best_w
        chosen_units = b_entry['count']
        in_dim_calc = w_entry['count'] // chosen_units
        if best_rem != 0:
            trim = w_entry['count'] - (in_dim_calc * chosen_units)
            print(f"Warning: weight length {w_entry['count']} not divisible by bias length {chosen_units}; trimming {trim} trailing floats")
            w_entry['vals'] = w_entry['vals'][:in_dim_calc * chosen_units]
            w_entry['count'] = len(w_entry['vals'])

        # reshape weight to (in_dim_calc, chosen_units)
        W = w_entry['vals'].reshape((in_dim_calc, chosen_units))
        B = b_entry['vals'].reshape((chosen_units,))

        W_name = f'{layer_name}_W'
        B_name = f'{layer_name}_B'
        initializers.append(numpy_helper.from_array(W.astype(np.float32), name=W_name))
        initializers.append(numpy_helper.from_array(B.astype(np.float32), name=B_name))

        # MatMul then Add
        mat_out = f'{layer_name}_mm'
        add_out = f'{layer_name}_preact'
        nodes.append(helper.make_node('MatMul', [prev, W_name], [mat_out], name=f'MatMul_{layer_name}'))
        nodes.append(helper.make_node('Add', [mat_out, B_name], [add_out], name=f'Add_{layer_name}'))

        # BatchNorm if present
        gamma_entry = pick_best(layer['gamma'])
        beta_entry = pick_best(layer['beta'])
        mean_entry = pick_best(layer['runningMean'])
        var_entry = pick_best(layer['runningVar'])

        # Normalize/truncate BN arrays to chosen_units when possible, otherwise skip BN
        do_bn = True
        for name, entry in (('gamma', gamma_entry), ('beta', beta_entry), ('runningMean', mean_entry), ('runningVar', var_entry)):
            if entry is None or entry['vals'] is None:
                do_bn = False
                break
            if entry['count'] < chosen_units:
                print(f"BN param {name} has {entry['count']} < chosen_units {chosen_units}; skipping BN for layer {idx+1}")
                do_bn = False
                break
            if entry['count'] > chosen_units:
                trim = entry['count'] - chosen_units
                print(f"Trimming BN param {name} from {entry['count']} to {chosen_units} (trim {trim})")
                entry['vals'] = entry['vals'][:chosen_units]
                entry['count'] = chosen_units

        if do_bn:
            gamma = gamma_entry['vals'].reshape((chosen_units,))
            beta = beta_entry['vals'].reshape((chosen_units,))
            mean = mean_entry['vals'].reshape((chosen_units,))
            var = var_entry['vals'].reshape((chosen_units,))
            g_name = f'{layer_name}_gamma'
            be_name = f'{layer_name}_beta'
            me_name = f'{layer_name}_mean'
            va_name = f'{layer_name}_var'
            initializers.append(numpy_helper.from_array(gamma.astype(np.float32), name=g_name))
            initializers.append(numpy_helper.from_array(beta.astype(np.float32), name=be_name))
            initializers.append(numpy_helper.from_array(mean.astype(np.float32), name=me_name))
            initializers.append(numpy_helper.from_array(var.astype(np.float32), name=va_name))
            bn_out = f'{layer_name}_bn'
            nodes.append(helper.make_node('BatchNormalization', [add_out, g_name, be_name, me_name, va_name], [bn_out], name=f'BN_{layer_name}', epsilon=1e-5))
            act_in = bn_out
        else:
            act_in = add_out
        # Relu except after last layer
        if idx < len(layers) - 1:
            relu_out = f'{layer_name}_relu'
            nodes.append(helper.make_node('Relu', [act_in], [relu_out], name=f'Relu_{layer_name}'))
            prev = relu_out
        else:
            prev = act_in

        # set input dim for next layer
        in_dim = chosen_units

    outputs_info = [helper.make_tensor_value_info('output', onnx.TensorProto.FLOAT, ['batch', outputs])]

    # final node to alias prev to output if necessary
    nodes.append(helper.make_node('Identity', [prev], ['output'], name='Output'))

    graph = helper.make_graph(nodes, 'mlp_export', inputs, outputs_info, initializer=initializers)
    # Create model with IR version 9 and opset 9 for maximum compatibility with ONNX Runtime on Android
    model = helper.make_model(graph, ir_version=9, opset_imports=[helper.make_opsetid("", 9)])
    
    # Verify IR version and opset
    print(f'Model IR version: {model.ir_version}, Opset: {model.opset_import[0].version}')
    
    onnx.save(model, out_path)
    print('Written ONNX', out_path)


if __name__ == '__main__':
    if len(sys.argv) < 4:
        print('Usage: params_to_onnx.py <dump-file> <outputs> <out-onnx>')
        sys.exit(1)
    dump = Path(sys.argv[1])
    outputs = int(sys.argv[2])
    out = Path(sys.argv[3])
    data = dump.read_bytes()
    entries = extract_entries(data)
    # group and build
    layers = group_layers(entries)
    print('Found', len(layers), 'layer groups')
    build_onnx(layers, outputs, out)
