package com.aeropuerto.prioritaria;

import com.aeropuerto.common.TipoAtencion;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Ventana de la cola PRIORITARIA.
 * Muestra la lista de pasajeros en espera y permite llamar al siguiente.
 *
 * Ruta: client-prioritaria/src/main/java/com/aeropuerto/prioritaria/PrioritariaApp.java
 * Correr: mvn javafx:run -pl client-prioritaria
 *
 * @author Pablo Acan
 */
public class PrioritariaApp extends Application {

    // ── Controles UI ─────────────────────────────────────────────────────────
    private TextField              campoIp;
    private Button                 botonConectar;
    private Button                 botonLlamar;
    private Label                  labelEstado;
    private Label                  labelContador;
    private ListView<String>       listaPasajeros;
    private Label                  labelUltimoLlamado;

    // ── Estado de conexión ────────────────────────────────────────────────────
    private String  ipServidor   = "127.0.0.1";
    private static final int PUERTO = 5000;
    private boolean conectado    = false;
    private Timer   timerPolling = null;

    // ── Datos de la lista ─────────────────────────────────────────────────────
    private final ObservableList<String> itemsLista = FXCollections.observableArrayList();

    // ── Colores del tema PRIORITARIA (naranja) ────────────────────────────────
    private static final String COLOR_PRIMARIO  = "#e65100";
    private static final String COLOR_FONDO_BTN = "#fff3e0";

    @Override
    public void start(Stage stage) {
        stage.setTitle("Aeropuerto Guatemala — Cola Prioritaria");
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> detenerPolling());

