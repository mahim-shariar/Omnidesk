#!/usr/bin/env python3
"""
OmniDesk Companion Server Setup Wrapper
Runs the interactive cross-platform installation wizard.
"""
import sys
from pathlib import Path

wizard = Path(__file__).resolve().parent / "install_wizard.py"
if wizard.exists():
    import runpy
    runpy.run_path(str(wizard), run_name="__main__")
else:
    print("Error: install_wizard.py not found in the same directory.")
    sys.exit(1)
