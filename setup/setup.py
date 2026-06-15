import platform
import sys
import os

# Ensure the setup scripts can import each other regardless of where it's run from
sys.path.append(os.path.dirname(__file__))

def main():
    os_name = platform.system()

    print("""
    ************************************
    *     STITCH AGENT AUTO-START      *
    ************************************
    """)

    if os_name == "Windows":
        print("  Detected OS: Windows")
        from windows_setup import setup_windows
        setup_windows()

    elif os_name == "Darwin":
        print("  Detected OS: macOS")
        from mac_setup import setup_mac
        setup_mac()

    elif os_name == "Linux":
        print("  Detected OS: Linux")
        from linux_setup import setup_linux
        setup_linux()

    else:
        print(f"  ❌ Unsupported OS: {os_name}")
        sys.exit(1)

if __name__ == "__main__":
    main()
