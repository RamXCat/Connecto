import os
import sys

def get_agent_path():
    # Robustly get the main.py path in the parent directory
    return os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "main.py"))

def setup_windows():
    agent_path   = get_agent_path()
    python_path  = sys.executable
    startup_dir  = os.path.join(
        os.environ["APPDATA"],
        "Microsoft", "Windows",
        "Start Menu", "Programs", "Startup"
    )
    bat_path = os.path.join(startup_dir, "stitch-agent.bat")

    # Create the .bat file
    # We use pythonw.exe instead of python.exe if available so it runs hidden without a console window!
    pythonw_path = python_path.replace("python.exe", "pythonw.exe")
    if not os.path.exists(pythonw_path):
        pythonw_path = python_path

    bat_content = f"""@echo off
cd "{os.path.dirname(agent_path)}"
start "" "{pythonw_path}" "{agent_path}"
"""

    with open(bat_path, 'w') as f:
        f.write(bat_content)

    print(f"""
    [SUCCESS] Windows auto-start configured!
    -----------------------------------
    Startup file created at:
    {bat_path}
    
    Stitch will now start
    automatically on every boot :)
    -----------------------------------
    """)

def remove_windows():
    startup_dir = os.path.join(
        os.environ["APPDATA"],
        "Microsoft", "Windows",
        "Start Menu", "Programs", "Startup"
    )
    bat_path = os.path.join(startup_dir, "stitch-agent.bat")

    if os.path.exists(bat_path):
        os.remove(bat_path)
        print("[SUCCESS] Auto-start removed successfully")
    else:
        print("[WARN] No auto-start found to remove")
