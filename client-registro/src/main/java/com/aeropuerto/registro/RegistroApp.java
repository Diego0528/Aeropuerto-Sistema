package com.aeropuerto.registro;

import com.aeropuerto.common.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Interfaz de registro de pasajeros.
 *
 * Permite al agente ingresar DPI, nombre y tipo de atención,
 * enviar el registro al servidor y ver la confirmación o error.
 *
 * RESPONSABLE: Pablo Acan (UI) + Diego Andrino (revisión de integración)
 */
public class RegistroApp extends Application {

    // ── Configuración de conexión ─────────────────────────────────────────────
    // TODO: Leer de config.properties antes de la entrega final
    private static final String HOST   = "localhost"; // Cambiar a IP del servidor en demo
    private static final int    PUERTO = 5000;

    // ── Estado ────────────────────────────────────────────────────────────────
    private ConexionServidor conexion;

    // ── Controles UI (referencias para leer/escribir durante eventos) ─────────
    private TextField     campoDpi;
    private TextField     campoNombre;
    private ToggleGroup   grupoTipo;
    private Label         labelEstado;
    private Button        botonRegistrar;

    // ── Inicio de JavaFX ──────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        stage.setTitle("Aeropuerto Guatemala — Registro de Pasajeros");
        stage.setResizable(false);

        // Conectar al servidor antes de mostrar la UI
        conexion = new ConexionServidor(HOST, PUERTO);
        boolean servidorDisponible = intentarConexion();

