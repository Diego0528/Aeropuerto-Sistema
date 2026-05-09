package com.aeropuerto.especial;

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
 * Ventanilla operador — Cola Especial / VIP.
 * Muestra vuelo, asiento y beneficios activables del pasajero VIP.
 */
public class EspecialApp extends Application {

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

    private static final String[] CLASES = {
        "Primera Clase", "Clase Ejecutiva"
    };

    private ConexionServidor conexion;
    private String dpiActual = null;

    private Label     labelTurno;
    private Label     labelNombre;
    private Label     labelEstado;
    private ComboBox<String> comboVuelo;
    private ComboBox<String> comboClase;
    private TextField campoAsiento;
    private CheckBox  cbSalaVip;
    private CheckBox  cbBoardingPrioritario;
    private CheckBox  cbMenuEspecial;
    private CheckBox  cbEquipajeExtra;
    private CheckBox  cbFastTrack;
    private Button    botonLlamar;
    private Button    botonFinalizar;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Ventanilla VIP — Aeropuerto Guatemala");
        stage.setResizable(false);

        conexion = new ConexionServidor(HOST, PUERTO);
        boolean disponible = intentarConexion();

        Scene scene = new Scene(construirUI(disponible), 660, 610);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> conexion.desconectar());
        stage.show();
    }

    private VBox construirUI(boolean disponible) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f8f5ee;");

        // Encabezado dorado
        HBox header = new HBox();
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color: #7B5E00;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label("Cola VIP / Especial — Ventanilla Operador");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titulo.setTextFill(Color.WHITE);
        header.getChildren().add(titulo);

        // Panel pasajero actual
        VBox panelPasajero = new VBox(8);
        panelPasajero.setPadding(new Insets(18, 20, 18, 20));
        panelPasajero.setStyle(
            "-fx-background-color: #FFFDF0; -fx-border-color: #D4AF37; -fx-border-width: 0 0 2 0;"
        );

        Label lblSeccion = new Label("PASAJERO VIP EN ATENCION");
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
        labelTurno.setTextFill(Color.web("#7B5E00"));
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

        Label lblVuelo = new Label("Vuelo:");
        lblVuelo.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        comboVuelo = new ComboBox<>();
        comboVuelo.getItems().addAll(VUELOS);
        comboVuelo.setPromptText("Seleccionar vuelo...");
        comboVuelo.setPrefWidth(300);
        comboVuelo.setDisable(true);

        Label lblClase = new Label("Clase:");
        lblClase.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        comboClase = new ComboBox<>();
        comboClase.getItems().addAll(CLASES);
        comboClase.setPromptText("Seleccionar clase...");
        comboClase.setPrefWidth(200);
        comboClase.setDisable(true);

        Label lblAsiento = new Label("Numero de asiento:");
        lblAsiento.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        campoAsiento = new TextField();
        campoAsiento.setPromptText("Ej: 1A, 2B");
        campoAsiento.setPrefWidth(120);
        campoAsiento.setDisable(true);

        Label lblBeneficios = new Label("Beneficios activados:");
        lblBeneficios.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        cbSalaVip             = new CheckBox("Acceso Sala VIP");
        cbBoardingPrioritario = new CheckBox("Boarding prioritario");
        cbMenuEspecial        = new CheckBox("Menu especial a bordo");
        cbEquipajeExtra       = new CheckBox("Equipaje adicional (hasta 2 maletas)");
        cbFastTrack           = new CheckBox("Fast Track en seguridad");

        VBox beneficios = new VBox(8,
            cbSalaVip, cbBoardingPrioritario, cbMenuEspecial,
            cbEquipajeExtra, cbFastTrack
        );
        beneficios.setDisable(true);

        form.add(lblVuelo,      0, 0); form.add(comboVuelo,  1, 0);
        form.add(lblClase,      0, 1); form.add(comboClase,  1, 1);
        form.add(lblAsiento,    0, 2); form.add(campoAsiento, 1, 2);
        form.add(lblBeneficios, 0, 3); form.add(beneficios,  1, 3);

        // Botones
        HBox botones = new HBox(12);
        botones.setPadding(new Insets(4, 20, 16, 20));
        botones.setAlignment(Pos.CENTER_LEFT);

        botonLlamar = new Button("Llamar Siguiente VIP");
        botonLlamar.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        botonLlamar.setStyle(
            "-fx-background-color: #7B5E00; -fx-text-fill: white; " +
            "-fx-padding: 10 20 10 20; -fx-background-radius: 5;"
        );
        botonLlamar.setDisable(!disponible);
        botonLlamar.setOnAction(e -> llamarSiguiente(beneficios));

        botonFinalizar = new Button("Finalizar Atencion");
        botonFinalizar.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        botonFinalizar.setStyle(
            "-fx-background-color: #1E6B3C; -fx-text-fill: white; " +
            "-fx-padding: 10 20 10 20; -fx-background-radius: 5;"
        );
        botonFinalizar.setDisable(true);
        botonFinalizar.setOnAction(e -> finalizarAtencion(beneficios));

        botones.getChildren().addAll(botonLlamar, botonFinalizar);

        // Estado
        labelEstado = new Label(disponible
            ? "Conectado — Cola ESPECIAL / VIP"
            : "Sin conexion al servidor. Verifica que este corriendo.");
        labelEstado.setFont(Font.font("Arial", 12));
        labelEstado.setTextFill(disponible ? Color.web("#1E6B3C") : Color.web("#990000"));
        labelEstado.setPadding(new Insets(2, 20, 10, 20));
        labelEstado.setWrapText(true);

        root.getChildren().addAll(header, panelPasajero, form, botones, labelEstado);
        return root;
    }

    private void llamarSiguiente(VBox beneficios) {
        botonLlamar.setDisable(true);
        mostrarEstado("Consultando cola VIP...", true);

        new Thread(() -> {
            try {
                Mensaje resp = conexion.enviarYRecibir(Mensaje.llamarSiguiente(TipoAtencion.ESPECIAL));
                Platform.runLater(() -> {
                    if (resp.getTipo() == TipoMensaje.PASAJERO_LLAMADO) {
                        dpiActual = resp.getCampo(0);
                        String nombre = resp.getCampo(1);
                        String turno  = resp.getCampo(2);
                        labelTurno.setText("#" + turno);
                        labelNombre.setText(nombre);
                        comboVuelo.setDisable(false);
                        comboVuelo.getSelectionModel().clearSelection();
                        comboClase.setDisable(false);
                        comboClase.getSelectionModel().clearSelection();
                        campoAsiento.setDisable(false);
                        campoAsiento.clear();
                        beneficios.setDisable(false);
                        limpiarBeneficios();
                        botonFinalizar.setDisable(false);
                        mostrarEstado("Atendiendo VIP: " + nombre + " | Turno #" + turno, true);
                    } else if (resp.getTipo() == TipoMensaje.COLA_VACIA) {
                        mostrarEstado("Cola VIP vacia. No hay pasajeros en espera.", false);
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

    private void finalizarAtencion(VBox beneficios) {
        if (dpiActual == null) return;
        botonFinalizar.setDisable(true);
        botonLlamar.setDisable(true);

        System.out.println("[ESPECIAL] Finalizado — Vuelo: " + comboVuelo.getValue()
            + " | Clase: " + comboClase.getValue()
            + " | Asiento: " + campoAsiento.getText()
            + " | SalaVIP:" + cbSalaVip.isSelected()
            + " | Boarding:" + cbBoardingPrioritario.isSelected());

        new Thread(() -> {
            try {
                Mensaje resp = conexion.enviarYRecibir(Mensaje.finAtencion(dpiActual));
                Platform.runLater(() -> {
                    if (resp.getTipo() == TipoMensaje.CONFIRMACION) {
                        mostrarEstado("Atencion VIP finalizada correctamente.", true);
                    } else {
                        mostrarEstado("Error al finalizar: " + resp.getCampo(0), false);
                    }
                    limpiarPanel(beneficios);
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

    private void limpiarPanel(VBox beneficios) {
        dpiActual = null;
        labelTurno.setText("—");
        labelNombre.setText("Ningun pasajero en atencion");
        comboVuelo.setDisable(true);
        comboVuelo.getSelectionModel().clearSelection();
        comboClase.setDisable(true);
        comboClase.getSelectionModel().clearSelection();
        campoAsiento.setDisable(true);
        campoAsiento.clear();
        beneficios.setDisable(true);
        limpiarBeneficios();
        botonFinalizar.setDisable(true);
    }

    private void limpiarBeneficios() {
        cbSalaVip.setSelected(false);
        cbBoardingPrioritario.setSelected(false);
        cbMenuEspecial.setSelected(false);
        cbEquipajeExtra.setSelected(false);
        cbFastTrack.setSelected(false);
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
            System.out.println("[ESPECIAL] Sin conexion: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
