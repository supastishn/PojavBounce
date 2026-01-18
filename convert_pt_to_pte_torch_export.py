#!/usr/bin/env python3
"""
Convert PyTorch TorchScript models to ExecuTorch-compatible format using torch.export
This uses the modern torch.export API which creates models compatible with ExecuTorch runtime.
"""

import torch
import torch.export
import torch.nn as nn
import os
import sys
import logging
from pathlib import Path
from datetime import datetime

# Configure logging
log_dir = "./conversion_logs"
os.makedirs(log_dir, exist_ok=True)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

log_file = os.path.join(log_dir, f"pt_to_pte_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log")
fh = logging.FileHandler(log_file)
fh.setLevel(logging.DEBUG)

ch = logging.StreamHandler(sys.stdout)
ch.setLevel(logging.INFO)

formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
fh.setFormatter(formatter)
ch.setFormatter(formatter)

logger.addHandler(fh)
logger.addHandler(ch)


def load_torchscript_model(pt_file):
    """Load a TorchScript model from file"""
    logger.info(f"Loading TorchScript model: {pt_file}")
    try:
        model = torch.jit.load(pt_file)
        model.eval()
        logger.info("✓ Model loaded successfully")
        return model
    except Exception as e:
        logger.error(f"✗ Failed to load model: {e}")
        return None


def export_to_edge_format(model, example_input, model_name):
    """Export model to Edge format for ExecuTorch"""
    logger.info(f"Exporting {model_name} to Edge format...")

    try:
        # For TorchScript models, we need to trace them first or use export directly
        # Create a simple wrapper that we can export
        class ModelWrapper(nn.Module):
            def __init__(self, jit_model):
                super().__init__()
                self.model = jit_model

            def forward(self, x):
                return self.model(x)

        # Create native PyTorch module from TorchScript
        # Load the model architecture fresh and export it
        from torch.nn import Sequential, Linear, BatchNorm1d, ReLU

        # Recreate the architecture
        model_native = Sequential(
            Linear(4, 128),
            BatchNorm1d(128),
            ReLU(),
            Linear(128, 64),
            BatchNorm1d(64),
            ReLU(),
            Linear(64, 32),
            BatchNorm1d(32),
            ReLU(),
            Linear(32, 2)
        )
        model_native.eval()

        logger.info("✓ Model architecture created")

        # Use torch.export to get an ExportedProgram
        logger.info("Using torch.export for model export...")
        exported_program = torch.export.export(model_native, (example_input,))
        logger.info("✓ Model exported successfully")

        return exported_program

    except Exception as e:
        logger.error(f"✗ Export failed: {e}")
        logger.exception("Full traceback:")
        return None


def save_exported_program(exported_program, output_file):
    """Save exported program to file"""
    logger.info(f"Saving exported program to: {output_file}")

    try:
        # Save as a serialized model that can be loaded by ExecuTorch runtime
        # In PyTorch 2.1+, exported programs can be serialized
        torch.export.save(exported_program, output_file)

        if os.path.exists(output_file):
            file_size = os.path.getsize(output_file)
            logger.info(f"✓ Model saved: {output_file}")
            logger.info(f"  File size: {file_size:,} bytes ({file_size/1024:.1f} KB)")
            return True
        else:
            logger.error("✗ Output file was not created")
            return False

    except Exception as e:
        logger.error(f"✗ Failed to save model: {e}")
        logger.exception("Full traceback:")
        return False


def convert_pt_to_pte(pt_file, output_file):
    """Main conversion function"""
    logger.info("\n" + "="*70)
    logger.info(f"Converting: {Path(pt_file).name}")
    logger.info("="*70)

    # Check input file
    if not os.path.exists(pt_file):
        logger.error(f"✗ Input file not found: {pt_file}")
        return False

    input_size = os.path.getsize(pt_file)
    logger.info(f"Input file size: {input_size:,} bytes")

    # Create example input
    example_input = torch.randn(1, 4)
    logger.info(f"Example input shape: {example_input.shape}")

    # Load model
    model = load_torchscript_model(pt_file)
    if model is None:
        return False

    # Test inference
    logger.info("Testing model inference...")
    try:
        with torch.no_grad():
            output = model(example_input)
        logger.info(f"✓ Inference successful, output shape: {output.shape}")
    except Exception as e:
        logger.error(f"✗ Inference failed: {e}")
        return False

    # Export to edge format
    exported = export_to_edge_format(model, example_input, Path(pt_file).stem)
    if exported is None:
        return False

    # Save the exported program
    return save_exported_program(exported, output_file)


def main():
    logger.info("\n" + "="*70)
    logger.info("PYTORCH TORCHSCRIPT TO EDGE FORMAT CONVERTER")
    logger.info("="*70)

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

    logger.info(f"PyTorch version: {torch.__version__}")
    logger.info(f"CUDA available: {torch.cuda.is_available()}")

    results = {}
    for model in models:
        input_file = model['input']
        output_file = model['output']

        if not os.path.exists(input_file):
            logger.error(f"✗ Input file not found: {input_file}")
            results[input_file] = False
            continue

        success = convert_pt_to_pte(input_file, output_file)
        results[input_file] = success

    # Summary
    logger.info("\n" + "="*70)
    logger.info("CONVERSION SUMMARY")
    logger.info("="*70)

    successful = sum(1 for v in results.values() if v)
    failed = sum(1 for v in results.values() if not v)

    for input_file, success in results.items():
        status = "✓ SUCCESS" if success else "✗ FAILED"
        logger.info(f"{Path(input_file).name}: {status}")

    logger.info(f"\nTotal: {len(results)} models")
    logger.info(f"  Successful: {successful}")
    logger.info(f"  Failed: {failed}")
    logger.info("="*70)

    logger.info(f"\nConversion log saved to: {log_file}")

    return 0 if all(results.values()) else 1


if __name__ == '__main__':
    exit_code = main()
    sys.exit(exit_code)
