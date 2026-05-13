package com.aeropuerto.prioritaria;

import com.aeropuerto.common.*;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Ventanilla operador — Cola Prioritaria.
 *
 * NUEVAS FUNCIONALIDADES:
 *   - Tiempo de atención medido desde llamada hasta finalización
 *   - Bloqueo: no se puede llamar otro ticket si hay uno en atención
 *   - Auto-reconexión si el servidor se cae
 *   - Errores específicos con causa real
 */
public class PrioritariaApp extends Application {

    private static final String HOST   = com.aeropuerto.common.ConfigServidor.getInstance().getHost();
    private static final int    PUERTO = com.aeropuerto.common.ConfigServidor.getInstance().getPuerto();

    private static final String[] VUELOS = {
        "AM 123 — Ciudad de Mexico", "AA 456 — Miami", "UA 789 — Houston",
        "CM 101 — Ciudad de Panama", "IB 202 — Madrid", "LA 303 — Bogota",
        "AV 404 — Medellin", "NK 505 — Fort Lauderdale",
        "VB 606 — Cancun", "TB 707 — San Jose, CR",
    };

    static final String T_DARK  = "rgba(255,255,255,0.95)";
    static final String T_MED   = "rgba(255,255,255,0.70)";
    static final String T_DIM   = "rgba(255,255,255,0.45)";

    // Acento naranja — Cola Prioritaria
    static final String ACC_BG  = "rgba(255,149,0,0.15)";
    static final String ACC_BR  = "rgba(255,149,0,0.45)";
    static final String ACC_T   = "#FF9F0A";
    static final String ACC_BAR = "#FF9500";

    static final String C_CONN_BG  = "rgba(52,199,89,0.12)";
    static final String C_CONN_BR  = "rgba(52,199,89,0.38)";
    static final String C_CONN_T   = "#4CD964";
    static final String C_CONN_DOT = "#34C759";

    // ── Estado ────────────────────────────────────────────────────────────────
    private ConexionServidor conexion;
    private String dpiActual = null;
    private long tiempoInicioAtencion = 0;

    private Label            labelTurno;
    private Label            labelNombre;
    private Label            labelEstado;
    private ComboBox<String> comboVuelo;
    private CheckBox         cbSillaRuedas;
    private CheckBox         cbAsistenciaCaminar;
    private CheckBox         cbOxigeno;
    private CheckBox         cbAcompanante;
    private CheckBox         cbEmbarazada;
    private CheckBox         cbAdultoMayor;
    private Button           botonLlamar;
    private Button           botonFinalizar;
    private Label            lblAvatarInicial;

    private HBox   badgeConexion;
    private Label  lblBadgeTexto;
    private Circle puntoBadge;

    @Override
    public void start(Stage stage) {
        stage.setTitle("AeroQueue — Cola Prioritaria");
        stage.setResizable(true);
        stage.setMinWidth(880);
        stage.setMinHeight(560);

        conexion = new ConexionServidor(HOST, PUERTO);
        conexion.setOnConexionPerdida(() -> Platform.runLater(this::marcarSinConexion));
        conexion.setOnConexionRestaurada(() -> Platform.runLater(this::marcarConectado));
        boolean disponible = intentarConexion();

        VBox root = construirUI(disponible);
        Scene scene = new Scene(root);
        scene.setFill(Color.web("#0f1629"));
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> conexion.desconectar());
        stage.setWidth(980);
        stage.setHeight(660);
        stage.show();

