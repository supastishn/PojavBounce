#!/usr/bin/env python3
"""Parse DJL params dumps heuristically and print parameter names, shapes, and first values.

This is a heuristic parser used for CI debugging. It is not a complete, robust DJL params parser,
but it should be sufficient to extract weight/bias/gamma/beta arrays from the provided dumps to
allow constructing an equivalent PyTorch model for ONNX export.
"""

import sys
import re
import struct
from pathlib import Path

KNOWN_NAMES = [b'weight', b'bias', b'gamma', b'beta', b'runningMean', b'runningVar']


def find_nearest_ascii_before(data, idx, max_back=128):
    # scan backward to find a readable ascii token
    start = max(0, idx - max_back)
    chunk = data[start:idx]
    # find last run of ascii letters/digits/_ before idx
    m = re.search(rb'([A-Za-z0-9_]{3,64})\x00?\Z', chunk)
    if m:
        return (start + m.start(1), start + m.end(1), m.group(1))
    return None


def find_next_marker(data, idx):
    # find index of next known marker (NDAR, DENSE, known names) or EOF
    markers = [b'NDAR', b'DENSE', b'FLOAT32'] + KNOWN_NAMES
    minpos = len(data)
    for mk in markers:
        p = data.find(mk, idx)
        if p != -1 and p < minpos:
            minpos = p
    return minpos


def extract_arrays(path: Path):
    data = path.read_bytes()
    arrays = []
    for m in re.finditer(b'DENSE', data):
        pos = m.start()
        # find nearest ascii name before this DENSE marker (like 'weight' or 'bias')
        name_info = find_nearest_ascii_before(data, pos)
        name = None
        if name_info:
            name = name_info[2].decode('utf-8', errors='ignore')
        # locate FLOAT32 after this marker
        fpos = data.find(b'FLOAT32', pos)
        if fpos == -1:
            continue
        # assume numeric region begins after FLOAT32; skip a few bytes of small header
        start = fpos + len(b'FLOAT32')
        # find next marker (name or next DENSE) to mark end
        end = find_next_marker(data, start)
        # sometimes there is a small header; scan forward until we find a plausible float
        # look for alignment to 4 bytes and that interpreting next 4 bytes as float yields finite value
        found = False
        for offset in range(start, min(start + 64, end)):
            if (end - offset) % 4 == 0 and offset + 4 <= len(data):
                val = struct.unpack('<f', data[offset:offset+4])[0]
                if abs(val) < 1e38:  # finite
                    # assume this is start of float array
                    start = offset
                    found = True
                    break
        if not found:
            # fallback: use start as-is
            start = start
        raw = data[start:end]
        if len(raw) % 4 != 0:
            # trim trailing bytes to multiple of 4
            raw = raw[:len(raw) - (len(raw) % 4)]
        count = len(raw) // 4
        if count == 0:
            arrays.append((name, None, 0, None))
            continue
        vals = struct.unpack('<' + 'f'*count, raw)
        arrays.append((name, start, count, vals[:8]))
    return arrays


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('Usage: parse_params_dump.py <dump-file>')
        sys.exit(1)
    path = Path(sys.argv[1])
    if not path.exists():
        print('File not found:', path)
        sys.exit(1)
    arrs = extract_arrays(path)
    for i, (name, start, count, head) in enumerate(arrs):
        print(f'[{i}] name={name!s} start={start} count={count} head={head}')
    print('Found', len(arrs), 'array blobs (heuristic).')
