#!/usr/bin/env python3
"""
ExecuTorch Model Converter with Enhanced Logging
Converts TorchScript (.pt) models to ExecuTorch (.pte) format with comprehensive logging
"""

import torch
import os
import sys
import logging
from pathlib import Path
from datetime import datetime

# Configure logging
log_dir = "./conversion_logs"
os.makedirs(log_dir, exist_ok=True)

# Create logger
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

# File handler
log_file = os.path.join(log_dir, f"conversion_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log")
fh = logging.FileHandler(log_file)
fh.setLevel(logging.DEBUG)

# Console handler
ch = logging.StreamHandler(sys.stdout)
ch.setLevel(logging.INFO)

# Formatter
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
fh.setFormatter(formatter)
ch.setFormatter(formatter)

logger.addHandler(fh)
logger.addHandler(ch)

try:
    from executorch.exir import to_edge
    from executorch.backends.xnnpack import partition_for_xnnpack
    EXECUTORCH_AVAILABLE = True
    logger.info("✓ ExecuTorch is available and imported successfully")
except ImportError as e:
    EXECUTORCH_AVAILABLE = False
    logger.error(f"⚠️  ExecutorTorch not installed: {e}")
    logger.error("Install with: pip install executorch")

def log_system_info():
    """Log system and environment information"""
    logger.info("="*80)
    logger.info("SYSTEM INFORMATION")
    logger.info("="*80)
    logger.info(f"Python version: {sys.version}")
    logger.info(f"PyTorch version: {torch.__version__}")
    logger.info(f"CUDA available: {torch.cuda.is_available()}")
    if torch.cuda.is_available():
        logger.info(f"CUDA version: {torch.version.cuda}")
    logger.info(f"Working directory: {os.getcwd()}")
    logger.info("="*80)

def convert_to_executorch(pt_file, output_file):
    """
    Convert a TorchScript model to ExecuTorch format with logging
    """
    if not EXECUTORCH_AVAILABLE:
        logger.error(f"❌ ExecuTorch not available. Skipping conversion for {pt_file}")
        return False

    logger.info(f"\n{'='*60}")
    logger.info(f"Converting {Path(pt_file).name}...")
    logger.info('='*60)

    try:
        # Check input file
        if not os.path.exists(pt_file):
            logger.error(f"❌ Input file not found: {pt_file}")
            return False

        input_size = os.path.getsize(pt_file)
        logger.info(f"Input file size: {input_size:,} bytes")

        # Load the TorchScript model
        logger.info("Loading TorchScript model...")
        model = torch.jit.load(pt_file)
        model.eval()
        logger.info("✓ Model loaded successfully")

        # Create example input
        logger.info("Creating example input...")
        example_inputs = (torch.randn(1, 4),)
        logger.info(f"✓ Example input created with shape: {example_inputs[0].shape}")

        # Test the model
        logger.info("Testing model inference...")
        with torch.no_grad():
            output = model(*example_inputs)
        logger.info(f"✓ Model test successful, output shape: {output.shape}")

        # Convert to ExecuTorch edge representation
        logger.info("Converting to ExecuTorch edge format...")
        edge = to_edge(model, example_inputs=example_inputs)
        logger.info("✓ Edge conversion complete")

        # Partition for XNNPACK (CPU backend)
        logger.info("Partitioning for XNNPACK backend...")
        edge = partition_for_xnnpack(edge)
        logger.info("✓ Partitioning complete")

        # Export to .pte format
        logger.info("Exporting to .pte format...")
        et_program = edge.to_executorch()
        et_program.write_to_file(output_file)
        logger.info(f"✓ Exported to {output_file}")

        # Check file size
        if os.path.exists(output_file):
            file_size = os.path.getsize(output_file)
            logger.info(f"✓ Output file size: {file_size:,} bytes")
            logger.info(f"✓ File size ratio (output/input): {file_size/input_size:.2%}")
        else:
            logger.warning(f"⚠️  Output file was not created at {output_file}")

        return True

    except Exception as e:
        logger.error(f"❌ Conversion failed: {e}")
        logger.exception("Full traceback:")
        return False

def main():
    """Main conversion process"""
    log_system_info()

    logger.info("\n" + "=" * 80)
    logger.info("EXECUTORCH MODEL CONVERTER WITH LOGGING")
    logger.info("=" * 80)

    # Models to convert
    models = [
        {
            'input': 'converted_models/21kc11kp.pt',
            'output': 'converted_models/21kc11kp.pte'
        },
        {
            'input': 'converted_models/19kc8kp.pt',
            'output': 'converted_models/19kc8kp.pte'
        }
    ]

    if not EXECUTORCH_AVAILABLE:
        logger.error("\n⚠️  ExecuTorch is not installed")
        logger.error("\nTo use this converter, install ExecuTorch:")
        logger.error("  pip install executorch")
        logger.error("\nNote: May require additional dependencies")
        logger.error("See: https://pytorch.org/executorch/stable/getting-started.html")
        return 1

    logger.info("\nStarting model conversion process...")
    results = {}

    for model in models:
        input_file = model['input']
        output_file = model['output']

        logger.info(f"\nProcessing: {input_file}")

        if not os.path.exists(input_file):
            logger.error(f"❌ Input file not found: {input_file}")
            results[input_file] = False
            continue

        success = convert_to_executorch(input_file, output_file)
        results[input_file] = success

    # Summary
    logger.info("\n" + "=" * 80)
    logger.info("CONVERSION SUMMARY")
    logger.info("=" * 80)

    successful = sum(1 for v in results.values() if v)
    failed = sum(1 for v in results.values() if not v)

    for input_file, success in results.items():
        status = "✅ SUCCESS" if success else "❌ FAILED"
        logger.info(f"{Path(input_file).name}: {status}")

    logger.info(f"\nTotal: {len(results)} models, {successful} successful, {failed} failed")

    logger.info("\n" + "=" * 80)
    logger.info("NEXT STEPS")
    logger.info("=" * 80)
    logger.info("""
1. Verify .pte files:
   ls -lh converted_models/*.pte

2. Copy to Android:
   adb push converted_models/*.pte /data/local/tmp/

3. Use in LiquidBounce:
   - Add .pte files to app assets
   - Load with ExecuTorch Android API
   - Run inference with ExecutorTorch runtime
""")

    logger.info(f"Conversion log saved to: {log_file}")

    return 0 if all(results.values()) else 1

if __name__ == '__main__':
    exit_code = main()
    logger.info(f"\nConversion process completed with exit code: {exit_code}")
    exit(exit_code)