        root.setOpacity(0); root.setTranslateY(-8);
        new ParallelTransition(anim_fade(root, 0, 1, 320), anim_slide(root, -8, 0, 320)).play();
    }

    private VBox construirUI(boolean disponible) {
        cbSillaRuedas       = new CheckBox("Silla de ruedas");
        cbAsistenciaCaminar = new CheckBox("Asistencia para caminar");
        cbOxigeno           = new CheckBox("Oxígeno a bordo");
        cbAcompanante       = new CheckBox("Acompañante autorizado");
        cbEmbarazada        = new CheckBox("Embarazada (más de 32 semanas)");
        cbAdultoMayor       = new CheckBox("Adulto mayor (más de 65 años)");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a2744, #0f1629);");

        VBox app = new VBox(0);
        app.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-background-radius: 18; -fx-border-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.22); -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,20,0.60), 40, 0, 0, 12);"
        );
        VBox.setVgrow(app, Priority.ALWAYS);
        VBox.setMargin(app, new Insets(35, 56, 35, 56));

        VBox checklist = new VBox(6);
        checklist.setDisable(true);
        checklist.getChildren().addAll(
                crearCardChecklist(cbSillaRuedas,       "♿  Silla de ruedas"),
                crearCardChecklist(cbAsistenciaCaminar, "🚶  Asistencia para caminar"),
                crearCardChecklist(cbOxigeno,           "💨  Oxígeno a bordo"),
                crearCardChecklist(cbAcompanante,       "👤  Acompañante autorizado"),
                crearCardChecklist(cbEmbarazada,        "🤰  Embarazada (+32 semanas)"),
                crearCardChecklist(cbAdultoMayor,       "👴  Adulto mayor (+65 años)")
        );

        ScrollPane scroll = construirScrollContenido(disponible, checklist);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        app.getChildren().addAll(construirNav(disponible), construirPaxStrip(), scroll);
        root.getChildren().add(app);
        VBox.setVgrow(app, Priority.ALWAYS);
        return root;
    }

    private HBox construirNav(boolean disponible) {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(15, 28, 15, 28));
        nav.setPrefHeight(48);
        nav.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 0 0 1 0;");

        Rectangle brandIco = new Rectangle(22, 22);
        brandIco.setArcWidth(6); brandIco.setArcHeight(6);
        brandIco.setFill(Color.web("#007AFF"));
        Label brandIcoLbl = new Label("✈");
        brandIcoLbl.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        brandIcoLbl.setTextFill(Color.WHITE);
        StackPane brandBox = new StackPane(brandIco, brandIcoLbl);
        brandBox.setPrefSize(22, 22);

        Label brandName = new Label("AEROQUEUE");
        brandName.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        brandName.setTextFill(Color.web(T_MED));
        brandName.setStyle("-fx-letter-spacing: 1.8px;");

        Label queueBadge = new Label("PRIORITARIA");
        queueBadge.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        queueBadge.setTextFill(Color.web(ACC_T));
        queueBadge.setPadding(new Insets(3, 10, 3, 10));
        queueBadge.setStyle("-fx-background-color: " + ACC_BG + "; -fx-border-color: " + ACC_BR + "; -fx-border-radius: 20; -fx-background-radius: 20; -fx-border-width: 1; -fx-letter-spacing: 1.5px;");

        HBox brand = new HBox(10, brandBox, brandName, queueBadge);
        brand.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(brand, Priority.ALWAYS);

        Label lblFecha = navLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE dd / MM / yyyy", new java.util.Locale("es", "GT"))));
        Label lblHora = navLabel("--:--:--");
        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), e -> lblHora.setText(java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
        reloj.setCycleCount(Animation.INDEFINITE); reloj.play();

        badgeConexion = construirBadgeConexion(disponible);
        nav.getChildren().addAll(brand, new HBox(18, lblFecha, lblHora, badgeConexion));
        ((HBox) nav.getChildren().get(1)).setAlignment(Pos.CENTER_RIGHT);
        return nav;
    }

    private Label navLabel(String t) {
        Label l = new Label(t); l.setFont(Font.font("Segoe UI", 11)); l.setTextFill(Color.web(T_DIM)); return l;
    }

    private HBox construirBadgeConexion(boolean conectado) {
        puntoBadge = new Circle(2.5, Color.web(conectado ? C_CONN_DOT : "#D70015"));
        if (conectado) { ScaleTransition p = new ScaleTransition(Duration.millis(1200), puntoBadge); p.setFromX(1); p.setToX(1.6); p.setFromY(1); p.setToY(1.6); p.setCycleCount(Animation.INDEFINITE); p.setAutoReverse(true); p.play(); }
        lblBadgeTexto = new Label(conectado ? "Conectado" : "Sin conexión");
        lblBadgeTexto.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lblBadgeTexto.setTextFill(Color.web(conectado ? C_CONN_T : "#D70015"));
        badgeConexion = new HBox(5, puntoBadge, lblBadgeTexto);
        badgeConexion.setAlignment(Pos.CENTER); badgeConexion.setPadding(new Insets(4, 10, 4, 10));
        aplicarEstiloBadge(conectado);
        return badgeConexion;
    }

    private void aplicarEstiloBadge(boolean c) {
        if (badgeConexion == null) return;
        badgeConexion.setStyle("-fx-background-color: " + (c ? C_CONN_BG : "rgba(215,0,21,0.10)") + "; -fx-border-color: " + (c ? C_CONN_BR : "rgba(215,0,21,0.40)") + "; -fx-border-radius: 20; -fx-background-radius: 20; -fx-border-width: 1;");
    }

    private void marcarConectado() {
        if (puntoBadge != null) puntoBadge.setFill(Color.web(C_CONN_DOT));
        if (lblBadgeTexto != null) { lblBadgeTexto.setText("Conectado"); lblBadgeTexto.setTextFill(Color.web(C_CONN_T)); }
        aplicarEstiloBadge(true);
        if (dpiActual == null && botonLlamar != null) botonLlamar.setDisable(false);
        mostrarEstado("Conexión restaurada. Sistema listo.", true);
    }

    private void marcarSinConexion() {
        if (puntoBadge != null) puntoBadge.setFill(Color.web("#D70015"));
        if (lblBadgeTexto != null) { lblBadgeTexto.setText("Sin conexión"); lblBadgeTexto.setTextFill(Color.web("#D70015")); }
        aplicarEstiloBadge(false);
        if (botonLlamar != null) botonLlamar.setDisable(true);
        mostrarEstado("Conexión perdida. Reconectando cada 5 segundos...", false);
    }

    private VBox construirPaxStrip() {
        VBox zona = new VBox(0);
        zona.setStyle("-fx-background-color: rgba(0,0,0,0.10); -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 0 0 1 0;");
        zona.setPadding(new Insets(16, 28, 18, 28));

        Label zoneLbl = secLabel("Pasajero en atención"); zoneLbl.setPadding(new Insets(0, 0, 12, 0));

        lblAvatarInicial = new Label("—"); lblAvatarInicial.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); lblAvatarInicial.setTextFill(Color.web(ACC_T));
        StackPane avatar = new StackPane(lblAvatarInicial); avatar.setPrefSize(50, 50); avatar.setMinSize(50, 50);
        avatar.setStyle("-fx-background-color: " + ACC_BG + "; -fx-border-color: " + ACC_BR + "; -fx-border-radius: 25; -fx-background-radius: 25; -fx-border-width: 1.5;");

        labelTurno = new Label("—"); labelTurno.setFont(Font.font("Consolas", FontWeight.BOLD, 46)); labelTurno.setTextFill(Color.web(ACC_T));
        Rectangle sepV = new Rectangle(1, 55); sepV.setFill(Color.web("rgba(255,255,255,0.14)"));
        labelNombre = new Label("Ningún pasajero en atención"); labelNombre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18)); labelNombre.setTextFill(Color.web(T_DARK)); labelNombre.setWrapText(true);
        labelEstado = new Label("Cola Prioritaria · Esperando acción"); labelEstado.setFont(Font.font("Segoe UI", 11)); labelEstado.setTextFill(Color.web(T_DIM));

        labelNombre.textProperty().addListener((obs, old, val) -> {
            boolean vacio = val == null || val.isBlank() || val.toLowerCase().startsWith("ningún") || val.toLowerCase().startsWith("ningun");
            if (vacio) { lblAvatarInicial.setText("—"); } else { String[] p = val.trim().split("\\s+"); lblAvatarInicial.setText(p.length >= 2 ? (p[0].substring(0, 1) + p[1].substring(0, 1)).toUpperCase() : p[0].substring(0, 1).toUpperCase()); }
        });

        VBox infoNombre = new VBox(4, labelNombre, labelEstado); HBox.setHgrow(infoNombre, Priority.ALWAYS);
        HBox strip = new HBox(20, avatar, labelTurno, sepV, infoNombre); strip.setAlignment(Pos.CENTER_LEFT);
        zona.getChildren().addAll(zoneLbl, strip);
        return zona;
    }

    private ScrollPane construirScrollContenido(boolean disponible, VBox checklist) {
        VBox colIzq = construirColIzquierda(checklist);
        VBox colDer  = construirColDerecha(disponible, checklist);
        colIzq.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 0 1 0 0;");
        HBox.setHgrow(colIzq, Priority.ALWAYS);
        HBox cuerpo = new HBox(0, colIzq, colDer);
        cuerpo.setStyle("-fx-background-color: transparent;");
        ScrollPane sp = new ScrollPane(cuerpo); sp.setFitToWidth(true); sp.setFitToHeight(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        return sp;
    }

    private VBox construirColIzquierda(VBox checklist) {
        VBox col = new VBox(18); col.setPadding(new Insets(22, 28, 22, 28));
        Label lblVuelo = secLabel("Vuelo confirmado"); lblVuelo.setPadding(new Insets(0, 0, 8, 0));
        comboVuelo = new ComboBox<>(); comboVuelo.getItems().addAll(VUELOS);
        comboVuelo.setPromptText("Seleccionar vuelo..."); comboVuelo.setMaxWidth(Double.MAX_VALUE);
        comboVuelo.setDisable(true); comboVuelo.setStyle(estiloCombo());
        col.getChildren().add(new VBox(0, lblVuelo, comboVuelo));
        col.getChildren().add(separador());
        Label lblCheck = secLabel("Asistencia requerida"); lblCheck.setPadding(new Insets(0, 0, 8, 0));
        col.getChildren().add(new VBox(0, lblCheck, checklist));
        VBox.setVgrow(col, Priority.ALWAYS);
        return col;
    }

    private VBox construirColDerecha(boolean disponible, VBox checklist) {
        VBox col = new VBox(12); col.setPadding(new Insets(22, 24, 22, 24));
        col.setPrefWidth(300); col.setMinWidth(260);
        col.setStyle("-fx-background-color: rgba(0,0,0,0.10);");
        col.getChildren().add(secLabel("Acciones"));

        botonLlamar = new Button("Llamar siguiente →");
        botonLlamar.setFont(Font.font("Arial", FontWeight.BOLD, 13)); botonLlamar.setMaxWidth(Double.MAX_VALUE);
        botonLlamar.setStyle(estiloBtn()); botonLlamar.setDisable(!disponible);
        botonLlamar.setOnMouseEntered(e -> { if (!botonLlamar.isDisabled()) botonLlamar.setOpacity(0.85); });
        botonLlamar.setOnMouseExited(e -> botonLlamar.setOpacity(1.0));
        botonLlamar.setOnMousePressed(e -> animPress(botonLlamar, true));
        botonLlamar.setOnMouseReleased(e -> animPress(botonLlamar, false));
        botonLlamar.setOnAction(e -> llamarSiguiente(checklist));

        botonFinalizar = new Button("Finalizar atención ✓");
        botonFinalizar.setFont(Font.font("Arial", FontWeight.BOLD, 13)); botonFinalizar.setMaxWidth(Double.MAX_VALUE);
        botonFinalizar.setStyle(estiloBtnSecundario()); botonFinalizar.setDisable(true);
        botonFinalizar.setOnMouseEntered(e -> { if (!botonFinalizar.isDisabled()) botonFinalizar.setOpacity(0.80); });
        botonFinalizar.setOnMouseExited(e -> botonFinalizar.setOpacity(1.0));
        botonFinalizar.setOnMousePressed(e -> animPress(botonFinalizar, true));
        botonFinalizar.setOnMouseReleased(e -> animPress(botonFinalizar, false));
        botonFinalizar.setOnAction(e -> finalizarAtencion(checklist));

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        col.getChildren().addAll(spacer, botonLlamar, botonFinalizar);
        return col;
    }

    private HBox crearCardChecklist(CheckBox cb, String etiqueta) {
        Circle dot = new Circle(5, Color.TRANSPARENT); dot.setStroke(Color.web("rgba(255,255,255,0.30)")); dot.setStrokeWidth(1.5);
        Label lbl = new Label(etiqueta); lbl.setFont(Font.font("Segoe UI", 13)); lbl.setTextFill(Color.web(T_DIM)); HBox.setHgrow(lbl, Priority.ALWAYS);
        HBox card = new HBox(12, dot, lbl); card.setAlignment(Pos.CENTER_LEFT); card.setPadding(new Insets(11, 14, 11, 14));
        card.setStyle(estiloCardCheck(false)); card.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));
        cb.selectedProperty().addListener((obs, old, sel) -> {
            card.setStyle(estiloCardCheck(sel));
            if (sel) { dot.setFill(Color.web(ACC_BAR)); dot.setStroke(Color.web(ACC_BR)); lbl.setTextFill(Color.web(ACC_T)); }
            else { dot.setFill(Color.TRANSPARENT); dot.setStroke(Color.web("rgba(255,255,255,0.30)")); lbl.setTextFill(Color.web(T_DIM)); }
        });
        return card;
    }

    private String estiloCardCheck(boolean sel) {
        if (!sel) return "-fx-background-color: rgba(255,255,255,0.07); -fx-border-color: rgba(255,255,255,0.16); -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
        return "-fx-background-color: " + ACC_BG + "; -fx-border-color: " + ACC_BR + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
    }

    private Label secLabel(String t) { Label l = new Label(t.toUpperCase()); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9)); l.setTextFill(Color.web(T_DIM)); l.setStyle("-fx-letter-spacing: 1.8px;"); return l; }
    private HBox separador() { Rectangle r = new Rectangle(0, 1, Color.web("rgba(255,255,255,0.12)")); HBox s = new HBox(r); r.widthProperty().bind(s.widthProperty()); return s; }
    private String estiloCombo() { return "-fx-background-color: rgba(255,255,255,0.12); -fx-border-width: 0; -fx-background-radius: 10; -fx-padding: 4 10 4 10;"; }
    private String estiloBtn() { return "-fx-background-color: #007AFF; -fx-border-width: 0; -fx-background-radius: 12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 13 22 13 22; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,122,255,0.50), 12, 0, 0, 4);"; }
    private String estiloBtnSecundario() { return "-fx-background-color: rgba(255,255,255,0.14); -fx-border-color: rgba(255,255,255,0.22); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-text-fill: rgba(255,255,255,0.88); -fx-font-weight: bold; -fx-padding: 13 22 13 22; -fx-cursor: hand;"; }
    private void animPress(Button b, boolean p) { ScaleTransition st = new ScaleTransition(Duration.millis(70), b); st.setToX(p ? 0.97 : 1.0); st.setToY(p ? 0.97 : 1.0); st.play(); }
    private FadeTransition anim_fade(Node n, double f, double t, int ms) { FadeTransition ft = new FadeTransition(Duration.millis(ms), n); ft.setFromValue(f); ft.setToValue(t); return ft; }
    private TranslateTransition anim_slide(Node n, double f, double t, int ms) { TranslateTransition tt = new TranslateTransition(Duration.millis(ms), n); tt.setFromY(f); tt.setToY(t); tt.setInterpolator(Interpolator.EASE_OUT); return tt; }

    // ── Lógica principal ──────────────────────────────────────────────────────

    private void llamarSiguiente(VBox checklist) {
        // BLOQUEO: no llamar si hay atención en curso
        if (dpiActual != null) {
            mostrarEstado("Finaliza la atención actual antes de llamar al siguiente pasajero.", false);
            return;
        }

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
                        tiempoInicioAtencion = System.currentTimeMillis();
                        labelTurno.setText("#" + turno);
                        labelNombre.setText(nombre);
                        comboVuelo.setDisable(false);
                        comboVuelo.getSelectionModel().clearSelection();
                        checklist.setDisable(false);
                        limpiarChecklist();
                        botonFinalizar.setDisable(false);
                        botonLlamar.setDisable(true); // Bloquear hasta finalizar
                        mostrarEstado("En atención: " + nombre + " | Turno #" + turno, true);
                    } else if (resp.getTipo() == TipoMensaje.COLA_VACIA) {
                        mostrarEstado("Cola Prioritaria vacía. No hay pasajeros en espera.", false);
                        botonLlamar.setDisable(false);
                    } else if (resp.getTipo() == TipoMensaje.ERROR) {
                        mostrarEstado("Error del servidor: " + resp.getCampo(0), false);
                        botonLlamar.setDisable(false);
                    } else {
                        mostrarEstado("Respuesta inesperada: " + resp.getTipo(), false);
                        botonLlamar.setDisable(false);
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado(ConexionServidor.mensajeError(e), false);
                    botonLlamar.setDisable(false);
                });
            }
        }).start();
    }

    private void finalizarAtencion(VBox checklist) {
        if (dpiActual == null) return;

        long duracionSegundos = tiempoInicioAtencion > 0
            ? (System.currentTimeMillis() - tiempoInicioAtencion) / 1000 : 0;

        String vuelo = comboVuelo.getValue() != null ? comboVuelo.getValue() : "";
        // Construir resumen de asistencia para observaciones
        StringBuilder obs = new StringBuilder();
        if (cbSillaRuedas.isSelected())       obs.append("SillaRuedas ");
        if (cbAsistenciaCaminar.isSelected())  obs.append("AsistenciaCaminar ");
        if (cbOxigeno.isSelected())            obs.append("Oxigeno ");
        if (cbAcompanante.isSelected())        obs.append("Acompanante ");
        if (cbEmbarazada.isSelected())         obs.append("Embarazada ");
        if (cbAdultoMayor.isSelected())        obs.append("AdultoMayor ");

        botonFinalizar.setDisable(true);
        botonLlamar.setDisable(true);

        System.out.println("[PRIORITARIA] Fin — Vuelo: " + vuelo + " | Asistencia: " + obs
            + " | Duración: " + duracionSegundos + "s");

        final String dpiParaEnviar = dpiActual;

        new Thread(() -> {
            try {
                Mensaje resp = conexion.enviarYRecibir(
                    Mensaje.finAtencion(dpiParaEnviar, vuelo, obs.toString().trim(), duracionSegundos)
                );
                Platform.runLater(() -> {
                    if (resp.getTipo() == TipoMensaje.CONFIRMACION) {
                        String min = String.valueOf(duracionSegundos / 60);
                        String seg = String.format("%02d", duracionSegundos % 60);
                        mostrarEstado("Atención finalizada — Duración: " + min + "m " + seg + "s", true);
                    } else if (resp.getTipo() == TipoMensaje.ERROR) {
                        mostrarEstado("Error al finalizar: " + resp.getCampo(0), false);
                    } else {
                        mostrarEstado("Respuesta inesperada: " + resp.getTipo(), false);
                    }
                    limpiarPanel(checklist);
                    botonLlamar.setDisable(!conexion.isConectado());
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado(ConexionServidor.mensajeError(e), false);
                    botonFinalizar.setDisable(false);
                });
            }
        }).start();
    }

    private void limpiarPanel(VBox checklist) {
        dpiActual = null; tiempoInicioAtencion = 0;
        labelTurno.setText("—"); labelNombre.setText("Ningún pasajero en atención");
        comboVuelo.setDisable(true); comboVuelo.getSelectionModel().clearSelection();
        checklist.setDisable(true); limpiarChecklist();
        botonFinalizar.setDisable(true);
    }

    private void limpiarChecklist() {
        cbSillaRuedas.setSelected(false); cbAsistenciaCaminar.setSelected(false);
        cbOxigeno.setSelected(false); cbAcompanante.setSelected(false);
        cbEmbarazada.setSelected(false); cbAdultoMayor.setSelected(false);
    }

    private void mostrarEstado(String msg, boolean ok) {
        labelEstado.setText(msg);
        labelEstado.setTextFill(ok ? Color.web("#4CD964") : Color.web("#FF453A"));
    }

    private boolean intentarConexion() {
        try {
            String pc = java.net.InetAddress.getLocalHost().getHostName();
            conexion.conectarEIdentificar("PRIORITARIA", pc);
            return true;
        } catch (IOException e) {
            System.out.println("[PRIORITARIA] Sin conexión: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) { launch(args); }
}
