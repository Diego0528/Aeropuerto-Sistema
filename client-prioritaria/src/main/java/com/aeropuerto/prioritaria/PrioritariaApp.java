package com.aeropuerto.prioritaria;

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
 * Ventanilla operador — Cola Prioritaria.
 * Muestra checklist de asistencia especial junto con el vuelo del pasajero.
 */
public class PrioritariaApp extends Application {

    private static final String HOST   = "localhost";
    private static final int    PUERTO = 5000;

    private static final String[] VUELOS = {
        "AM 123 — Ciudad de Mexico",
        "AA 456 — Miami",
        "UA 789 — Houston",
        "CM 101 — Ciudad de Panama",
        "IB 202 — Madrid",
        "LA 303 — Bogota",
        "AV 404 — Medellin",
        "NK 505 — Fort Lauderdale",
        "VB 606 — Cancun",
        "TB 707 — San Jose, CR"
    };

    private ConexionServidor conexion;
    private String dpiActual = null;

    private Label     labelTurno;
    private Label     labelNombre;
    private Label     labelEstado;
    private ComboBox<String> comboVuelo;
    private CheckBox  cbSillaRuedas;
    private CheckBox  cbAsistenciaCaminar;
    private CheckBox  cbOxigeno;
    private CheckBox  cbAcompanante;
    private CheckBox  cbEmbarazada;
    private CheckBox  cbAdultoMayor;
    private Button    botonLlamar;
    private Button    botonFinalizar;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Ventanilla Prioritaria — Aeropuerto Guatemala");
        stage.setResizable(false);

        conexion = new ConexionServidor(HOST, PUERTO);
        boolean disponible = intentarConexion();

