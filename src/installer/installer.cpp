#include <windows.h>

#define INITGUID
#include <initguid.h>

// Define IID_IPersistFile manually
DEFINE_GUID(IID_IPersistFile,
0x0000010b, 0x0000, 0x0000,
0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46);

#include <shlobj.h>
#include <shlwapi.h>
#include <string>
#include <cstdlib>
#include <ctime>

#pragma comment(lib, "shell32.lib")
#pragma comment(lib, "shlwapi.lib")

HWND hStatus;
HWND hButton;

const char* REPO_URL = "https://github.com/jaime100200300/minecraft-clone.git";

void setStatus(const char* text) {
    SetWindowTextA(hStatus, text);
}

std::string getTempDir() {
    char tempPath[MAX_PATH];
    GetTempPathA(MAX_PATH, tempPath);

    // simple random suffix
    std::srand((unsigned)time(NULL));
    int r = std::rand();

    char buf[64];
    sprintf(buf, "mcclone-%d", r);

    std::string dir = std::string(tempPath) + buf;
    CreateDirectoryA(dir.c_str(), NULL);
    return dir;
}

std::string getDesktop() {
    char path[MAX_PATH];
    if (SUCCEEDED(SHGetFolderPathA(NULL, CSIDL_DESKTOPDIRECTORY, NULL, 0, path))) {
        return std::string(path);
    }
    return "C:\\Users\\Public\\Desktop";
}

std::string getStartMenu() {
    char path[MAX_PATH];
    if (SUCCEEDED(SHGetFolderPathA(NULL, CSIDL_PROGRAMS, NULL, 0, path))) {
        return std::string(path);
    }
    return "";
}

void createShortcut(const std::string& shortcutPath, const std::string& targetPath) {
    IShellLinkA* link;
    if (SUCCEEDED(CoCreateInstance(CLSID_ShellLink, NULL, CLSCTX_INPROC_SERVER,
                                   IID_IShellLinkA, (LPVOID*)&link))) {
        link->SetPath(targetPath.c_str());

        IPersistFile* file;
        if (SUCCEEDED(link->QueryInterface(IID_IPersistFile, (LPVOID*)&file))) {
            WCHAR wShortcut[MAX_PATH];
            MultiByteToWideChar(CP_ACP, 0, shortcutPath.c_str(), -1, wShortcut, MAX_PATH);
            file->Save(wShortcut, TRUE);
            file->Release();
        }
        link->Release();
    }
}

DWORD WINAPI installThread(LPVOID) {
    setStatus("Cloning repository...");

    std::string tempDir = getTempDir();

    std::string cmd = "git clone ";
    cmd += REPO_URL;
    cmd += " \"";
    cmd += tempDir;
    cmd += "\"";

    system(cmd.c_str());

    char localAppData[MAX_PATH];
    GetEnvironmentVariableA("LOCALAPPDATA", localAppData, MAX_PATH);
    std::string installDir = std::string(localAppData) + "\\Minecraft Clone";

    CreateDirectoryA(installDir.c_str(), NULL);

    std::string exePath = tempDir + "\\out\\minecraft-clone.exe";

    if (!PathFileExistsA(exePath.c_str())) {
        setStatus("ERROR: minecraft-clone.exe not found!");
        return 0;
    }

    setStatus("Copying EXE...");
    std::string destExe = installDir + "\\minecraft-clone.exe";
    CopyFileA(exePath.c_str(), destExe.c_str(), FALSE);

    setStatus("Creating shortcuts...");

    std::string desktop = getDesktop() + "\\Minecraft Clone.lnk";
    std::string startMenu = getStartMenu() + "\\Minecraft Clone.lnk";

    createShortcut(desktop, destExe);
    if (!getStartMenu().empty()) {
        createShortcut(startMenu, destExe);
    }

    setStatus("Installation complete!");
    return 0;
}

LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
    case WM_CREATE:
        hStatus = CreateWindowA("STATIC", "Ready to install.",
            WS_VISIBLE | WS_CHILD,
            20, 60, 360, 25,
            hwnd, NULL, NULL, NULL);

        hButton = CreateWindowA("BUTTON", "Install",
            WS_VISIBLE | WS_CHILD,
            150, 100, 100, 30,
            hwnd, (HMENU)1, NULL, NULL);
        break;

    case WM_COMMAND:
        if (LOWORD(wParam) == 1) {
            CreateThread(NULL, 0, installThread, NULL, 0, NULL);
        }
        break;

    case WM_DESTROY:
        PostQuitMessage(0);
        break;
    }
    return DefWindowProcA(hwnd, msg, wParam, lParam);
}

int WINAPI WinMain(HINSTANCE hInst, HINSTANCE, LPSTR, int nCmdShow) {
    CoInitialize(NULL);

    const char CLASS_NAME[] = "MinecraftCloneInstaller";

    WNDCLASSA wc = {};
    wc.lpfnWndProc = WndProc;
    wc.hInstance = hInst;
    wc.lpszClassName = CLASS_NAME;

    RegisterClassA(&wc);

    HWND hwnd = CreateWindowExA(
        0, CLASS_NAME, "Minecraft Clone Installer",
        WS_OVERLAPPEDWINDOW,
        CW_USEDEFAULT, CW_USEDEFAULT, 400, 200,
        NULL, NULL, hInst, NULL
    );

    ShowWindow(hwnd, nCmdShow);

    MSG msg = {};
    while (GetMessageA(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessageA(&msg);
    }

    CoUninitialize();
    return 0;
}
