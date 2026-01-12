#!/usr/bin/env python3
"""Extract parameter blobs by searching for known parameter name tokens and decoding following FLOAT32 blobs.

Heuristic parser that matches token -> DENSE -> FLOAT32 -> data until next token.
"""

import sys
import re
import struct
from pathlib import Path

KNOWN = [b'weight', b'bias', b'gamma', b'beta', b'runningMean', b'runningVar']


def find_tokens(data):
    tokens = []
    for pat in KNOWN:
        for m in re.finditer(re.escape(pat), data):
            tokens.append((m.start(), pat.decode()))
    tokens.sort()
    return tokens


def extract_by_tokens(path: Path):
    data = path.read_bytes()
    tokens = find_tokens(data)
    entries = []
    for i, (pos, name) in enumerate(tokens):
        # find DENSE after this position
        dense_pos = data.find(b'DENSE', pos)
        if dense_pos == -1:
            continue
        float_pos = data.find(b'FLOAT32', dense_pos)
        if float_pos == -1:
            continue
        start = float_pos + len(b'FLOAT32')
        # Find next token ahead or next DENSE marker
        next_pos_candidates = [data.find(b'DENSE', start+1), data.find(b'FLOAT32', start+1)]
        # find next known param token beyond start
        next_token_pos = min([p for p, _ in tokens[i+1:]] + [len(data)]) if i+1 < len(tokens) else len(data)
        # choose end as min between next_token_pos and next DENSE marker
        end = next_token_pos
        # but ensure end > start
        if end <= start:
            end = len(data)
        raw = data[start:end]
        # trim to multiple of 4
        raw = raw[:len(raw) - (len(raw) % 4)]
        count = len(raw) // 4
        vals = None
        if count > 0:
            vals = struct.unpack('<' + 'f'*count, raw)
        entries.append((name, pos, start, end, count, vals[:8] if vals else None))
    return entries


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('Usage: parse_params_dump_by_name.py <dump-file>')
        sys.exit(1)
    path = Path(sys.argv[1])
    if not path.exists():
        print('File not found:', path)
        sys.exit(1)
    entries = extract_by_tokens(path)
    for idx, (name, pos, start, end, count, head) in enumerate(entries):
        print(f'[{idx}] name={name} pos={pos} data_start={start} data_end={end} count={count} head={head}')
    print('Found', len(entries), 'entries (token-based).')
