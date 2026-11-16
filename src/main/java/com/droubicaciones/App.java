package com.droubicaciones;

import com.droubicaciones.config.DatabaseConfig;
import com.droubicaciones.model.SesionUsuario;
import com.droubicaciones.web.UbicacionWebServer;

public class App {

    public static void main(String[] args) {
        // Inicialización de sesión por defecto
        SesionUsuario.iniciarSesion("ADMIN", "Administrador", 999);

        // Iniciar Servidor Web Local para Smartphones, Pistolas y PCs en la Red
        int port = DatabaseConfig.getWebPort();
        try {
            UbicacionWebServer webServer = new UbicacionWebServer(port);
            webServer.start();
            System.out.println("ℹ️ [Modo Servidor Web Activo] Ejecutando en segundo plano.");
            System.out.println("🌐 Acceso Web activo desde cualquier PC, Pistola o Teléfono en la red local.");
        } catch (Exception e) {
            System.err.println("❌ Error al iniciar Servidor Web Local: " + e.getMessage());
        }
    }
}
