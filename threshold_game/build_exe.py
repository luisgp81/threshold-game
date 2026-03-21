"""
THRESHOLD: Global Crisis - PyInstaller Build Script
Produces a standalone executable for Steam distribution.
"""
import os
import sys
import subprocess
import shutil

GAME_NAME = "THRESHOLD_GlobalCrisis"
MAIN_SCRIPT = "main.py"
DATA_DIR = "data"
ASSETS_DIR = "assets"
ICON_PATH = os.path.join(ASSETS_DIR, "icon.ico")


def build():
    print("=" * 60)
    print("  THRESHOLD: Global Crisis - Build Script")
    print("=" * 60)

    # Ensure PyInstaller is available
    try:
        import PyInstaller
        print(f"[OK] PyInstaller {PyInstaller.__version__} found.")
    except ImportError:
        print("[ERROR] PyInstaller not found. Install: pip install pyinstaller")
        sys.exit(1)

    # Build command
    cmd = [
        sys.executable, "-m", "PyInstaller",
        "--onedir",
        "--windowed",
        f"--name={GAME_NAME}",
        # Data files
        f"--add-data={DATA_DIR}{os.pathsep}{DATA_DIR}",
        f"--add-data={ASSETS_DIR}{os.pathsep}{ASSETS_DIR}",
        # Hidden imports
        "--hidden-import=pygame",
        "--hidden-import=json",
        # Clean build
        "--clean",
        "--noconfirm",
    ]

    # Add icon if it exists
    if os.path.exists(ICON_PATH):
        cmd.append(f"--icon={ICON_PATH}")
        print(f"[OK] Icon: {ICON_PATH}")
    else:
        print(f"[WARN] No icon found at {ICON_PATH}, building without icon.")

    cmd.append(MAIN_SCRIPT)

    print("\n[...] Running PyInstaller...")
    print(" ".join(cmd))
    print()

    result = subprocess.run(cmd, cwd=os.path.dirname(os.path.abspath(__file__)))

    if result.returncode == 0:
        dist_path = os.path.join("dist", GAME_NAME)
        print(f"\n[SUCCESS] Build complete!")
        print(f"  Output: {dist_path}/")
        print(f"  Executable: {os.path.join(dist_path, GAME_NAME)}")
        print("\n  For Steam upload, zip the entire dist/{} folder.".format(GAME_NAME))
    else:
        print(f"\n[FAILED] Build failed with return code {result.returncode}")
        sys.exit(result.returncode)


if __name__ == "__main__":
    build()
