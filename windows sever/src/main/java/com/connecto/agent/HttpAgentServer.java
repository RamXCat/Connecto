package com.connecto.agent;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;

public class HttpAgentServer extends NanoHTTPD {
    private final String secretToken;

    public HttpAgentServer(int port, String secretToken) throws IOException {
        super(port);
        this.secretToken = secretToken;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        if (uri.equals("/status") && method == Method.GET) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"online\"}");
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
}
