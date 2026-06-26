package com.connecto.agent;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;

public class HttpAgentServer extends NanoHTTPD {
    private final String secretToken;
    private Runnable onShowRequest;

    // State for rate limiting and debouncing volume/brightness
    private static String volumeHelperPath = null;
    private static String brightnessHelperPath = null;

    private int targetVolume = -1;
    private int currentVolume = -1;
    private boolean volumeWorkerRunning = false;
    private final Object volumeLock = new Object();

    private int targetBrightness = -1;
    private int currentBrightness = -1;
    private boolean brightnessWorkerRunning = false;
    private final Object brightnessLock = new Object();

    public HttpAgentServer(int port, String secretToken) throws IOException {
        super(port);
        this.secretToken = secretToken;
        prepareHelpers();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    private static void prepareHelpers() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) return;

        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            java.io.File volExe = new java.io.File(tmpDir, "connecto_volume_helper.exe");
            java.io.File brightExe = new java.io.File(tmpDir, "connecto_brightness_helper.exe");
            
            volumeHelperPath = volExe.getAbsolutePath();
            brightnessHelperPath = brightExe.getAbsolutePath();

            String cscPath = "C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\csc.exe";
            if (!new java.io.File(cscPath).exists()) {
                return;
            }

            if (!volExe.exists()) {
                String code = """
                        using System;
                        using System.Runtime.InteropServices;
                        
                        [Guid("5CDF2C82-841E-4546-9722-0CF74078229A"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                        interface IAudioEndpointVolume {
                            int f(); int g(); int h(); int i();
                            int SetMasterVolumeLevelScalar(float fLevel, Guid pguidEventContext);
                            int j();
                            int GetMasterVolumeLevelScalar(out float pfLevel);
                            int k(); int l(); int m(); int n();
                            int SetMute(bool bMute, Guid pguidEventContext);
                            int GetMute(out bool pbMute);
                        }
                        
                        [Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                        interface IMMDevice {
                            int Activate(ref Guid id, int clsCtx, int activationParams, out IAudioEndpointVolume aev);
                        }
                        
                        [Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                        interface IMMDeviceEnumerator {
                            int f();
                            int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice endpoint);
                        }
                        
                        [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
                        class MMDeviceEnumeratorComObject {}
                        
                        public class Audio {
                            public static void Main(string[] args) {
                                if (args.Length == 0) return;
                                try {
                                    float percentage = float.Parse(args[0]);
                                    var enumerator = new MMDeviceEnumeratorComObject() as IMMDeviceEnumerator;
                                    IMMDevice dev = null;
                                    enumerator.GetDefaultAudioEndpoint(0, 1, out dev);
                                    IAudioEndpointVolume epv = null;
                                    var epvid = typeof(IAudioEndpointVolume).GUID;
                                    dev.Activate(ref epvid, 23, 0, out epv);
                                    epv.SetMasterVolumeLevelScalar(percentage / 100, Guid.Empty);
                                } catch (Exception) {}
                            }
                        }
                        """;
                java.io.File csFile = java.io.File.createTempFile("vol", ".cs");
                java.nio.file.Files.write(csFile.toPath(), code.getBytes());
                Process p = Runtime.getRuntime().exec(new String[]{cscPath, "/target:exe", "/out:" + volExe.getAbsolutePath(), csFile.getAbsolutePath()});
                p.waitFor();
                csFile.delete();
            }