        Scene scene = new Scene(construirUI(servidorDisponible), 500, 480);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> conexion.desconectar());
        stage.show();
    }

    // ── Construcción de la UI ─────────────────────────────────────────────────

    private VBox construirUI(boolean servidorDisponible) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f4f6f9;");

        // Título
        Label titulo = new Label("✈  Registro de Pasajeros");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.web("#1F4E79"));

        Label subtitulo = new Label("Aeropuerto Internacional de Guatemala");
        subtitulo.setFont(Font.font("Arial", 13));
        subtitulo.setTextFill(Color.web("#555555"));

        Separator sep1 = new Separator();

        // ── Formulario ────────────────────────────────────────────────────────
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(14);
        form.setPadding(new Insets(8, 0, 8, 0));

        // DPI
        Label lblDpi = new Label("DPI del pasajero:");
        lblDpi.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        campoDpi = new TextField();
        campoDpi.setPromptText("Ej: 1234567890123");
        campoDpi.setMaxWidth(300);
        campoDpi.setStyle("-fx-font-size: 13px;");

        // Nombre
        Label lblNombre = new Label("Nombre completo:");
        lblNombre.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        campoNombre = new TextField();
        campoNombre.setPromptText("Ej: Juan Carlos Pérez");
        campoNombre.setMaxWidth(300);
        campoNombre.setStyle("-fx-font-size: 13px;");

        // Tipo de atención
        Label lblTipo = new Label("Tipo de atención:");
        lblTipo.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        grupoTipo = new ToggleGroup();
        RadioButton rbGeneral     = new RadioButton("General");
        RadioButton rbPrioritaria = new RadioButton("Prioritaria  (adultos mayores, embarazadas)");
        RadioButton rbEspecial    = new RadioButton("Especial  (VIP)");

        rbGeneral.setToggleGroup(grupoTipo);
        rbPrioritaria.setToggleGroup(grupoTipo);
        rbEspecial.setToggleGroup(grupoTipo);
        rbGeneral.setSelected(true); // Default

        rbGeneral.setUserData(TipoAtencion.GENERAL);
        rbPrioritaria.setUserData(TipoAtencion.PRIORITARIA);
        rbEspecial.setUserData(TipoAtencion.ESPECIAL);

        VBox opcionesTipo = new VBox(6, rbGeneral, rbPrioritaria, rbEspecial);

        form.add(lblDpi,       0, 0); form.add(campoDpi,     1, 0);
        form.add(lblNombre,    0, 1); form.add(campoNombre,  1, 1);
        form.add(lblTipo,      0, 2); form.add(opcionesTipo, 1, 2);

        // ── Botón registrar ───────────────────────────────────────────────────
        botonRegistrar = new Button("Registrar Pasajero");
        botonRegistrar.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        botonRegistrar.setStyle(
            "-fx-background-color: #1F4E79; -fx-text-fill: white; " +
            "-fx-padding: 10 24 10 24; -fx-background-radius: 6;"
        );
        botonRegistrar.setDisable(!servidorDisponible);
        botonRegistrar.setOnAction(e -> registrar());

        // Atajo de teclado: Enter en cualquier campo también registra
        campoDpi.setOnAction(e -> registrar());
        campoNombre.setOnAction(e -> registrar());

        // ── Label de estado ───────────────────────────────────────────────────
        labelEstado = new Label(servidorDisponible
            ? "✔  Conectado al servidor en " + HOST + ":" + PUERTO
            : "✘  Sin conexión al servidor. Verifica que esté corriendo.");
        labelEstado.setFont(Font.font("Arial", 13));
        labelEstado.setTextFill(servidorDisponible ? Color.web("#1E6B3C") : Color.web("#990000"));
        labelEstado.setWrapText(true);

        // ── Ensamblado ────────────────────────────────────────────────────────
        VBox contenedor = new VBox(16,
            titulo, subtitulo, sep1, form,
            new Separator(),
            botonRegistrar, labelEstado
        );
        contenedor.setAlignment(Pos.TOP_LEFT);
        contenedor.setPadding(new Insets(0));

        root.getChildren().add(contenedor);
        return root;
    }

    // ── Lógica de registro ────────────────────────────────────────────────────

    private void registrar() {
        String dpi    = campoDpi.getText().trim();
        String nombre = campoNombre.getText().trim();
        TipoAtencion tipo = (TipoAtencion) grupoTipo.getSelectedToggle().getUserData();

        // Validación local antes de enviar al servidor
        if (dpi.isEmpty()) {
            mostrarEstado("⚠  Ingresa el DPI del pasajero.", false);
            campoDpi.requestFocus();
            return;
        }
        if (nombre.isEmpty()) {
            mostrarEstado("⚠  Ingresa el nombre del pasajero.", false);
            campoNombre.requestFocus();
            return;
        }

        // Deshabilitar botón mientras se procesa (evita doble click)
        botonRegistrar.setDisable(true);
        mostrarEstado("Enviando al servidor...", true);

        // Enviar en hilo separado para no congelar la UI
        // (readLine() bloquea — si lo corres en el hilo de JavaFX, la ventana se congela)
        new Thread(() -> {
            try {
                Mensaje respuesta = conexion.enviarYRecibir(
                    Mensaje.registro(dpi, nombre, tipo)
                );

                // Volver al hilo de JavaFX para actualizar la UI
                Platform.runLater(() -> {
                    if (respuesta.getTipo() == TipoMensaje.CONFIRMACION) {
                        String turno = respuesta.getCampo(1); // numeroCola
                        mostrarEstado("✔  Registrado correctamente. Turno #" + turno + " (" + tipo + ")", true);
                        limpiarFormulario();
                    } else {
                        // ERROR
                        String error = respuesta.getCampo(0);
                        mostrarEstado("✘  Error: " + error, false);
                    }
                    botonRegistrar.setDisable(false);
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado("✘  Error de conexión: " + e.getMessage(), false);
                    botonRegistrar.setDisable(false);
                });
            }
        }).start();
    }

    // ── Helpers de UI ────────────────────────────────────────────────────────

    private void mostrarEstado(String mensaje, boolean exito) {
        labelEstado.setText(mensaje);
        labelEstado.setTextFill(exito ? Color.web("#1E6B3C") : Color.web("#990000"));
    }

    private void limpiarFormulario() {
        campoDpi.clear();
        campoNombre.clear();
        campoDpi.requestFocus();
        // El tipo de atención se deja igual — el agente probablemente
        // sigue registrando del mismo tipo
    }

    private boolean intentarConexion() {
        try {
            conexion.conectar();
            return true;
        } catch (IOException e) {
            System.out.println("[REGISTRO] No se pudo conectar al servidor: " + e.getMessage());
            return false;
        }
    }

    // ── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }
}
