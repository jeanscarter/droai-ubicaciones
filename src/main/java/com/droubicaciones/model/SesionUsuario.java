package com.droubicaciones.model;

import java.net.InetAddress;

/**
 * Singleton de sesión del usuario autenticado en Profit Plus.
 */
public class SesionUsuario {

    private static SesionUsuario instance;

    private final String coUsuario;
    private final String nombreUsuario;
    private final int nivel;
    private final String maquina;

    private SesionUsuario(String coUsuario, String nombreUsuario, int nivel) {
        this.coUsuario = coUsuario != null ? coUsuario.trim() : "";
        this.nombreUsuario = nombreUsuario != null ? nombreUsuario.trim() : "";
        this.nivel = nivel;

        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostName = "LOCAL";
        }
        this.maquina = hostName;
    }

    public static synchronized void iniciarSesion(String coUsuario, String nombreUsuario, int nivel) {
        instance = new SesionUsuario(coUsuario, nombreUsuario, nivel);
        System.out.println("[SesionUsuario] ✔ Sesión iniciada: " + coUsuario + " (" + nombreUsuario + ") — Nivel: " + nivel + " — Máquina: " + instance.maquina);
    }

    public static synchronized SesionUsuario current() {
        return instance;
    }

    public static synchronized boolean isAutenticado() {
        return instance != null;
    }

    public String getCoUsuario() { return coUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public int getNivel() { return nivel; }
    public String getMaquina() { return maquina; }
}
