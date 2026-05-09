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
 * Kiosko de registro de pasajeros.
 * Consulta RENAP ficticio por DPI y autocompleta el nombre antes de encolarse.
 */
public class RegistroApp extends Application {

    private static final String HOST   = "localhost";
    private static final int    PUERTO = 5000;

    private ConexionServidor conexion;

    private TextField  campoDpi;
    private TextField  campoNombre;
    private ToggleGroup grupoTipo;
    private Label      labelEstado;
    private Label      labelRenap;
    private Button     botonRegistrar;
    private boolean    nombreDesdRenap = false;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Aeropuerto Guatemala — Registro de Pasajeros");
        stage.setResizable(false);

        conexion = new ConexionServidor(HOST, PUERTO);
        boolean servidorDisponible = intentarConexion();

        Scene scene = new Scene(construirUI(servidorDisponible), 520, 540);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> conexion.desconectar());
        stage.show();
    }

    private VBox construirUI(boolean servidorDisponible) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f4f6f9;");

        Label titulo = new Label("Registro de Pasajeros");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.web("#1F4E79"));

        Label subtitulo = new Label("Aeropuerto Internacional La Aurora — Guatemala");
        subtitulo.setFont(Font.font("Arial", 13));
        subtitulo.setTextFill(Color.web("#555555"));

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(14);
        form.setPadding(new Insets(8, 0, 8, 0));

        // DPI
        Label lblDpi = new Label("DPI del pasajero:");
        lblDpi.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        campoDpi = new TextField();
        campoDpi.setPromptText("Ej: 1234567890101  (13 dígitos)");
        campoDpi.setMaxWidth(300);
        campoDpi.setStyle("-fx-font-size: 13px;");

        // Etiqueta de resultado RENAP
        labelRenap = new Label("Ingresa el DPI para consultar RENAP");
        labelRenap.setFont(Font.font("Arial", 12));
        labelRenap.setTextFill(Color.web("#777777"));

        // Nombre
        Label lblNombre = new Label("Nombre completo:");
        lblNombre.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        campoNombre = new TextField();
        campoNombre.setPromptText("Se autocompleta desde RENAP");
        campoNombre.setMaxWidth(300);
        campoNombre.setStyle("-fx-font-size: 13px;");

        // Tipo de atención
        Label lblTipo = new Label("Tipo de atención:");
        lblTipo.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        grupoTipo = new ToggleGroup();
        RadioButton rbGeneral     = new RadioButton("General");
        RadioButton rbPrioritaria = new RadioButton("Prioritaria  (adultos mayores, embarazadas, discapacidad)");
        RadioButton rbEspecial    = new RadioButton("Especial  (VIP)");

        rbGeneral.setToggleGroup(grupoTipo);
        rbPrioritaria.setToggleGroup(grupoTipo);
        rbEspecial.setToggleGroup(grupoTipo);
        rbGeneral.setSelected(true);

        rbGeneral.setUserData(TipoAtencion.GENERAL);
        rbPrioritaria.setUserData(TipoAtencion.PRIORITARIA);
        rbEspecial.setUserData(TipoAtencion.ESPECIAL);

        VBox opcionesTipo = new VBox(6, rbGeneral, rbPrioritaria, rbEspecial);

        form.add(lblDpi,       0, 0); form.add(campoDpi,     1, 0);
        form.add(new Label(),  0, 1); form.add(labelRenap,   1, 1);
        form.add(lblNombre,    0, 2); form.add(campoNombre,  1, 2);
        form.add(lblTipo,      0, 3); form.add(opcionesTipo, 1, 3);

        botonRegistrar = new Button("Registrar en Cola");
        botonRegistrar.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        botonRegistrar.setStyle(
            "-fx-background-color: #1F4E79; -fx-text-fill: white; " +
            "-fx-padding: 10 24 10 24; -fx-background-radius: 6;"
        );
        botonRegistrar.setDisable(!servidorDisponible);
        botonRegistrar.setOnAction(e -> registrar());

        // DPI: buscar en RENAP al presionar Enter o al perder foco
        campoDpi.setOnAction(e -> consultarRenap());
        campoDpi.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !campoDpi.getText().isBlank()) consultarRenap();
        });

        campoNombre.setOnAction(e -> registrar());

        labelEstado = new Label(servidorDisponible
            ? "Conectado al servidor en " + HOST + ":" + PUERTO
            : "Sin conexion al servidor. Verifica que el servidor este corriendo.");
        labelEstado.setFont(Font.font("Arial", 13));
        labelEstado.setTextFill(servidorDisponible ? Color.web("#1E6B3C") : Color.web("#990000"));
        labelEstado.setWrapText(true);

        VBox contenedor = new VBox(16,
            titulo, subtitulo,
            new Separator(),
            form,
            new Separator(),
            botonRegistrar, labelEstado
        );
        contenedor.setAlignment(Pos.TOP_LEFT);

        root.getChildren().add(contenedor);
        return root;
    }

    private void consultarRenap() {
        String dpi = campoDpi.getText().trim();
        if (dpi.isEmpty()) return;

        CatalogoRENAP.DatosPersona datos = CatalogoRENAP.consultar(dpi);
        if (datos != null) {
            campoNombre.setText(datos.nombre);
            campoNombre.setEditable(false);
            campoNombre.setStyle("-fx-font-size: 13px; -fx-background-color: #eaf4ea;");
            labelRenap.setText("RENAP: datos encontrados (" + datos.fechaNacimiento + ")");
            labelRenap.setTextFill(Color.web("#1E6B3C"));
            nombreDesdRenap = true;
        } else {
            campoNombre.clear();
            campoNombre.setEditable(true);
            campoNombre.setStyle("-fx-font-size: 13px;");
            labelRenap.setText("DPI no encontrado en RENAP — ingresa el nombre manualmente");
            labelRenap.setTextFill(Color.web("#CC6600"));
            nombreDesdRenap = false;
            campoNombre.requestFocus();
        }
    }

    private void registrar() {
        String dpi    = campoDpi.getText().trim();
        String nombre = campoNombre.getText().trim();
        TipoAtencion tipo = (TipoAtencion) grupoTipo.getSelectedToggle().getUserData();

        if (dpi.isEmpty()) {
            mostrarEstado("Ingresa el DPI del pasajero.", false);
            campoDpi.requestFocus();
            return;
        }
        if (nombre.isEmpty()) {
            mostrarEstado("El nombre esta vacio. Verifica el DPI o ingresalo manualmente.", false);
            campoNombre.requestFocus();
            return;
        }

        botonRegistrar.setDisable(true);
        mostrarEstado("Enviando al servidor...", true);

        new Thread(() -> {
            try {
                Mensaje respuesta = conexion.enviarYRecibir(
                    Mensaje.registro(dpi, nombre, tipo)
                );
                Platform.runLater(() -> {
                    if (respuesta.getTipo() == TipoMensaje.CONFIRMACION) {
                        String turno = respuesta.getCampo(1);
                        mostrarEstado("Registrado. Turno #" + turno + " — Cola " + tipo, true);
                        limpiarFormulario();
                    } else {
                        mostrarEstado("Error: " + respuesta.getCampo(0), false);
                    }
                    botonRegistrar.setDisable(false);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado("Error de conexion: " + e.getMessage(), false);
                    botonRegistrar.setDisable(false);
                });
            }
        }).start();
    }

    private void mostrarEstado(String mensaje, boolean exito) {
        labelEstado.setText(mensaje);
        labelEstado.setTextFill(exito ? Color.web("#1E6B3C") : Color.web("#990000"));
    }

    private void limpiarFormulario() {
        campoDpi.clear();
        campoNombre.clear();
        campoNombre.setEditable(true);
        campoNombre.setStyle("-fx-font-size: 13px;");
        labelRenap.setText("Ingresa el DPI para consultar RENAP");
        labelRenap.setTextFill(Color.web("#777777"));
        nombreDesdRenap = false;
        campoDpi.requestFocus();
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

    public static void main(String[] args) {
        launch(args);
    }
}