        Scene scene = new Scene(construirUI(disponible), 640, 580);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> conexion.desconectar());
        stage.show();
    }

    private VBox construirUI(boolean disponible) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f0f4f8;");

        // Encabezado
        HBox header = new HBox();
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color: #5C3317;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label("Cola Prioritaria — Ventanilla Operador");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titulo.setTextFill(Color.WHITE);
        header.getChildren().add(titulo);

        // Panel pasajero actual
        VBox panelPasajero = new VBox(8);
        panelPasajero.setPadding(new Insets(18, 20, 18, 20));
        panelPasajero.setStyle(
            "-fx-background-color: white; -fx-border-color: #d4c4b0; -fx-border-width: 0 0 1 0;"
        );

        Label lblSeccion = new Label("PASAJERO EN ATENCION");
        lblSeccion.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        lblSeccion.setTextFill(Color.web("#888888"));

        HBox infoPasajero = new HBox(24);
        infoPasajero.setAlignment(Pos.CENTER_LEFT);

        VBox boxTurno = new VBox(2);
        Label lblTurnoTag = new Label("Turno");
        lblTurnoTag.setFont(Font.font("Arial", 11));
        lblTurnoTag.setTextFill(Color.web("#888888"));
        labelTurno = new Label("—");
        labelTurno.setFont(Font.font("Arial", FontWeight.BOLD, 44));
        labelTurno.setTextFill(Color.web("#5C3317"));
        boxTurno.getChildren().addAll(lblTurnoTag, labelTurno);

        Separator sepV = new Separator();
        sepV.setStyle("-fx-orientation: vertical;");
        sepV.setPrefHeight(60);

        VBox boxNombre = new VBox(4);
        Label lblNombreTag = new Label("Nombre");
        lblNombreTag.setFont(Font.font("Arial", 11));
        lblNombreTag.setTextFill(Color.web("#888888"));
        labelNombre = new Label("Ningun pasajero en atencion");
        labelNombre.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        labelNombre.setTextFill(Color.web("#333333"));
        labelNombre.setWrapText(true);
        boxNombre.getChildren().addAll(lblNombreTag, labelNombre);

        infoPasajero.getChildren().addAll(boxTurno, sepV, boxNombre);
        panelPasajero.getChildren().addAll(lblSeccion, infoPasajero);

        // Formulario
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(14);
        form.setPadding(new Insets(20, 20, 10, 20));

        Label lblVuelo = new Label("Vuelo confirmado:");
        lblVuelo.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        comboVuelo = new ComboBox<>();
        comboVuelo.getItems().addAll(VUELOS);
        comboVuelo.setPromptText("Seleccionar vuelo...");
        comboVuelo.setPrefWidth(340);
        comboVuelo.setDisable(true);

        Label lblChecklist = new Label("Asistencia requerida:");
        lblChecklist.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        cbSillaRuedas       = new CheckBox("Silla de ruedas");
        cbAsistenciaCaminar = new CheckBox("Asistencia para caminar");
        cbOxigeno           = new CheckBox("Oxigeno a bordo");
        cbAcompanante       = new CheckBox("Acompanante autorizado");
        cbEmbarazada        = new CheckBox("Embarazada (mas de 32 semanas)");
        cbAdultoMayor       = new CheckBox("Adulto mayor (mas de 65 anos)");

        VBox checklist = new VBox(8,
            cbSillaRuedas, cbAsistenciaCaminar, cbOxigeno,
            cbAcompanante, cbEmbarazada, cbAdultoMayor
        );
        checklist.setDisable(true);

        form.add(lblVuelo,     0, 0); form.add(comboVuelo, 1, 0);
        form.add(lblChecklist, 0, 1); form.add(checklist,  1, 1);

        // Botones
        HBox botones = new HBox(12);
        botones.setPadding(new Insets(4, 20, 16, 20));
        botones.setAlignment(Pos.CENTER_LEFT);

        botonLlamar = new Button("Llamar Siguiente");
        botonLlamar.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        botonLlamar.setStyle(
            "-fx-background-color: #5C3317; -fx-text-fill: white; " +
            "-fx-padding: 10 20 10 20; -fx-background-radius: 5;"
        );
        botonLlamar.setDisable(!disponible);
        botonLlamar.setOnAction(e -> llamarSiguiente(checklist));

        botonFinalizar = new Button("Finalizar Atencion");
        botonFinalizar.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        botonFinalizar.setStyle(
            "-fx-background-color: #1E6B3C; -fx-text-fill: white; " +
            "-fx-padding: 10 20 10 20; -fx-background-radius: 5;"
        );
        botonFinalizar.setDisable(true);
        botonFinalizar.setOnAction(e -> finalizarAtencion(checklist));

        botones.getChildren().addAll(botonLlamar, botonFinalizar);

        // Estado
        labelEstado = new Label(disponible
            ? "Conectado — Cola PRIORITARIA"
            : "Sin conexion al servidor. Verifica que este corriendo.");
        labelEstado.setFont(Font.font("Arial", 12));
        labelEstado.setTextFill(disponible ? Color.web("#1E6B3C") : Color.web("#990000"));
        labelEstado.setPadding(new Insets(2, 20, 10, 20));
        labelEstado.setWrapText(true);

        root.getChildren().addAll(header, panelPasajero, form, botones, labelEstado);
        return root;
    }

    private void llamarSiguiente(VBox checklist) {
        botonLlamar.setDisable(true);
        mostrarEstado("Consultando cola...", true);

        new Thread(() -> {
            try {
                Mensaje resp = conexion.enviarYRecibir(Mensaje.llamarSiguiente(TipoAtencion.PRIORITARIA));
                Platform.runLater(() -> {
                    if (resp.getTipo() == TipoMensaje.PASAJERO_LLAMADO) {
                        dpiActual = resp.getCampo(0);
                        String nombre = resp.getCampo(1);
                        String turno  = resp.getCampo(2);
                        labelTurno.setText("#" + turno);
                        labelNombre.setText(nombre);
                        comboVuelo.setDisable(false);
                        comboVuelo.getSelectionModel().clearSelection();
                        checklist.setDisable(false);
                        limpiarChecklist();
                        botonFinalizar.setDisable(false);
                        mostrarEstado("Atendiendo: " + nombre + " | Turno #" + turno, true);
                    } else if (resp.getTipo() == TipoMensaje.COLA_VACIA) {
                        mostrarEstado("Cola Prioritaria vacia. No hay pasajeros en espera.", false);
                    } else {
                        mostrarEstado("Respuesta inesperada: " + resp.serializar(), false);
                    }
                    botonLlamar.setDisable(false);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado("Error de conexion: " + e.getMessage(), false);
                    botonLlamar.setDisable(false);
                });
            }
        }).start();
    }

    private void finalizarAtencion(VBox checklist) {
        if (dpiActual == null) return;
        botonFinalizar.setDisable(true);
        botonLlamar.setDisable(true);

        System.out.println("[PRIORITARIA] Finalizado — Vuelo: " + comboVuelo.getValue()
            + " | SillaRuedas:" + cbSillaRuedas.isSelected()
            + " | Oxigeno:" + cbOxigeno.isSelected()
            + " | Acompanante:" + cbAcompanante.isSelected());

        new Thread(() -> {
            try {
                Mensaje resp = conexion.enviarYRecibir(Mensaje.finAtencion(dpiActual));
                Platform.runLater(() -> {
                    if (resp.getTipo() == TipoMensaje.CONFIRMACION) {
                        mostrarEstado("Atencion finalizada correctamente.", true);
                    } else {
                        mostrarEstado("Error al finalizar: " + resp.getCampo(0), false);
                    }
                    limpiarPanel(checklist);
                    botonLlamar.setDisable(false);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado("Error de conexion: " + e.getMessage(), false);
                    botonLlamar.setDisable(false);
                    botonFinalizar.setDisable(false);
                });
            }
        }).start();
    }

    private void limpiarPanel(VBox checklist) {
        dpiActual = null;
        labelTurno.setText("—");
        labelNombre.setText("Ningun pasajero en atencion");
        comboVuelo.setDisable(true);
        comboVuelo.getSelectionModel().clearSelection();
        checklist.setDisable(true);
        limpiarChecklist();
        botonFinalizar.setDisable(true);
    }

    private void limpiarChecklist() {
        cbSillaRuedas.setSelected(false);
        cbAsistenciaCaminar.setSelected(false);
        cbOxigeno.setSelected(false);
        cbAcompanante.setSelected(false);
        cbEmbarazada.setSelected(false);
        cbAdultoMayor.setSelected(false);
    }

    private void mostrarEstado(String msg, boolean ok) {
        labelEstado.setText(msg);
        labelEstado.setTextFill(ok ? Color.web("#1E6B3C") : Color.web("#990000"));
    }

    private boolean intentarConexion() {
        try {
            conexion.conectar();
            return true;
        } catch (IOException e) {
            System.out.println("[PRIORITARIA] Sin conexion: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
