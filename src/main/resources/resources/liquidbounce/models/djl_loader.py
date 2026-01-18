#!/usr/bin/env python3
import struct
import numpy as np

def load_djl_params(filename):
    """Load parameters from DJL .params file"""
    params = {}
    
    with open(filename, 'rb') as f:
        # Read header
        magic = f.read(4)
        if magic != b'DJL@':
            raise ValueError("Invalid DJL file")
        
        version = struct.unpack('>I', f.read(4))[0]
        format_len = struct.unpack('>H', f.read(2))[0]
        format_name = f.read(format_len).decode('utf-8')
        
        print(f"DJL version: {version}, format: {format_name}")
        
        # Read rest to find arrays
        content = f.read()
        
    return params

if __name__ == "__main__":
    import sys
    params = load_djl_params(sys.argv[1])
    
