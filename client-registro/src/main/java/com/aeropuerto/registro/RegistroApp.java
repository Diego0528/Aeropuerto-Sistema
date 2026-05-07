package com.aeropuerto.registro;

import com.aeropuerto.common.TipoAtencion;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Ventana de registro de pasajeros — client-registro.
 *
 * Campos:
 *   - DPI (TextField)
 *   - Nombre completo (TextField)
 *   - Tipo de atención (ComboBox: GENERAL / PRIORITARIA / ESPECIAL)
 *   - IP del servidor (TextField, pre-llenada con 127.0.0.1 para pruebas)
 *
 * Al presionar "Registrar":
 *   1. Valida que los campos no estén vacíos.
 *   2. Lanza un hilo separado para no bloquear la UI.
 *   3. Abre conexión con ClienteSocket y envía mensaje REGISTRO.
 *   4. Muestra la respuesta (CONFIRMACION o ERROR) en un Label de estado.
 *
 * Ruta: client-registro/src/main/java/com/aeropuerto/registro/RegistroApp.java
 * Correr: mvn javafx:run -pl client-registro
 *
 * @author Pablo Acan
 */
public class RegistroApp extends Application {

    // ── Controles de la UI ───────────────────────────────────────────────────
    private TextField        campoDpi;
    private TextField        campoNombre;
    private ComboBox<String> comboTipo;
    private TextField        campoIp;
    private Label            labelEstado;
    private Button           botonRegistrar;

    // Puerto fijo según el informe del proyecto
    private static final int PUERTO = 5000;

    // ── Punto de entrada JavaFX ──────────────────────────────────────────────
    @Override
    public void start(Stage stage) {
        stage.setTitle("Aeropuerto Guatemala — Registro de Pasajeros");
        stage.setResizable(false);

        VBox root = construirUI();
        Scene scene = new Scene(root, 480, 530);
        aplicarEstilos(scene);

        stage.setScene(scene);
        stage.show();
    }

    // ── Construcción de la interfaz ──────────────────────────────────────────

