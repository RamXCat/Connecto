package com.connecto.agent;

import java.net.*;
import com.google.gson.JsonObject;

public class UdpBroadcaster extends Thread {
    private final int port = 55555;
    private final int httpPort;
    private final String deviceName;
    private volatile boolean running = true;

    public UdpBroadcaster(String deviceName, int httpPort) {
        this.deviceName = deviceName;
        this.httpPort = httpPort;
        setDaemon(true);
    }

    public void stopBroadcasting() {
        running = false;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            while (running) {
                String ip = getLocalIp();
                
                JsonObject payload = new JsonObject();
                payload.addProperty("service", "stitch");
                payload.addProperty("device", deviceName);
                payload.addProperty("ip", ip);
                payload.addProperty("port", httpPort);
                payload.addProperty("platform", System.getProperty("os.name"));
                payload.addProperty("timestamp", System.currentTimeMillis() / 1000);

                byte[] buffer = payload.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), port);
                
                try {
                    socket.send(packet);
                } catch (Exception ignored) {}

                Thread.sleep(3000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getLocalIp() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
