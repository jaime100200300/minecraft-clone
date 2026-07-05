import customtkinter as ctk
import os
import shutil
import subprocess
from pathlib import Path
import threading
import uuid

os.system("pip install customtkinter >nul 2>nul")
os.system("pip install uuid >nul 2>nul")

# === Paths ===
TEMP_DIR = Path(os.getenv("TEMP")) / f"mcclone-{uuid.uuid4()}"
INSTALL_DIR = Path(os.getenv("LOCALAPPDATA")) / "Minecraft Clone"


# Auto-detect Desktop (OneDrive or normal)
USERPROFILE = Path(os.path.expanduser("~"))

DESKTOP_ONEDRIVE = USERPROFILE / "OneDrive" / "Desktop"
DESKTOP_NORMAL = USERPROFILE / "Desktop"

if DESKTOP_ONEDRIVE.exists():
    DESKTOP = DESKTOP_ONEDRIVE
elif DESKTOP_NORMAL.exists():
    DESKTOP = DESKTOP_NORMAL
else:
    # Fallback: create normal Desktop if missing
    DESKTOP = DESKTOP_NORMAL
    DESKTOP.mkdir(parents=True, exist_ok=True)



START_MENU = Path(os.getenv("APPDATA")) / r"Microsoft\Windows\Start Menu\Programs"

REPO_URL = "https://github.com/jaime100200300/minecraft-clone.git"

def create_shortcut(shortcut_path, target_path):
    # One-line PowerShell command using os.system
    cmd = (
        f'powershell -Command '
        f'"$s=(New-Object -COM WScript.Shell).CreateShortcut(\'{shortcut_path}\');'
        f'$s.TargetPath=\'{target_path}\';'
        f'$s.Save();"'
    )
    os.system(cmd)

def install(progress):
    progress.configure(text="Cloning repository into TEMP...")
    subprocess.run(["git", "clone", REPO_URL, str(TEMP_DIR)], shell=True)

    progress.configure(text="Creating install directory...")
    INSTALL_DIR.mkdir(parents=True, exist_ok=True)

    exe_path = TEMP_DIR / "out" / "minecraft-clone.exe"
    if not exe_path.exists():
        progress.configure(text="ERROR: minecraft-clone.exe not found!")
        return

    progress.configure(text="Copying EXE...")
    shutil.copy(exe_path, INSTALL_DIR / "minecraft-clone.exe")

    progress.configure(text="Creating desktop shortcut...")
    create_shortcut(
        str(DESKTOP / "Minecraft Clone.lnk"),
        str(INSTALL_DIR / "minecraft-clone.exe")
    )

    progress.configure(text="Creating Start Menu shortcut...")
    create_shortcut(
        str(START_MENU / "Minecraft Clone.lnk"),
        str(INSTALL_DIR / "minecraft-clone.exe")
    )

    progress.configure(text="Installation complete!")

def start_install(progress):
    threading.Thread(target=install, args=(progress,), daemon=True).start()

def main():
    ctk.set_appearance_mode("dark")
    ctk.set_default_color_theme("blue")

    app = ctk.CTk()
    app.title("Minecraft Clone Installer")
    app.geometry("400x200")

    title = ctk.CTkLabel(app, text="Minecraft Clone Installer", font=("Arial", 20))
    title.pack(pady=10)

    progress = ctk.CTkLabel(app, text="Ready to install.")
    progress.pack(pady=10)

    install_button = ctk.CTkButton(app, text="Install", command=lambda: start_install(progress))
    install_button.pack(pady=10)

    app.mainloop()

if __name__ == "__main__":
    main()