    private VBox construirUI() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);

        // ── Encabezado ───────────────────────────────────────────────────────
        Label titulo = new Label("✈  Registro de Pasajero");
        titulo.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
        titulo.setStyle("-fx-text-fill: #1a237e;");

        Label subtitulo = new Label("Aeropuerto Internacional de Guatemala");
        subtitulo.setStyle("-fx-text-fill: #555; -fx-font-size: 13px;");

        Separator sep = new Separator();
        sep.setPadding(new Insets(4, 0, 4, 0));

        // ── Sección conexión ─────────────────────────────────────────────────
        TitledPane panelConexion = new TitledPane();
        panelConexion.setText("Conexión al Servidor");
        panelConexion.setCollapsible(false);

        GridPane gridConexion = new GridPane();
        gridConexion.setHgap(10); gridConexion.setVgap(10);
        gridConexion.setPadding(new Insets(12));

        campoIp = new TextField("127.0.0.1");
        campoIp.setPromptText("Ej: 192.168.1.10");
        campoIp.setPrefWidth(260);

        Label lblPuertoVal = new Label(String.valueOf(PUERTO));
        lblPuertoVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a237e;");

        gridConexion.add(new Label("IP del servidor:"), 0, 0); gridConexion.add(campoIp, 1, 0);
        gridConexion.add(new Label("Puerto:"),          0, 1); gridConexion.add(lblPuertoVal, 1, 1);
        panelConexion.setContent(gridConexion);

        // ── Sección datos del pasajero ────────────────────────────────────────
        TitledPane panelRegistro = new TitledPane();
        panelRegistro.setText("Datos del Pasajero");
        panelRegistro.setCollapsible(false);

        GridPane gridForm = new GridPane();
        gridForm.setHgap(10); gridForm.setVgap(14);
        gridForm.setPadding(new Insets(16));

        // DPI — solo dígitos y guiones
        campoDpi = new TextField();
        campoDpi.setPromptText("Ej: 1234567890101");
        campoDpi.setPrefWidth(260);
        campoDpi.textProperty().addListener((obs, ant, nuevo) -> {
            if (!nuevo.matches("[0-9\\-]*")) campoDpi.setText(ant);
        });

        // Nombre
        campoNombre = new TextField();
        campoNombre.setPromptText("Ej: Juan Pérez García");
        campoNombre.setPrefWidth(260);

        // Tipo de atención
        comboTipo = new ComboBox<>();
        comboTipo.getItems().addAll(
                TipoAtencion.GENERAL.name(),
                TipoAtencion.PRIORITARIA.name(),
                TipoAtencion.ESPECIAL.name()
        );
        comboTipo.setValue(TipoAtencion.GENERAL.name());
        comboTipo.setPrefWidth(260);
        Tooltip.install(comboTipo, new Tooltip(
                "GENERAL: cualquier pasajero\n" +
                        "PRIORITARIA: adultos mayores, embarazadas, discapacidad\n" +
                        "ESPECIAL: VIP u otros criterios"
        ));

        gridForm.add(new Label("DPI:"),               0, 0); gridForm.add(campoDpi,    1, 0);
        gridForm.add(new Label("Nombre completo:"),   0, 1); gridForm.add(campoNombre, 1, 1);
        gridForm.add(new Label("Tipo de atención:"),  0, 2); gridForm.add(comboTipo,   1, 2);
        panelRegistro.setContent(gridForm);

        // ── Botones ───────────────────────────────────────────────────────────
        HBox contenedorBotones = new HBox(12);
        contenedorBotones.setAlignment(Pos.CENTER);

        botonRegistrar = new Button("✔  Registrar Pasajero");
        botonRegistrar.setStyle(
                "-fx-background-color: #1a237e; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-font-size: 14px;" +
                        "-fx-padding: 10 20 10 20; -fx-cursor: hand;" +
                        "-fx-background-radius: 6;"
        );
        botonRegistrar.setPrefWidth(210);
        botonRegistrar.setDefaultButton(true);
        botonRegistrar.setOnAction(e -> accionRegistrar());

        Button botonLimpiar = new Button("✖  Limpiar");
        botonLimpiar.setStyle(
                "-fx-background-color: #e0e0e0; -fx-text-fill: #333;" +
                        "-fx-font-size: 13px; -fx-padding: 10 16 10 16;" +
                        "-fx-cursor: hand; -fx-background-radius: 6;"
        );
        botonLimpiar.setPrefWidth(110);
        botonLimpiar.setOnAction(e -> limpiarFormulario());

        contenedorBotones.getChildren().addAll(botonRegistrar, botonLimpiar);

        // ── Label de estado ───────────────────────────────────────────────────
        labelEstado = new Label("Listo para registrar pasajeros.");
        labelEstado.setWrapText(true);
        labelEstado.setMaxWidth(420);
        labelEstado.setAlignment(Pos.CENTER);
        labelEstado.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #555;" +
                        "-fx-background-color: #e8eaf6; -fx-padding: 10 16 10 16;" +
                        "-fx-background-radius: 6;"
        );

        // ── Ensamblar ─────────────────────────────────────────────────────────
        root.getChildren().addAll(
                titulo, subtitulo, sep,
                panelConexion,
                panelRegistro,
                contenedorBotones,
                labelEstado
        );

        return root;
    }

    // ── Lógica del botón Registrar ────────────────────────────────────────────

    private void accionRegistrar() {
        String dpi    = campoDpi.getText().trim();
        String nombre = campoNombre.getText().trim();
        String tipo   = comboTipo.getValue();
        String ip     = campoIp.getText().trim();

        // Validar campos vacíos
        if (dpi.isEmpty() || nombre.isEmpty() || ip.isEmpty()) {
            mostrarEstado("⚠  Todos los campos son obligatorios.", "#b71c1c", "#ffebee");
            return;
        }

        // Validar longitud mínima del DPI (sin guiones)
        if (dpi.replaceAll("-", "").length() < 9) {
            mostrarEstado("⚠  El DPI debe tener al menos 9 dígitos.", "#b71c1c", "#ffebee");
            return;
        }

        // Deshabilitar botón para evitar doble envío
        botonRegistrar.setDisable(true);
        mostrarEstado("⏳  Conectando con el servidor en " + ip + "...", "#555", "#e8eaf6");

        // ── Hilo separado para la operación de red ────────────────────────────
        // IMPORTANTE: nunca hacer operaciones de red en el hilo de JavaFX,
        // porque bloquea la interfaz gráfica mientras espera respuesta.
        Thread hiloRegistro = new Thread(() -> {
            ClienteSocket cliente = new ClienteSocket(ip, PUERTO);
            try {
                cliente.conectar();
                String respuesta = cliente.enviarRegistro(dpi, nombre, tipo);

                // Volver al hilo de JavaFX para actualizar la UI
                Platform.runLater(() -> procesarRespuesta(respuesta, dpi));

            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado(
                            "✖  No se pudo conectar al servidor.\n" +
                                    "Verifica la IP (" + ip + ") y que el servidor esté corriendo.",
                            "#b71c1c", "#ffebee"
                    );
                    botonRegistrar.setDisable(false);
                });
            } finally {
                cliente.desconectar();
            }
        });

        hiloRegistro.setDaemon(true); // El hilo muere cuando se cierra la app
        hiloRegistro.setName("hilo-registro-" + dpi);
        hiloRegistro.start();
    }

    // ── Procesar respuesta del servidor ──────────────────────────────────────

    /**
     * Interpreta el mensaje del servidor y actualiza la UI.
     * Formatos:
     *   "CONFIRMACION|dpi|numeroCola"
     *   "ERROR|mensajeError"
     */
    private void procesarRespuesta(String respuesta, String dpiEnviado) {
        if (respuesta == null) {
            mostrarEstado("✖  El servidor no respondió.", "#b71c1c", "#ffebee");
            botonRegistrar.setDisable(false);
            return;
        }

        String[] partes = respuesta.split("\\|");

        switch (partes[0]) {
            case "CONFIRMACION":
                String numeroCola = partes.length > 2 ? partes[2] : "?";
                mostrarEstado(
                        "✔  Pasajero registrado.\n" +
                                "DPI: " + dpiEnviado + "   |   Número en cola: #" + numeroCola,
                        "#1b5e20", "#e8f5e9"
                );
                limpiarCamposDatos(); // Preparar para siguiente registro
                break;

            case "ERROR":
                String msgError = partes.length > 1 ? partes[1] : "Error desconocido";
                mostrarEstado("✖  " + msgError, "#b71c1c", "#ffebee");
                break;

            default:
                mostrarEstado("⚠  Respuesta inesperada: " + respuesta, "#e65100", "#fff3e0");
                break;
        }

        botonRegistrar.setDisable(false);
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private void mostrarEstado(String texto, String colorTexto, String colorFondo) {
        labelEstado.setText(texto);
        labelEstado.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: " + colorTexto + ";" +
                        "-fx-background-color: " + colorFondo + "; -fx-padding: 10 16 10 16;" +
                        "-fx-background-radius: 6;"
        );
    }

    private void limpiarFormulario() {
        limpiarCamposDatos();
        mostrarEstado("Listo para registrar pasajeros.", "#555", "#e8eaf6");
    }

    private void limpiarCamposDatos() {
        campoDpi.clear();
        campoNombre.clear();
        comboTipo.setValue(TipoAtencion.GENERAL.name());
        campoDpi.requestFocus();
    }

    private void aplicarEstilos(Scene scene) {
        scene.getRoot().setStyle("-fx-background-color: #F5F7FA;");
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        launch(args);
    }
}