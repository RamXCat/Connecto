package com.connecto.agent;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class MainApp extends Application {

    private HttpAgentServer server;
    private UdpBroadcaster broadcaster;

    // Default Configurations
    private static final String DEVICE_NAME = "My Laptop";
    private static final int HTTP_PORT = 5000;
    private static final String SECRET_TOKEN = "cxtzilla@123";

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 30;"); // Modern dark mode

        Label title = new Label("Stitch Agent");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', sans-serif;");

        Label subtitle = new Label("Scan to pair Connecto App");
        subtitle.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 14px;");

        Label status = new Label("Starting server...");
        status.setStyle("-fx-text-fill: #f44336; -fx-font-size: 14px;");

        ImageView qrView = new ImageView();
        qrView.setFitWidth(280);
        qrView.setFitHeight(280);
        qrView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 5);"); // Subtle shadow

        root.getChildren().addAll(title, subtitle, qrView, status);

        Scene scene = new Scene(root, 400, 520);
        
        try {
            primaryStage.getIcons().add(new Image(MainApp.class.getResourceAsStream("/logo.png")));
        } catch (Exception ignored) {}
        
        primaryStage.setTitle("Connecto - Stitch Agent");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        
        setupSystemTray(primaryStage); // Add System Tray logic
        
        primaryStage.show();

        // Boot systems in background thread
        new Thread(() -> {
            try {
                server = new HttpAgentServer(HTTP_PORT, SECRET_TOKEN);
                broadcaster = new UdpBroadcaster(DEVICE_NAME, HTTP_PORT);
                broadcaster.start();

                String ip = UdpBroadcaster.getLocalIp();
                
                // Generate Payload
                JsonObject payload = new JsonObject();
                payload.addProperty("service", "stitch");
                payload.addProperty("device", DEVICE_NAME);
                payload.addProperty("ip", ip);
                payload.addProperty("port", HTTP_PORT);
                payload.addProperty("token", SECRET_TOKEN);

                Image qrImage = generateQRCode(payload.toString());

                Platform.runLater(() -> {
                    qrView.setImage(qrImage);
                    status.setText("🟢 Online at " + ip + ":" + HTTP_PORT);
                    status.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 16px; -fx-font-weight: bold;");
                });

            } catch (Throwable t) {
                try {
                    java.io.PrintWriter pw = new java.io.PrintWriter("crash.log");
                    t.printStackTrace(pw);
                    pw.close();
                } catch (Exception ignored) {}
                Platform.runLater(() -> {
                    String msg = t.toString();
                    if (t.getCause() != null) msg += " -> " + t.getCause().toString();
                    status.setText("Error: " + msg);
                    status.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11px;");
                });
            }
        }).start();
    }

    @Override
    public void stop() {
        if (server != null) server.stop();
        if (broadcaster != null) broadcaster.stopBroadcasting();
        System.exit(0);
    }

    private void setupSystemTray(Stage primaryStage) {
        if (!java.awt.SystemTray.isSupported()) return;

        Platform.setImplicitExit(false); // Keep JavaFX alive when window closes

        try {
            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(
                java.awt.Toolkit.getDefaultToolkit().getImage(MainApp.class.getResource("/logo.png")),
                "Connecto Agent"
            );
            trayIcon.setImageAutoSize(true);

            java.awt.PopupMenu popup = new java.awt.PopupMenu();
            
            java.awt.MenuItem openItem = new java.awt.MenuItem("Show QR Code");
            openItem.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.toFront();
            }));

            java.awt.MenuItem exitItem = new java.awt.MenuItem("Quit Connecto");
            exitItem.addActionListener(e -> {
                java.awt.SystemTray.getSystemTray().remove(trayIcon);
                Platform.exit();
                System.exit(0);
            });

            popup.add(openItem);
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            // Double click tray icon to open
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.toFront();
            }));

            java.awt.SystemTray.getSystemTray().add(trayIcon);

            // Override 'X' button to hide window instead of exiting
            primaryStage.setOnCloseRequest(e -> {
                primaryStage.hide();
                e.consume();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Image generateQRCode(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 280, 280);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return new Image(new ByteArrayInputStream(pngOutputStream.toByteArray()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