            if (!brightExe.exists()) {
                String code = """
                        using System;
                        using System.Management;
                        
                        public class Brightness {
                            public static void Main(string[] args) {
                                if (args.Length == 0) return;
                                try {
                                    int level = int.Parse(args[0]);
                                    using (var mclass = new ManagementClass("root/WMI", "WmiMonitorBrightnessMethods", null)) {
                                        using (var instances = mclass.GetInstances()) {
                                            foreach (ManagementObject instance in instances) {
                                                instance.InvokeMethod("WmiSetBrightness", new object[] { uint.MaxValue, (byte)level });
                                            }
                                        }
                                    }
                                } catch (Exception) {}
                            }
                        }
                        """;
                java.io.File csFile = java.io.File.createTempFile("bright", ".cs");
                java.nio.file.Files.write(csFile.toPath(), code.getBytes());
                Process p = Runtime.getRuntime().exec(new String[]{cscPath, "/r:System.Management.dll", "/target:exe", "/out:" + brightExe.getAbsolutePath(), csFile.getAbsolutePath()});
                p.waitFor();
                csFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnShowRequest(Runnable onShowRequest) {
        this.onShowRequest = onShowRequest;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        if (uri.equals("/status") && method == Method.GET) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"online\"}");
        }

        if (uri.equals("/show") && method == Method.GET) {
            if (onShowRequest != null) onShowRequest.run();
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"showing\"}");
        }

        if (method == Method.POST) {
            String authHeader = session.getHeaders().get("authorization");
            if (authHeader == null || !authHeader.equals("Bearer " + secretToken)) {
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", "{\"error\":\"Unauthorized\"}");
            }

            String action = uri.substring(1); // remove leading slash
            try {
                if (action.equals("shutdown") || action.equals("sleep") || action.equals("restart") || action.equals("lock")) {
                    executeCommand(action);
                    return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true, \"action\":\"" + action + "\"}");
                } else if (action.equals("volume") || action.equals("brightness")) {
                    int level = -1;
                    java.util.Map<String, java.util.List<String>> params = session.getParameters();
                    if (params.containsKey("level") && !params.get("level").isEmpty()) {
                        level = Integer.parseInt(params.get("level").get(0));
                    } else {
                        java.util.Map<String, String> files = new java.util.HashMap<>();
                        session.parseBody(files);
                        String postData = files.get("postData");
                        if (postData != null && postData.contains("\"level\"")) {
                            String[] parts = postData.split("\"level\"");
                            if (parts.length > 1) {
                                String val = parts[1].replaceAll("[^0-9]", "");
                                if (!val.isEmpty()) level = Integer.parseInt(val);
                            }
                        }
                    }
                    
                    if (level >= 0 && level <= 100) {
                        if (action.equals("volume")) executeVolume(level);
                        else executeBrightness(level);
                        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true, \"action\":\"" + action + "\", \"level\":" + level + "}");
                    } else {
                        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Missing or invalid level parameter (0-100)\"}");
                    }
                } else {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Invalid action\"}");
                }
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    private void executeCommand(String action) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (action.equals("shutdown")) {
            if (os.contains("win")) Runtime.getRuntime().exec("shutdown /s /t 5");
            else Runtime.getRuntime().exec(new String[]{"sudo", "shutdown", "-h", "now"});
        } else if (action.equals("sleep")) {
            if (os.contains("win")) Runtime.getRuntime().exec("rundll32.exe powrprof.dll,SetSuspendState 0,1,0");
            else if (os.contains("mac")) Runtime.getRuntime().exec("pmset sleepnow");
            else Runtime.getRuntime().exec("systemctl suspend");
        } else if (action.equals("restart")) {
            if (os.contains("win")) Runtime.getRuntime().exec("shutdown /r /t 5");
            else Runtime.getRuntime().exec(new String[]{"sudo", "reboot"});
        } else if (action.equals("lock")) {
            if (os.contains("win")) Runtime.getRuntime().exec("rundll32.exe user32.dll,LockWorkStation");
            else if (os.contains("mac")) Runtime.getRuntime().exec("pmset displaysleepnow");
            else Runtime.getRuntime().exec("loginctl lock-session");
        }
    }

    private void executeVolume(int level) {
        synchronized (volumeLock) {
            targetVolume = level;
            if (!volumeWorkerRunning) {
                volumeWorkerRunning = true;
                new Thread(this::volumeWorker).start();
            }
        }
    }

    private void volumeWorker() {
        while (true) {
            int valToSet;
            synchronized (volumeLock) {
                if (targetVolume == currentVolume) {
                    volumeWorkerRunning = false;
                    break;
                }
                valToSet = targetVolume;
            }
            try {
                setVolumeProcess(valToSet);
                currentVolume = valToSet;
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void setVolumeProcess(int level) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            if (volumeHelperPath != null && new java.io.File(volumeHelperPath).exists()) {
                Process p = Runtime.getRuntime().exec(new String[]{volumeHelperPath, String.valueOf(level)});
                p.waitFor();
            } else {
                // Fallback to powershell if csc failed or wasn't run
                String script = """
                        $code = @"
                        using System;
                        using System.Runtime.InteropServices;
                        
                        [Guid("5CDF2C82-841E-4546-9722-0CF74078229A"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                        interface IAudioEndpointVolume {
                            int f(); int g(); int h(); int i();
                            int SetMasterVolumeLevelScalar(float fLevel, Guid pguidEventContext);
                            int j();
                            int GetMasterVolumeLevelScalar(out float pfLevel);
                            int k(); int l(); int m(); int n();
                            int SetMute(bool bMute, Guid pguidEventContext);
                            int GetMute(out bool pbMute);
                        }
                        
                        [Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                        interface IMMDevice {
                            int Activate(ref Guid id, int clsCtx, int activationParams, out IAudioEndpointVolume aev);
                        }
                        
                        [Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                        interface IMMDeviceEnumerator {
                            int f();
                            int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice endpoint);
                        }
                        
                        [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
                        class MMDeviceEnumeratorComObject {}
                        
                        public class Audio {
                            public static void SetVolume(float percentage) {
                                var enumerator = new MMDeviceEnumeratorComObject() as IMMDeviceEnumerator;
                                IMMDevice dev = null;
                                enumerator.GetDefaultAudioEndpoint(0, 1, out dev);
                                IAudioEndpointVolume epv = null;
                                var epvid = typeof(IAudioEndpointVolume).GUID;
                                dev.Activate(ref epvid, 23, 0, out epv);
                                epv.SetMasterVolumeLevelScalar(percentage / 100, Guid.Empty);
                            }
                        }
                        "@
                        Add-Type -TypeDefinition $code
                        [Audio]::SetVolume(%d)
                        """.formatted(level);
                java.io.File tempScript = java.io.File.createTempFile("volume", ".ps1");
                java.nio.file.Files.write(tempScript.toPath(), script.getBytes());
                Process p = Runtime.getRuntime().exec(new String[]{"powershell", "-ExecutionPolicy", "Bypass", "-File", tempScript.getAbsolutePath()});
                p.waitFor();
                tempScript.delete();
            }
        } else if (os.contains("mac")) {
            Process p = Runtime.getRuntime().exec(new String[]{"osascript", "-e", "set volume output volume " + level});
            p.waitFor();
        }
    }

    private void executeBrightness(int level) {
        synchronized (brightnessLock) {
            targetBrightness = level;
            if (!brightnessWorkerRunning) {
                brightnessWorkerRunning = true;
                new Thread(this::brightnessWorker).start();
            }
        }
    }

    private void brightnessWorker() {
        while (true) {
            int valToSet;
            synchronized (brightnessLock) {
                if (targetBrightness == currentBrightness) {
                    brightnessWorkerRunning = false;
                    break;
                }
                valToSet = targetBrightness;
            }
            try {
                setBrightnessProcess(valToSet);
                currentBrightness = valToSet;
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void setBrightnessProcess(int level) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            if (brightnessHelperPath != null && new java.io.File(brightnessHelperPath).exists()) {
                Process p = Runtime.getRuntime().exec(new String[]{brightnessHelperPath, String.valueOf(level)});
                p.waitFor();
            } else {
                Process p = Runtime.getRuntime().exec(new String[]{"powershell", "-Command", "(Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1, " + level + ")"});
                p.waitFor();
            }
        } else if (os.contains("mac")) {
            Process p = Runtime.getRuntime().exec(new String[]{"brightness", String.valueOf(level / 100.0f)});
            p.waitFor();
        }
    }
}
