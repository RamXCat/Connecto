package com.connecto.agent;

/**
 * A separate Launcher class is required for non-modular JavaFX applications
 * when packaging them into a standalone Fat JAR or Executable.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
