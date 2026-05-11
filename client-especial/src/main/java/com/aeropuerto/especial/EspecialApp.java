package com.aeropuerto.especial;

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
 * Ventanilla operador — Cola Especial / VIP.
 * Muestra vuelo, asiento y beneficios activables del pasajero VIP.
 *
 * DISEÑO: Glassmorphism oscuro navy — misma plantilla que RegistroApp.
 * Acento azul Apple (#64AAFF / #007AFF) — identidad de cola Especial.
 * Personalidad VIP: toques dorados en badge, strip y encabezado de beneficios.
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

    // ── Paleta glass navy ─────────────────────────────────────────────────────
    static final String T_DARK  = "rgba(255,255,255,0.95)";
    static final String T_MED   = "rgba(255,255,255,0.70)";
    static final String T_DIM   = "rgba(255,255,255,0.45)";

    // Acento — Cola Especial = Azul Apple
    static final String ACC_BG  = "rgba(0,122,255,0.15)";
    static final String ACC_BR  = "rgba(0,122,255,0.45)";
    static final String ACC_T   = "#64AAFF";
    static final String ACC_BAR = "#007AFF";

    // Dorado VIP — personalidad exclusiva de esta interfaz
    static final String VIP_BG   = "rgba(212,175,55,0.10)";
    static final String VIP_BR   = "rgba(212,175,55,0.40)";
    static final String VIP_T    = "#D4AF37";
    static final String VIP_GLOW = "rgba(212,175,55,0.18)";

    // Conexión
    static final String C_CONN_BG  = "rgba(52,199,89,0.12)";
    static final String C_CONN_BR  = "rgba(52,199,89,0.38)";
    static final String C_CONN_T   = "#4CD964";
    static final String C_CONN_DOT = "#34C759";

    // ── Estado ────────────────────────────────────────────────────────────────
    private ConexionServidor conexion;
    private String dpiActual = null;

    private Label            labelTurno;
    private Label            labelNombre;
    private Label            labelEstado;
    private ComboBox<String> comboVuelo;
    private ComboBox<String> comboClase;
    private TextField        campoAsiento;
    private CheckBox         cbSalaVip;
    private CheckBox         cbBoardingPrioritario;
    private CheckBox         cbMenuEspecial;
    private CheckBox         cbEquipajeExtra;
    private CheckBox         cbFastTrack;
    private Button           botonLlamar;
    private Button           botonFinalizar;

    private Label lblAvatarInicial;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        stage.setTitle("AeroQueue — Cola VIP");
        stage.setResizable(true);
        stage.setMinWidth(920);
        stage.setMinHeight(580);

        conexion = new ConexionServidor(HOST, PUERTO);
        boolean disponible = intentarConexion();

        VBox root = construirUI(disponible);
        Scene scene = new Scene(root);
        scene.setFill(Color.web("#0f1629"));
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> conexion.desconectar());
        stage.setWidth(1040);
        stage.setHeight(680);
        stage.show();

        root.setOpacity(0);
        root.setTranslateY(-8);
        new ParallelTransition(
                anim_fade(root, 0, 1, 320),
                anim_slide(root, -8, 0, 320)
        ).play();
    }

    // ── Raíz ─────────────────────────────────────────────────────────────────

    private VBox construirUI(boolean disponible) {
        // Inicializar CheckBoxes (no se agregan al scene graph directamente)
        cbSalaVip             = new CheckBox("Acceso Sala VIP");
        cbBoardingPrioritario = new CheckBox("Boarding prioritario");
        cbMenuEspecial        = new CheckBox("Menú especial a bordo");
        cbEquipajeExtra       = new CheckBox("Equipaje adicional (hasta 2 maletas)");
        cbFastTrack           = new CheckBox("Fast Track en seguridad");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a2744, #0f1629);");

        VBox app = new VBox(0);
        app.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-background-radius: 18;" +
                "-fx-border-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.22);" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,20,0.60), 40, 0, 0, 12);"
        );
        VBox.setVgrow(app, Priority.ALWAYS);
        VBox.setMargin(app, new Insets(35, 56, 35, 56));

        // beneficios VBox creado aquí y pasado a ambas columnas
        VBox beneficios = new VBox(6);
        beneficios.setDisable(true);
        beneficios.getChildren().addAll(
                crearCardBeneficio(cbSalaVip,             "🏛  Acceso Sala VIP"),
                crearCardBeneficio(cbBoardingPrioritario, "✈  Boarding prioritario"),
                crearCardBeneficio(cbMenuEspecial,        "🍽  Menú especial a bordo"),
                crearCardBeneficio(cbEquipajeExtra,       "🧳  Equipaje adicional (×2)"),
                crearCardBeneficio(cbFastTrack,           "⚡  Fast Track seguridad")
        );

        ScrollPane scroll = construirScrollContenido(disponible, beneficios);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        app.getChildren().addAll(construirNav(disponible), construirPaxStrip(), scroll);
        root.getChildren().add(app);
        VBox.setVgrow(app, Priority.ALWAYS);
        return root;
    }

    // ── Nav ───────────────────────────────────────────────────────────────────

    private HBox construirNav(boolean disponible) {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(15, 28, 15, 28));
        nav.setPrefHeight(48);
        nav.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07);" +
                "-fx-border-color: rgba(255,255,255,0.12);" +
                "-fx-border-width: 0 0 1 0;"
        );

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

        // Badge VIP con dorado — personalidad exclusiva
        Label queueBadge = new Label("✦  VIP");
        queueBadge.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        queueBadge.setTextFill(Color.web(VIP_T));
        queueBadge.setPadding(new Insets(3, 10, 3, 10));
        queueBadge.setStyle(
                "-fx-background-color: " + VIP_BG + ";" +
                "-fx-border-color: " + VIP_BR + ";" +
                "-fx-border-radius: 20;" +
                "-fx-background-radius: 20;" +
                "-fx-border-width: 1;" +
                "-fx-letter-spacing: 1.5px;" +
                "-fx-effect: dropshadow(gaussian, " + VIP_GLOW + ", 8, 0, 0, 0);"
        );

        HBox brand = new HBox(10, brandBox, brandName, queueBadge);
        brand.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(brand, Priority.ALWAYS);

        Label lblFecha = navLabel(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE dd / MM / yyyy", new java.util.Locale("es", "GT"))));
        Label lblHora = navLabel("--:--:--");

        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                lblHora.setText(java.time.LocalTime.now().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss")))));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();

        HBox connBadge = construirBadgeConexion(disponible);
        HBox derecha = new HBox(18, lblFecha, lblHora, connBadge);
        derecha.setAlignment(Pos.CENTER_RIGHT);

        nav.getChildren().addAll(brand, derecha);
        return nav;
    }

    private Label navLabel(String texto) {
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("Segoe UI", 11));
        lbl.setTextFill(Color.web(T_DIM));
        return lbl;
    }

    private HBox construirBadgeConexion(boolean conectado) {
        Circle punto = new Circle(2.5, Color.web(C_CONN_DOT));
        if (conectado) {
            ScaleTransition p = new ScaleTransition(Duration.millis(1200), punto);
            p.setFromX(1); p.setToX(1.6); p.setFromY(1); p.setToY(1.6);
            p.setCycleCount(Animation.INDEFINITE); p.setAutoReverse(true); p.play();
        }
        Label lbl = new Label(conectado ? "Conectado" : "Sin conexión");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lbl.setTextFill(Color.web(conectado ? C_CONN_T : "#D70015"));
        HBox badge = new HBox(5, punto, lbl);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(4, 10, 4, 10));
        badge.setStyle(
                "-fx-background-color: " + (conectado ? C_CONN_BG : "rgba(215,0,21,0.10)") + ";" +
                "-fx-border-color: " + (conectado ? C_CONN_BR : "rgba(215,0,21,0.40)") + ";" +
                "-fx-border-radius: 20;" +
                "-fx-background-radius: 20;" +
                "-fx-border-width: 1;"
        );
        return badge;
    }

    // ── Strip pasajero VIP ────────────────────────────────────────────────────

    private VBox construirPaxStrip() {
        VBox zona = new VBox(0);
        // Toque dorado: borde inferior dorado en lugar del blanco estándar
        zona.setStyle(
                "-fx-background-color: rgba(0,0,0,0.12);" +
                "-fx-border-color: " + VIP_BR + ";" +
                "-fx-border-width: 0 0 1 0;" +
                "-fx-effect: dropshadow(gaussian, " + VIP_GLOW + ", 20, 0, 0, 4);"
        );
        zona.setPadding(new Insets(16, 28, 18, 28));

        Label zoneLbl = secLabel("Pasajero VIP en atención");
        zoneLbl.setTextFill(Color.web("rgba(212,175,55,0.65)"));
        zoneLbl.setPadding(new Insets(0, 0, 12, 0));

        lblAvatarInicial = new Label("—");
        lblAvatarInicial.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblAvatarInicial.setTextFill(Color.web(ACC_T));
        StackPane avatar = new StackPane(lblAvatarInicial);
        avatar.setPrefSize(50, 50);
        avatar.setMinSize(50, 50);
        // Avatar con doble acento: borde dorado sobre fondo azul translúcido
        avatar.setStyle(
                "-fx-background-color: " + ACC_BG + ";" +
                "-fx-border-color: " + VIP_BR + ";" +
                "-fx-border-radius: 25;" +
                "-fx-background-radius: 25;" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, " + VIP_GLOW + ", 10, 0, 0, 0);"
        );

        labelTurno = new Label("—");
        labelTurno.setFont(Font.font("Consolas", FontWeight.BOLD, 46));
        labelTurno.setTextFill(Color.web(ACC_T));

        Rectangle sepV = new Rectangle(1, 55);
        sepV.setFill(Color.web("rgba(212,175,55,0.30)"));

        labelNombre = new Label("Ningún pasajero en atención");
        labelNombre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        labelNombre.setTextFill(Color.web(T_DARK));
        labelNombre.setWrapText(true);

        labelEstado = new Label("Cola VIP · Esperando acción");
        labelEstado.setFont(Font.font("Segoe UI", 11));
        labelEstado.setTextFill(Color.web(T_DIM));

        labelNombre.textProperty().addListener((obs, old, val) -> {
            boolean vacio = val == null || val.isBlank()
                    || val.toLowerCase().startsWith("ningún")
                    || val.toLowerCase().startsWith("ningun");
            if (vacio) {
                lblAvatarInicial.setText("—");
            } else {
                String[] p = val.trim().split("\\s+");
                lblAvatarInicial.setText(p.length >= 2
                        ? (p[0].substring(0, 1) + p[1].substring(0, 1)).toUpperCase()
                        : p[0].substring(0, 1).toUpperCase());
            }
        });

        VBox infoNombre = new VBox(4, labelNombre, labelEstado);
        HBox.setHgrow(infoNombre, Priority.ALWAYS);

        HBox strip = new HBox(20, avatar, labelTurno, sepV, infoNombre);
        strip.setAlignment(Pos.CENTER_LEFT);

        zona.getChildren().addAll(zoneLbl, strip);
        return zona;
    }

    // ── Scroll + Columnas ─────────────────────────────────────────────────────

    private ScrollPane construirScrollContenido(boolean disponible, VBox beneficios) {
        HBox cuerpo = new HBox(0);
        cuerpo.setStyle("-fx-background-color: transparent;");

        VBox colIzq = construirColIzquierda();
        VBox colDer  = construirColDerecha(disponible, beneficios);

        colIzq.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: rgba(255,255,255,0.12);" +
                "-fx-border-width: 0 1 0 0;"
        );
        HBox.setHgrow(colIzq, Priority.ALWAYS);

        cuerpo.getChildren().addAll(colIzq, colDer);

        ScrollPane sp = new ScrollPane(cuerpo);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-width: 0;"
        );
        return sp;
    }

    private VBox construirColIzquierda() {
        VBox col = new VBox(18);
        col.setPadding(new Insets(22, 28, 22, 28));

        // Vuelo
        Label lblVuelo = secLabel("Vuelo");
        lblVuelo.setPadding(new Insets(0, 0, 8, 0));
        comboVuelo = new ComboBox<>();
        comboVuelo.getItems().addAll(VUELOS);
        comboVuelo.setPromptText("Seleccionar vuelo...");
        comboVuelo.setMaxWidth(Double.MAX_VALUE);
        comboVuelo.setDisable(true);
        comboVuelo.setStyle(estiloCombo());
        col.getChildren().add(new VBox(0, lblVuelo, comboVuelo));
        col.getChildren().add(separador());

        // Clase
        Label lblClase = secLabel("Clase de vuelo");
        lblClase.setPadding(new Insets(0, 0, 8, 0));
        comboClase = new ComboBox<>();
        comboClase.getItems().addAll(CLASES);
        comboClase.setPromptText("Seleccionar clase...");
        comboClase.setMaxWidth(Double.MAX_VALUE);
        comboClase.setDisable(true);
        comboClase.setStyle(estiloCombo());
        col.getChildren().add(new VBox(0, lblClase, comboClase));
        col.getChildren().add(separador());

        // Asiento
        Label lblAsiento = secLabel("Número de asiento");
        lblAsiento.setPadding(new Insets(0, 0, 8, 0));
        campoAsiento = new TextField();
        campoAsiento.setPromptText("Ej: 1A, 2B");
        campoAsiento.setDisable(true);
        campoAsiento.setFont(Font.font("Segoe UI", 13));
        campoAsiento.setStyle(estiloInputGlass());
        col.getChildren().add(new VBox(0, lblAsiento, campoAsiento));

        VBox.setVgrow(col, Priority.ALWAYS);
        return col;
    }

    private VBox construirColDerecha(boolean disponible, VBox beneficios) {
        VBox col = new VBox(16);
        col.setPadding(new Insets(22, 24, 22, 24));
        col.setPrefWidth(360);
        col.setMinWidth(320);
        col.setStyle("-fx-background-color: rgba(0,0,0,0.10);");

        // Encabezado de beneficios con acento dorado
        Label lblBen = secLabel("Beneficios VIP ✦");
        lblBen.setTextFill(Color.web("rgba(212,175,55,0.65)"));
        col.getChildren().add(lblBen);
        col.getChildren().add(beneficios);

        col.getChildren().add(separador());

        Label lblAcc = secLabel("Acciones");
        col.getChildren().add(lblAcc);

        botonLlamar = new Button("Llamar siguiente VIP →");
        botonLlamar.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        botonLlamar.setMaxWidth(Double.MAX_VALUE);
        botonLlamar.setStyle(estiloBtn());
        botonLlamar.setDisable(!disponible);
        botonLlamar.setOnMouseEntered(e -> botonLlamar.setOpacity(0.85));
        botonLlamar.setOnMouseExited(e  -> botonLlamar.setOpacity(1.0));
        botonLlamar.setOnMousePressed(e -> animPress(botonLlamar, true));
        botonLlamar.setOnMouseReleased(e -> animPress(botonLlamar, false));
        botonLlamar.setOnAction(e -> llamarSiguiente(beneficios));

        botonFinalizar = new Button("Finalizar atención ✓");
        botonFinalizar.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        botonFinalizar.setMaxWidth(Double.MAX_VALUE);
        botonFinalizar.setStyle(estiloBtnSecundario());
        botonFinalizar.setDisable(true);
        botonFinalizar.setOnMouseEntered(e -> { if (!botonFinalizar.isDisabled()) botonFinalizar.setOpacity(0.80); });
        botonFinalizar.setOnMouseExited(e  -> botonFinalizar.setOpacity(1.0));
        botonFinalizar.setOnMousePressed(e -> animPress(botonFinalizar, true));
        botonFinalizar.setOnMouseReleased(e -> animPress(botonFinalizar, false));
        botonFinalizar.setOnAction(e -> finalizarAtencion(beneficios));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        col.getChildren().addAll(spacer, botonLlamar, botonFinalizar);
        return col;
    }

    // ── Beneficio glass card ──────────────────────────────────────────────────

    private HBox crearCardBeneficio(CheckBox cb, String etiqueta) {
        Circle dot = new Circle(5, Color.TRANSPARENT);
        dot.setStroke(Color.web("rgba(255,255,255,0.30)"));
        dot.setStrokeWidth(1.5);

        Label lbl = new Label(etiqueta);
        lbl.setFont(Font.font("Segoe UI", 13));
        lbl.setTextFill(Color.web(T_DIM));
        HBox.setHgrow(lbl, Priority.ALWAYS);

        HBox card = new HBox(12, dot, lbl);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(11, 14, 11, 14));
        card.setStyle(estiloCardBeneficio(false));
        card.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));

        cb.selectedProperty().addListener((obs, old, sel) -> {
            card.setStyle(estiloCardBeneficio(sel));
            if (sel) {
                dot.setFill(Color.web(ACC_BAR));
                dot.setStroke(Color.web(ACC_BR));
                lbl.setTextFill(Color.web(ACC_T));
            } else {
                dot.setFill(Color.TRANSPARENT);
                dot.setStroke(Color.web("rgba(255,255,255,0.30)"));
                lbl.setTextFill(Color.web(T_DIM));
            }
        });

        return card;
    }

    private String estiloCardBeneficio(boolean sel) {
        if (!sel) return
                "-fx-background-color: rgba(255,255,255,0.07);" +
                "-fx-border-color: rgba(255,255,255,0.16);" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-border-width: 1;" +
                "-fx-cursor: hand;";
        return
                "-fx-background-color: " + ACC_BG + ";" +
                "-fx-border-color: " + ACC_BR + ";" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-border-width: 1;" +
                "-fx-cursor: hand;";
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private Label secLabel(String texto) {
        Label lbl = new Label(texto.toUpperCase());
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        lbl.setTextFill(Color.web(T_DIM));
        lbl.setStyle("-fx-letter-spacing: 1.8px;");
        return lbl;
    }

    private HBox separador() {
        Rectangle r = new Rectangle(0, 1, Color.web("rgba(255,255,255,0.12)"));
        HBox sep = new HBox(r);
        r.widthProperty().bind(sep.widthProperty());
        return sep;
    }

    private String estiloInputGlass() {
        return "-fx-background-color: rgba(255,255,255,0.12);" +
               "-fx-border-width: 0;" +
               "-fx-background-radius: 10;" +
               "-fx-text-fill: rgba(255,255,255,0.92);" +
               "-fx-prompt-text-fill: rgba(255,255,255,0.32);" +
               "-fx-padding: 11 14 11 14;";
    }

    private String estiloCombo() {
        return "-fx-background-color: rgba(255,255,255,0.12);" +
               "-fx-border-width: 0;" +
               "-fx-background-radius: 10;" +
               "-fx-padding: 4 10 4 10;";
    }

    private String estiloBtn() {
        return "-fx-background-color: #007AFF;" +
               "-fx-border-width: 0;" +
               "-fx-background-radius: 12;" +
               "-fx-text-fill: white;" +
               "-fx-font-weight: bold;" +
               "-fx-padding: 13 22 13 22;" +
               "-fx-cursor: hand;" +
               "-fx-effect: dropshadow(gaussian, rgba(0,122,255,0.50), 12, 0, 0, 4);";
    }

    private String estiloBtnSecundario() {
        return "-fx-background-color: rgba(255,255,255,0.14);" +
               "-fx-border-color: rgba(255,255,255,0.22);" +
               "-fx-border-width: 1;" +
               "-fx-background-radius: 12;" +
               "-fx-border-radius: 12;" +
               "-fx-text-fill: rgba(255,255,255,0.88);" +
               "-fx-font-weight: bold;" +
               "-fx-padding: 13 22 13 22;" +
               "-fx-cursor: hand;";
    }

    private void animPress(Button b, boolean pressed) {
        ScaleTransition st = new ScaleTransition(Duration.millis(70), b);
        st.setToX(pressed ? 0.97 : 1.0);
        st.setToY(pressed ? 0.97 : 1.0);
        st.play();
    }

    private FadeTransition anim_fade(Node n, double from, double to, int ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), n);
        ft.setFromValue(from); ft.setToValue(to);
        return ft;
    }

    private TranslateTransition anim_slide(Node n, double from, double to, int ms) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), n);
        tt.setFromY(from); tt.setToY(to);
        tt.setInterpolator(Interpolator.EASE_OUT);
        return tt;
    }

    // ── Lógica original ───────────────────────────────────────────────────────

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
                        mostrarEstado("Cola VIP vacía. No hay pasajeros en espera.", false);
                    } else {
                        mostrarEstado("Respuesta inesperada: " + resp.serializar(), false);
                    }
                    botonLlamar.setDisable(false);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado("Error de conexión: " + e.getMessage(), false);
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
                        mostrarEstado("Atención VIP finalizada correctamente.", true);
                    } else {
                        mostrarEstado("Error al finalizar: " + resp.getCampo(0), false);
                    }
                    limpiarPanel(beneficios);
                    botonLlamar.setDisable(false);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarEstado("Error de conexión: " + e.getMessage(), false);
                    botonLlamar.setDisable(false);
                    botonFinalizar.setDisable(false);
                });
            }
        }).start();
    }

    private void limpiarPanel(VBox beneficios) {
        dpiActual = null;
        labelTurno.setText("—");
        labelNombre.setText("Ningún pasajero en atención");
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
        labelEstado.setTextFill(ok ? Color.web("#4CD964") : Color.web("#FF453A"));
    }

    private boolean intentarConexion() {
        try {
            conexion.conectar();
            return true;
        } catch (IOException e) {
            System.out.println("[ESPECIAL] Sin conexión: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