        Scene scene = new Scene(construirUI(), 500, 580);
        scene.getRoot().setStyle("-fx-background-color: #FFF8F0;");
        stage.setScene(scene);
        stage.show();
    }

    // ── Construcción de la UI ─────────────────────────────────────────────────

    private VBox construirUI() {
        VBox root = new VBox(14);
        root.setPadding(new Insets(28));
        root.setAlignment(Pos.TOP_CENTER);

        // Encabezado
        Label titulo = new Label("🟠  Cola Prioritaria");
        titulo.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
        titulo.setStyle("-fx-text-fill: " + COLOR_PRIMARIO + ";");

        Label subtitulo = new Label("Adultos mayores · Embarazadas · Discapacidad");
        subtitulo.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        Separator sep = new Separator();

        // Panel conexión
        TitledPane panelConexion = new TitledPane("Conexión al Servidor", construirPanelConexion());
        panelConexion.setCollapsible(false);

        // Contador
        labelContador = new Label("Pasajeros en espera: —");
        labelContador.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + COLOR_PRIMARIO + ";"
        );

        // Lista de pasajeros
        listaPasajeros = new ListView<>(itemsLista);
        listaPasajeros.setPrefHeight(200);
        listaPasajeros.setPlaceholder(new Label("Sin pasajeros en cola prioritaria."));
        listaPasajeros.setStyle("-fx-background-radius: 6;");

        TitledPane panelLista = new TitledPane("Pasajeros en Espera", listaPasajeros);
        panelLista.setCollapsible(false);

        // Último llamado
        labelUltimoLlamado = new Label("Ningún pasajero llamado aún.");
        labelUltimoLlamado.setWrapText(true);
        labelUltimoLlamado.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #333;" +
                        "-fx-background-color: #fff3e0; -fx-padding: 10 14 10 14;" +
                        "-fx-background-radius: 6; -fx-border-color: " + COLOR_PRIMARIO + ";" +
                        "-fx-border-radius: 6; -fx-border-width: 1;"
        );
        labelUltimoLlamado.setMaxWidth(Double.MAX_VALUE);

        // Botón llamar siguiente
        botonLlamar = new Button("📢  Llamar Siguiente Prioritario");
        botonLlamar.setStyle(
                "-fx-background-color: " + COLOR_PRIMARIO + "; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-font-size: 14px;" +
                        "-fx-padding: 11 20 11 20; -fx-cursor: hand; -fx-background-radius: 6;"
        );
        botonLlamar.setPrefWidth(280);
        botonLlamar.setDisable(true);
        botonLlamar.setOnAction(e -> accionLlamarSiguiente());

        // Label de estado general
        labelEstado = new Label("Desconectado. Ingresa la IP y conecta.");
        labelEstado.setWrapText(true);
        labelEstado.setMaxWidth(440);
        labelEstado.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #777;" +
                        "-fx-background-color: #f5f5f5; -fx-padding: 8 12 8 12;" +
                        "-fx-background-radius: 6;"
        );

        root.getChildren().addAll(
                titulo, subtitulo, sep,
                panelConexion,
                labelContador,
                panelLista,
                labelUltimoLlamado,
                botonLlamar,
                labelEstado
        );

        return root;
    }

    private GridPane construirPanelConexion() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(12));

        campoIp = new TextField("127.0.0.1");
        campoIp.setPrefWidth(230);

        botonConectar = new Button("Conectar");
        botonConectar.setStyle(
                "-fx-background-color: " + COLOR_PRIMARIO + "; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;" +
                        "-fx-padding: 6 14 6 14;"
        );
        botonConectar.setOnAction(e -> accionConectar());

        Label lblPuerto = new Label("Puerto: " + PUERTO);
        lblPuerto.setStyle("-fx-font-weight: bold; -fx-text-fill: " + COLOR_PRIMARIO + ";");

        grid.add(new Label("IP del servidor:"), 0, 0); grid.add(campoIp,       1, 0);
        grid.add(new Label("Puerto:"),          0, 1); grid.add(lblPuerto,     1, 1);
        grid.add(botonConectar,                 1, 2);

        return grid;
    }

    // ── Lógica de conexión y polling ──────────────────────────────────────────

    private void accionConectar() {
        ipServidor = campoIp.getText().trim();
        if (ipServidor.isEmpty()) {
            mostrarEstado("⚠  Ingresa la IP del servidor.", "#b71c1c", "#ffebee");
            return;
        }

        if (conectado) {
            // Desconectar
            detenerPolling();
            conectado = false;
            botonConectar.setText("Conectar");
            botonLlamar.setDisable(true);
            mostrarEstado("Desconectado.", "#777", "#f5f5f5");
            labelContador.setText("Pasajeros en espera: —");
            itemsLista.clear();
            return;
        }

        // Intentar conectar probando un ESTADO_COLA
        botonConectar.setDisable(true);
        mostrarEstado("⏳  Conectando...", "#555", "#e8eaf6");

        Thread t = new Thread(() -> {
            try {
                // Prueba de conexión
                Socket s = new Socket(ipServidor, PUERTO);
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                out.println("ESTADO_COLA|PRIORITARIA");
                String resp = in.readLine();
                s.close();

                Platform.runLater(() -> {
                    conectado = true;
                    botonConectar.setText("Desconectar");
                    botonConectar.setDisable(false);
                    botonLlamar.setDisable(false);
                    mostrarEstado("✔  Conectado a " + ipServidor + ":" + PUERTO, "#1b5e20", "#e8f5e9");
                    procesarEstadoCola(resp);
                    iniciarPolling();
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado(
                            "✖  No se pudo conectar a " + ipServidor + ":" + PUERTO,
                            "#b71c1c", "#ffebee"
                    );
                    botonConectar.setDisable(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Polling: cada 3 segundos pide al servidor el estado de la cola PRIORITARIA.
     * Es la forma más simple según el informe (vs push).
     */
    private void iniciarPolling() {
        timerPolling = new Timer("polling-prioritaria", true);
        timerPolling.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                solicitarEstadoCola();
            }
        }, 3000, 3000); // cada 3 segundos
    }

    private void detenerPolling() {
        if (timerPolling != null) {
            timerPolling.cancel();
            timerPolling = null;
        }
    }

    private void solicitarEstadoCola() {
        try {
            Socket s = new Socket(ipServidor, PUERTO);
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            out.println("ESTADO_COLA|PRIORITARIA");
            String resp = in.readLine();
            s.close();

            Platform.runLater(() -> procesarEstadoCola(resp));

        } catch (IOException e) {
            Platform.runLater(() ->
                    mostrarEstado("⚠  Se perdió la conexión con el servidor.", "#b71c1c", "#ffebee")
            );
        }
    }

    /**
     * Procesa la respuesta ESTADO_COLA del servidor.
     * Formato esperado: "ESTADO_COLA|nombre1-dpi1,nombre2-dpi2,...|total"
     */
    private void procesarEstadoCola(String respuesta) {
        if (respuesta == null) return;

        String[] partes = respuesta.split("\\|");
        if (partes.length < 3) {
            labelContador.setText("Pasajeros en espera: 0");
            itemsLista.clear();
            return;
        }

        String listaCruda = partes[1];
        String total      = partes[2];

        labelContador.setText("Pasajeros en espera: " + total);
        itemsLista.clear();

        if (!listaCruda.isEmpty()) {
            String[] pasajeros = listaCruda.split(",");
            for (int i = 0; i < pasajeros.length; i++) {
                // Formato de cada pasajero: "nombre-dpi"
                String[] datos = pasajeros[i].split("-");
                String nombre  = datos.length > 0 ? datos[0] : "Desconocido";
                String dpi     = datos.length > 1 ? datos[1] : "?";
                itemsLista.add((i + 1) + ".  " + nombre + "   (DPI: " + dpi + ")");
            }
        }
    }

    // ── Lógica del botón Llamar Siguiente ────────────────────────────────────

    private void accionLlamarSiguiente() {
        botonLlamar.setDisable(true);
        mostrarEstado("⏳  Solicitando siguiente pasajero...", "#555", "#e8eaf6");

        Thread t = new Thread(() -> {
            try {
                Socket s = new Socket(ipServidor, PUERTO);
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                // Protocolo: "LLAMAR_SIGUIENTE|PRIORITARIA"
                out.println("LLAMAR_SIGUIENTE|PRIORITARIA");
                String resp = in.readLine();
                s.close();

                Platform.runLater(() -> procesarPasajeroLlamado(resp));

            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado("✖  Error al contactar el servidor.", "#b71c1c", "#ffebee");
                    botonLlamar.setDisable(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Muestra el pasajero que fue llamado.
     * Formato: "PASAJERO_LLAMADO|dpi|nombre|numeroCola"
     *       o  "ERROR|Cola vacía"
     */
    private void procesarPasajeroLlamado(String respuesta) {
        botonLlamar.setDisable(false);

        if (respuesta == null) {
            mostrarEstado("✖  Sin respuesta del servidor.", "#b71c1c", "#ffebee");
            return;
        }

        String[] partes = respuesta.split("\\|");

        if (partes[0].equals("PASAJERO_LLAMADO")) {
            String dpi    = partes.length > 1 ? partes[1] : "?";
            String nombre = partes.length > 2 ? partes[2] : "?";
            String num    = partes.length > 3 ? partes[3] : "?";

            labelUltimoLlamado.setText(
                    "📢  Llamando:  " + nombre +
                            "\nDPI: " + dpi + "   |   Turno #" + num
            );
            mostrarEstado("✔  Pasajero llamado exitosamente.", "#1b5e20", "#e8f5e9");

        } else if (partes[0].equals("ERROR")) {
            String msg = partes.length > 1 ? partes[1] : "Error";
            mostrarEstado("⚠  " + msg, "#e65100", "#fff3e0");
            labelUltimoLlamado.setText("No hay pasajeros prioritarios en espera.");
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private void mostrarEstado(String texto, String colorTexto, String colorFondo) {
        labelEstado.setText(texto);
        labelEstado.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: " + colorTexto + ";" +
                        "-fx-background-color: " + colorFondo + "; -fx-padding: 8 12 8 12;" +
                        "-fx-background-radius: 6;"
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}