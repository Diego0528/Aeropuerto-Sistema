package com.aeropuerto.registro;

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
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * Kiosko de registro de pasajeros.
 *
 * DISEÑO: Glassmorphism claro estilo iOS/macOS
 *   Fondo:        gradiente azul-gris frío
 *   App card:     rgba(235,240,248,0.72) + blur(40px)
 *   Inputs:       rgba(60,72,95,0.28) — gris oscuro translúcido desde el inicio
 *   Acento:       rgba(255,255,255,0.65) — blanco glass
 *   Texto:        rgba(15,28,55,0.82)
 *
 * FLUJO:
 *   1. Ingresar DPI → buscar en RENAP
 *   2. Encontrado: nombre + fecha autocompletados, tipo asignado automático
 *      (edad ≥ 60 → Prioritaria, resto → General)
 *   3. No encontrado: nombre editable, tipo manual
 *   4. Operador puede cambiar el tipo si lo necesita
 *   5. Necesidades especiales + observaciones opcionales
 *   6. Generar ticket → servidor → TicketStage modal
 */
public class RegistroApp extends Application {

    private static final String HOST   = com.aeropuerto.common.ConfigServidor.getInstance().getHost();
    private static final int    PUERTO = com.aeropuerto.common.ConfigServidor.getInstance().getPuerto();

    // ── Paleta glass sobre fondo navy oscuro ─────────────────────────────────
    // rgba() SÍ funciona en JavaFX cuando el fondo padre es sólido y oscuro.
    // Card blanco-translúcido sobre navy → efecto ghost/cristal real.
    static final String BG_APP      = "rgba(255,255,255,0.10)";
    static final String CARD_NAV    = "rgba(255,255,255,0.07)";
    static final String CARD_DPI    = "rgba(0,0,0,0.10)";
    static final String CARD_COL_R  = "rgba(0,0,0,0.10)";
    static final String INPUT_BG    = "rgba(255,255,255,0.12)";
    static final String INPUT_FOCUS = "rgba(255,255,255,0.20)";
    static final String BORDE_GLASS  = "rgba(255,255,255,0.22)";
    static final String BORDE_GLASS2 = "rgba(255,255,255,0.12)";
    static final String BTN_GLASS    = "#007AFF";

    // Texto — blanco sobre fondo oscuro
    static final String T_DARK   = "rgba(255,255,255,0.95)";
    static final String T_MED    = "rgba(255,255,255,0.70)";
    static final String T_DIM    = "rgba(255,255,255,0.45)";
    static final String T_FAINT  = "rgba(255,255,255,0.28)";
    static final String T_WHITE  = "rgba(255,255,255,0.95)";
    static final String T_WHITE2 = "rgba(255,255,255,0.45)";

    // Cola General — verde Apple brillante (legible en oscuro)
    static final String C_GENERAL_BG  = "rgba(52,199,89,0.15)";
    static final String C_GENERAL_BR  = "rgba(52,199,89,0.45)";
    static final String C_GENERAL_T   = "#4CD964";
    static final String C_GENERAL_BAR = "#30D158";

    // Cola Prioritaria — naranja Apple brillante
    static final String C_PRIOR_BG    = "rgba(255,149,0,0.15)";
    static final String C_PRIOR_BR    = "rgba(255,149,0,0.45)";
    static final String C_PRIOR_T     = "#FF9F0A";
    static final String C_PRIOR_BAR   = "#FF9500";

    // Cola Especial — azul Apple brillante
    static final String C_ESPEC_BG    = "rgba(0,122,255,0.15)";
    static final String C_ESPEC_BR    = "rgba(0,122,255,0.45)";
    static final String C_ESPEC_T     = "#64AAFF";
    static final String C_ESPEC_BAR   = "#007AFF";

    // Estado conexión
    static final String C_CONN_BG  = "rgba(52,199,89,0.12)";
    static final String C_CONN_BR  = "rgba(52,199,89,0.38)";
    static final String C_CONN_T   = "#4CD964";
    static final String C_CONN_DOT = "#34C759";

    // ── Estado ────────────────────────────────────────────────────────────────
    private ConexionServidor conexion;
    private Stage            primaryStage;

    private TextField   campoDpi;
    private TextField   campoNombre;
    private TextField   campoFecha;
    private TextField   campoGenero;
    private TextField   campoNecesidades;
    private TextArea    campoObservaciones;
    private VBox        campoNombreZone;

    private ToggleGroup grupoTipo;
    private RadioButton rbGeneral, rbPrioritaria, rbEspecial;

    private HBox  bannerRenap;
    private Label lblBannerTitulo, lblBannerSub, lblBannerIco;
    private Label lblNotaDpi;
    private Label lblNotaTipo;
    private Label lblAvatarInicial;
    private Label lblPaxNombre, lblPaxSub;
    private HBox  paxStrip;
    private Button botonRegistrar;

    private String  razonTipo        = "";
    private boolean nombreDesdeRenap = false;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("AeroQueue — Registro");
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(620);

        conexion = new ConexionServidor(HOST, PUERTO);
        boolean conectado = intentarConexion();

        VBox root = construirUI(conectado);
        Scene scene = new Scene(root);
        scene.setFill(Color.web("#0f1629"));
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> conexion.desconectar());
        stage.setWidth(1080);
        stage.setHeight(660);
        stage.show();

        // Las capas internas del TextArea (scroll-pane, viewport, content) tienen
        // fondo propio que produce el recuadro gris. Se transparentan después de
        // que el nodo esté en escena, para que solo el outer box glass sea visible.
        Platform.runLater(() -> {
            for (String sel : new String[]{".scroll-pane", ".viewport", ".content"}) {
                Node n = campoObservaciones.lookup(sel);
                if (n != null) n.setStyle("-fx-background-color: transparent;");
            }
        });

        // Entrada suave
        root.setOpacity(0);
        root.setTranslateY(-8);
        new ParallelTransition(
                anim_fade(root, 0, 1, 320),
                anim_slide(root, -8, 0, 320)
        ).play();
    }

    // ── Raíz ─────────────────────────────────────────────────────────────────

    private VBox construirUI(boolean conectado) {
        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1a2744, #0f1629);"
        );

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

        ScrollPane scroll = construirScrollContenido(conectado);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        app.getChildren().addAll(construirNav(conectado), construirZonaDPI(), scroll);
        root.getChildren().add(app);
        VBox.setVgrow(app, Priority.ALWAYS);
        return root;
    }

    // ── Nav ───────────────────────────────────────────────────────────────────

    private HBox construirNav(boolean conectado) {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(15, 28, 15, 28));
        nav.setPrefHeight(48);
        nav.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07);" +
                        "-fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-width: 0 0 1 0;"
        );

        // Brand
        Rectangle brandIco = new Rectangle(22, 22);
        brandIco.setArcWidth(6); brandIco.setArcHeight(6);
        brandIco.setFill(Color.web("#007AFF"));
        brandIco.setStroke(Color.TRANSPARENT);
        brandIco.setStrokeWidth(0);
        Label brandIcoLbl = new Label("✈");
        brandIcoLbl.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        brandIcoLbl.setTextFill(Color.WHITE);
        StackPane brandBox = new StackPane(brandIco, brandIcoLbl);
        brandBox.setPrefSize(22, 22);

        Label brandName = new Label("AEROQUEUE");
        brandName.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        brandName.setTextFill(Color.web(T_MED));
        brandName.setStyle("-fx-letter-spacing: 1.8px;");

        HBox brand = new HBox(8, brandBox, brandName);
        brand.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(brand, Priority.ALWAYS);

        // Derecha: fecha, hora, conexión
        Label lblFecha = navLabel(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE dd / MM / yyyy", new java.util.Locale("es", "GT"))));
        Label lblHora  = navLabel("--:--:--");

        // Reloj en vivo
        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                lblHora.setText(java.time.LocalTime.now().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss")))));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();

        HBox connBadge = construirBadgeConexion(conectado);

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
                "-fx-background-color: " + (conectado ? C_CONN_BG : "#FFF0F0") + ";" +
                        "-fx-border-color: " + (conectado ? C_CONN_BR : "#FF3B30") + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-width: 1;"
        );
        return badge;
    }

    // ── Zona DPI ──────────────────────────────────────────────────────────────

    private VBox construirZonaDPI() {
        VBox zona = new VBox(0);
        zona.setStyle(
                "-fx-background-color: rgba(0,0,0,0.10);" +
                        "-fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-width: 0 0 1 0;"
        );
        zona.setPadding(new Insets(18, 28, 0, 28));

        Label zoneLbl = secLabel("Identificación del pasajero");
        zoneLbl.setPadding(new Insets(0, 0, 9, 0));

        // DPI input + botón
        campoDpi = new TextField();
        campoDpi.setPromptText("0000 0000 00000");
        campoDpi.setFont(Font.font("DM Mono", 22));
        campoDpi.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: rgba(255,255,255,0.35);" +
                        "-fx-border-width: 0 0 2 0;" +
                        "-fx-text-fill: rgba(255,255,255,0.95);" +
                        "-fx-padding: 0 0 6 0;"
        );
        campoDpi.setPrefWidth(280);
        campoDpi.setOnAction(e -> consultarRenap());
        campoDpi.focusedProperty().addListener((obs, old, f) -> {
            if (!f && !campoDpi.getText().isBlank()) consultarRenap();
        });

        Button btnBuscar = new Button("Buscar");
        btnBuscar.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        btnBuscar.setStyle(estiloBtnSecundario());
        btnBuscar.setOnMouseEntered(e -> btnBuscar.setStyle(estiloBtnSecundario() + "-fx-opacity: 0.80;"));
        btnBuscar.setOnMouseExited(e  -> btnBuscar.setStyle(estiloBtnSecundario()));
        btnBuscar.setOnAction(e -> consultarRenap());

        HBox dpiRow = new HBox(12, campoDpi, btnBuscar);
        dpiRow.setAlignment(Pos.BOTTOM_LEFT);

        lblNotaDpi = new Label("Ingresa el DPI para consultar el registro RENAP");
        lblNotaDpi.setFont(Font.font("Arial", 10));
        lblNotaDpi.setTextFill(Color.web(T_DIM));
        lblNotaDpi.setPadding(new Insets(6, 0, 0, 0));

        // Banner RENAP
        bannerRenap = construirBannerRenap();

        // Strip del pasajero
        paxStrip = construirPaxStrip();

        // Campo nombre — oculto hasta que RENAP no encuentre el DPI
        campoNombreZone = construirCampoInput(
                "Nombre del pasajero",
                "Apellidos y nombres completos",
                true
        );
        campoNombreZone.setPadding(new Insets(12, 0, 14, 0));
        campoNombreZone.setVisible(false);
        campoNombreZone.setManaged(false);

        zona.getChildren().addAll(zoneLbl, dpiRow, lblNotaDpi, bannerRenap, paxStrip, campoNombreZone);
        return zona;
    }

    private HBox construirBannerRenap() {
        lblBannerIco = new Label("✔");
        lblBannerIco.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lblBannerIco.setTextFill(Color.web(C_CONN_T));

        lblBannerTitulo = new Label("DPI encontrado en registro RENAP");
        lblBannerTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        lblBannerTitulo.setTextFill(Color.web(C_CONN_T));

        lblBannerSub = new Label("Datos precargados automáticamente. Verifica antes de continuar.");
        lblBannerSub.setFont(Font.font("Arial", 10));
        lblBannerSub.setTextFill(Color.web(T_FAINT));

        VBox textos = new VBox(2, lblBannerTitulo, lblBannerSub);
        HBox banner = new HBox(10, lblBannerIco, textos);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(10, 14, 10, 14));
        banner.setStyle(
                "-fx-background-color: " + C_CONN_BG + ";" +
                        "-fx-border-color: " + C_CONN_BR + ";" +
                        "-fx-border-radius: 9;" +
                        "-fx-background-radius: 9;" +
                        "-fx-border-width: 1;"
        );
        banner.setVisible(false);
        banner.setManaged(false);

        VBox wrapper = new VBox(banner);
        wrapper.setPadding(new Insets(10, 0, 0, 0));
        // Guardamos referencia directa
        this.bannerRenap = banner;
        return banner;
    }

    private HBox construirPaxStrip() {
        lblAvatarInicial = new Label("—");
        lblAvatarInicial.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblAvatarInicial.setTextFill(Color.web("#007AFF"));
        StackPane avatar = new StackPane(lblAvatarInicial);
        avatar.setPrefSize(40, 40);
        avatar.setMinSize(40, 40);
        avatar.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15);" +
                        "-fx-border-color: rgba(255,255,255,0.28);" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-width: 1.5;"
        );

        lblPaxNombre = new Label("—");
        lblPaxNombre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lblPaxNombre.setTextFill(Color.web(T_DARK));

        lblPaxSub = new Label("Ingresa un DPI para continuar");
        lblPaxSub.setFont(Font.font("Segoe UI", 11));
        lblPaxSub.setTextFill(Color.web(T_DIM));

        VBox info = new VBox(3, lblPaxNombre, lblPaxSub);

        HBox strip = new HBox(14, avatar, info);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.setPadding(new Insets(14, 0, 16, 0));
        strip.setStyle(
                "-fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-width: 1 0 0 0;"
        );
        return strip;
    }

    // ── Scroll + Cuerpo ───────────────────────────────────────────────────────

    private ScrollPane construirScrollContenido(boolean conectado) {
        HBox cuerpo = new HBox(0);
        cuerpo.setStyle("-fx-background-color: transparent;");

        VBox colIzq = construirColIzquierda();
        VBox colDer  = construirColDerecha();

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
        VBox col = new VBox(16);
        col.setPadding(new Insets(22, 28, 22, 28));

        // Tipos de atención
        col.getChildren().add(construirSeccionTipo());

        // Alerta automática
        lblNotaTipo = new Label("");
        lblNotaTipo.setFont(Font.font("Arial", 10));
        lblNotaTipo.setWrapText(true);
        lblNotaTipo.setVisible(false);
        lblNotaTipo.setManaged(false);
        col.getChildren().add(lblNotaTipo);

        col.getChildren().add(separador());

        // Necesidades especiales
        col.getChildren().add(construirCampoInput(
                "Necesidades especiales",
                "Ej: silla de ruedas, asistencia visual, acompañante…",
                false
        ));

        // Observaciones
        col.getChildren().add(construirCampoTextArea(
                "Observaciones del operador",
                "Notas adicionales, comentarios, situaciones particulares…"
        ));

        return col;
    }

    private VBox construirSeccionTipo() {
        VBox sec = new VBox(10);

        Label lbl = secLabel("Tipo de atención");
        lbl.setPadding(new Insets(0, 0, 2, 0));

        grupoTipo = new ToggleGroup();
        rbGeneral     = crearItemTipo("General",        "Sin condición especial — atención estándar",       TipoAtencion.GENERAL);
        rbPrioritaria = crearItemTipo("Prioritaria",    "Adulto mayor, embarazada o con discapacidad",      TipoAtencion.PRIORITARIA);
        rbEspecial    = crearItemTipo("Especial / VIP", "Tarjeta VIP, diplomático o servicio especial",     TipoAtencion.ESPECIAL);
        rbGeneral.setSelected(true);

        grupoTipo.selectedToggleProperty().addListener((obs, old, n) -> refrescarTipos());

        VBox lista = new VBox(6);
        for (RadioButton rb : new RadioButton[]{ rbGeneral, rbPrioritaria, rbEspecial }) {
            lista.getChildren().add((HBox) rb.getProperties().get("card"));
        }

        sec.getChildren().addAll(lbl, lista);
        refrescarTipos();
        return sec;
    }

    private RadioButton crearItemTipo(String nombre, String desc, TipoAtencion tipo) {
        RadioButton rb = new RadioButton();
        rb.setToggleGroup(grupoTipo);
        rb.setUserData(tipo);
        rb.setVisible(false); rb.setManaged(false);

        Circle circulo = new Circle(5, Color.TRANSPARENT);
        circulo.setStroke(Color.web("rgba(40,55,80,0.18)"));
        circulo.setStrokeWidth(1.5);

        Label lNom = new Label(nombre);
        lNom.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lNom.setTextFill(Color.web("rgba(40,55,80,0.38)"));

        Label lDes = new Label(desc);
        lDes.setFont(Font.font("Arial", 10));
        lDes.setTextFill(Color.web(T_FAINT));

        VBox textos = new VBox(3, lNom, lDes);
        HBox.setHgrow(textos, Priority.ALWAYS);

        // Badge AUTO — oculto por defecto
        Label autoBadge = new Label("Auto");
        autoBadge.setFont(Font.font("Arial", FontWeight.BOLD, 8));
        autoBadge.setVisible(false);
        autoBadge.setManaged(false);

        HBox card = new HBox(12, circulo, textos, autoBadge);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(13, 15, 13, 15));
        card.setStyle(estiloCardTipo(false, "g"));
        card.setOnMouseClicked(e -> rb.setSelected(true));

        rb.getProperties().put("card",      card);
        rb.getProperties().put("circulo",   circulo);
        rb.getProperties().put("lnombre",   lNom);
        rb.getProperties().put("autoBadge", autoBadge);
        rb.getProperties().put("tipoKey",
                tipo == TipoAtencion.GENERAL ? "g"
                        : tipo == TipoAtencion.PRIORITARIA ? "p" : "e");

        return rb;
    }

    private void refrescarTipos() {
        for (RadioButton rb : new RadioButton[]{ rbGeneral, rbPrioritaria, rbEspecial }) {
            HBox   card    = (HBox)   rb.getProperties().get("card");
            Circle circulo = (Circle) rb.getProperties().get("circulo");
            Label  lNom    = (Label)  rb.getProperties().get("lnombre");
            String key     = (String) rb.getProperties().get("tipoKey");
            if (card == null) continue;

            boolean sel = rb.isSelected();
            card.setStyle(estiloCardTipo(sel, key));

            if (sel) {
                String[] c = coloresParaTipo(key);
                circulo.setFill(Color.web(c[0]));
                circulo.setStroke(Color.web(c[1]));
                lNom.setTextFill(Color.web(c[2]));
            } else {
                circulo.setFill(Color.TRANSPARENT);
                circulo.setStroke(Color.web("rgba(255,255,255,0.30)"));
                lNom.setTextFill(Color.web(T_DIM));
            }
        }
    }

    private String estiloCardTipo(boolean sel, String key) {
        if (!sel) return
                "-fx-background-color: rgba(255,255,255,0.07);" +
                        "-fx-border-color: rgba(255,255,255,0.16);" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;";
        String[] c = coloresParaTipo(key);
        return
                "-fx-background-color: " + c[3] + ";" +
                        "-fx-border-color: " + c[4] + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;";
    }

    /** [fill, stroke, texto, bg, border] */
    private String[] coloresParaTipo(String key) {
        return switch (key) {
            case "g" -> new String[]{
                    C_GENERAL_BAR, C_GENERAL_BR, C_GENERAL_T, C_GENERAL_BG, C_GENERAL_BR};
            case "p" -> new String[]{
                    C_PRIOR_BAR,   C_PRIOR_BR,   C_PRIOR_T,   C_PRIOR_BG,   C_PRIOR_BR};
            default  -> new String[]{
                    C_ESPEC_BAR,   C_ESPEC_BR,   C_ESPEC_T,   C_ESPEC_BG,   C_ESPEC_BR};
        };
    }

    // ── Columna derecha ───────────────────────────────────────────────────────

    private VBox construirColDerecha() {
        VBox col = new VBox(14);
        col.setPadding(new Insets(22, 24, 22, 24));
        col.setPrefWidth(375);
        col.setMinWidth(340);
        col.setStyle("-fx-background-color: rgba(0,0,0,0.10);");

        Label lbl = secLabel("Vista previa del ticket");
        col.getChildren().add(lbl);

        // Ticket preview — se actualiza dinámicamente
        col.getChildren().add(construirTicketPreview());

        botonRegistrar = new Button("Generar ticket →");
        botonRegistrar.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        botonRegistrar.setMaxWidth(Double.MAX_VALUE);
        botonRegistrar.setStyle(estiloBtn());
        botonRegistrar.setOnMouseEntered(e -> botonRegistrar.setOpacity(0.85));
        botonRegistrar.setOnMouseExited(e  -> botonRegistrar.setOpacity(1.0));
        botonRegistrar.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(70), botonRegistrar);
            st.setToX(0.97); st.setToY(0.97); st.play();
        });
        botonRegistrar.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(70), botonRegistrar);
            st.setToX(1.0);  st.setToY(1.0);  st.play();
        });
        botonRegistrar.setOnAction(e -> registrar());
        VBox.setVgrow(botonRegistrar, Priority.NEVER);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        col.getChildren().addAll(spacer, botonRegistrar);

        return col;
    }

    // Guardamos referencias para actualizar el ticket en tiempo real
    private Label tkNomLabel, tkNumLabel, tkColaLabel, tkDpiVal, tkColaVal,
            tkFechaVal, tkHoraVal, tkNecesVal;
    private HBox  tkNecesRow;

    private VBox construirTicketPreview() {
        tkColaLabel = new Label("COLA GENERAL");
        tkColaLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        tkColaLabel.setTextFill(Color.web(C_GENERAL_T));
        tkColaLabel.setPadding(new Insets(3, 10, 3, 10));
        tkColaLabel.setStyle(
                "-fx-background-color: " + C_GENERAL_BG + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: " + C_GENERAL_BR + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1;"
        );

        tkNumLabel = new Label("G-??");
        tkNumLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 52));
        tkNumLabel.setTextFill(Color.web(C_GENERAL_T));

        tkNomLabel = new Label("—");
        tkNomLabel.setFont(Font.font("Segoe UI", 12));
        tkNomLabel.setTextFill(Color.web(T_DIM));

        Rectangle sep = new Rectangle();
        sep.setHeight(1);
        sep.setFill(Color.web("rgba(255,255,255,0.14)"));
        sep.widthProperty().bind(tkNomLabel.getScene() == null
                ? javafx.beans.binding.Bindings.createDoubleBinding(() -> 280.0)
                : tkNomLabel.widthProperty());

        // Grid de datos
        tkDpiVal   = tkFieldVal("—");
        tkColaVal  = tkFieldVal("—");
        tkFechaVal = tkFieldVal("—");
        tkHoraVal  = tkFieldVal("—");
        tkNecesVal = tkFieldVal("—");

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        grid.add(tkFieldGroup("DPI",    tkDpiVal),   0, 0);
        grid.add(tkFieldGroup("Cola",   tkColaVal),  1, 0);
        grid.add(tkFieldGroup("Fecha",  tkFechaVal), 0, 1);
        grid.add(tkFieldGroup("Hora",   tkHoraVal),  1, 1);

        tkNecesRow = new HBox(tkFieldGroup("Necesidades", tkNecesVal));
        tkNecesRow.setVisible(false);
        tkNecesRow.setManaged(false);
        GridPane.setColumnSpan(tkNecesRow, 2);
        grid.add(tkNecesRow, 0, 2);

        VBox card = new VBox(5,
                tkColaLabel, tkNumLabel, tkNomLabel,
                sep, grid
        );
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(22));
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                        "-fx-border-color: rgba(255,255,255,0.20);" +
                        "-fx-border-radius: 14;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 4);"
        );
        return card;
    }

    private VBox tkFieldGroup(String etiqueta, Label valor) {
        Label lbl = new Label(etiqueta.toUpperCase());
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 8));
        lbl.setTextFill(Color.web(T_FAINT));
        lbl.setStyle("-fx-letter-spacing: 1.5px;");
        return new VBox(3, lbl, valor);
    }

    private Label tkFieldVal(String texto) {
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("Consolas", 11));
        lbl.setTextFill(Color.web(T_MED));
        return lbl;
    }

    private void actualizarTicketPreview() {
        String nombre = campoNombre != null ? campoNombre.getText().trim() : "—";
        TipoAtencion tipo = grupoTipo.getSelectedToggle() != null
                ? (TipoAtencion) grupoTipo.getSelectedToggle().getUserData()
                : TipoAtencion.GENERAL;
        String key = tipo == TipoAtencion.GENERAL ? "g"
                : tipo == TipoAtencion.PRIORITARIA ? "p" : "e";
        String prefijo = tipo == TipoAtencion.GENERAL ? "G"
                : tipo == TipoAtencion.PRIORITARIA ? "P" : "E";
        String[] c = coloresParaTipo(key);

        tkNumLabel.setTextFill(Color.web(c[2]));
        tkNumLabel.setText(prefijo + "-??");
        String etqCola = tipo == TipoAtencion.GENERAL ? "COLA GENERAL"
                : tipo == TipoAtencion.PRIORITARIA ? "COLA PRIORITARIA" : "COLA ESPECIAL";
        tkColaLabel.setText(etqCola);
        tkColaLabel.setTextFill(Color.web(c[2]));
        tkColaLabel.setStyle(
                "-fx-background-color: " + c[3] + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: " + c[4] + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1;"
        );
        tkNomLabel.setText(nombre.isEmpty() ? "—" : nombre);

        String dpi = campoDpi.getText().trim().replaceAll("\\s+", "");
        tkDpiVal.setText(dpi.length() == 13 ? dpi : "—");
        tkColaVal.setText(tipo == TipoAtencion.GENERAL ? "General"
                : tipo == TipoAtencion.PRIORITARIA ? "Prioritaria" : "Especial");
        tkFechaVal.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        tkHoraVal.setText(java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        String nec = campoNecesidades != null ? campoNecesidades.getText().trim() : "";
        if (!nec.isEmpty()) {
            tkNecesVal.setText(nec);
            tkNecesRow.setVisible(true);
            tkNecesRow.setManaged(true);
        } else {
            tkNecesRow.setVisible(false);
            tkNecesRow.setManaged(false);
        }
    }

    // ── Helpers de campos ─────────────────────────────────────────────────────

    private VBox construirCampoInput(String etiqueta, String placeholder, boolean esNombre) {
        Label lbl = secLabel(etiqueta);
        lbl.setPadding(new Insets(0, 0, 8, 0));

        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setStyle(estiloInputOscuro());
        tf.setFont(Font.font("Arial", 12));

        if (esNombre) {
            this.campoNombre = tf;
            tf.setOnAction(e -> botonRegistrar.fire());
        } else {
            this.campoNecesidades = tf;
            tf.textProperty().addListener((obs, o, n) -> actualizarTicketPreview());
        }

        return new VBox(0, lbl, tf);
    }

    private VBox construirCampoTextArea(String etiqueta, String placeholder) {
        Label lbl = secLabel(etiqueta);
        lbl.setPadding(new Insets(0, 0, 8, 0));

        campoObservaciones = new TextArea();
        campoObservaciones.setPromptText(placeholder);
        campoObservaciones.setPrefRowCount(3);
        campoObservaciones.setWrapText(true);
        campoObservaciones.setFont(Font.font("Segoe UI", 12));
        // Outer box = glass igual que TextField. Capas internas → transparentes.
        campoObservaciones.setStyle(
                estiloInputOscuro() +
                        "-fx-background-insets: 0;" +
                        "-fx-control-inner-background: transparent;"
        );

        return new VBox(0, lbl, campoObservaciones);
    }

    private String estiloInputOscuro() {
        return
                "-fx-background-color: rgba(255,255,255,0.12);" +
                        "-fx-border-width: 0;" +
                        "-fx-background-radius: 10;" +
                        "-fx-text-fill: rgba(255,255,255,0.92);" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.32);" +
                        "-fx-padding: 11 14 11 14;";
    }

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

    private String estiloBtn() {
        return
                "-fx-background-color: #007AFF;" +
                        "-fx-border-width: 0;" +
                        "-fx-background-radius: 12;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 11 22 11 22;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,122,255,0.50), 12, 0, 0, 4);";
    }

    private String estiloBtnSecundario() {
        return
                "-fx-background-color: rgba(255,255,255,0.14);" +
                        "-fx-border-color: rgba(255,255,255,0.22);" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-text-fill: rgba(255,255,255,0.88);" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 18 8 18;" +
                        "-fx-cursor: hand;";
    }

    // ── Lógica RENAP ──────────────────────────────────────────────────────────

    private void consultarRenap() {
        String dpi = campoDpi.getText().trim().replaceAll("\\s+", "");
        if (dpi.isEmpty()) return;

        if (!dpi.matches("\\d{13}")) {
            lblNotaDpi.setText("DPI inválido — debe tener exactamente 13 dígitos numéricos");
            lblNotaDpi.setTextFill(Color.web("#FF453A"));
            agitar(campoDpi);
            ocultarBanner();
            return;
        }

        // ── Medir tiempo de búsqueda en RENAP ─────────────────────────────────
        long inicioNs = System.nanoTime();
        CatalogoRENAP.DatosPersona datos = CatalogoRENAP.consultar(dpi);
        long tiempoBusquedaMs = (System.nanoTime() - inicioNs) / 1_000_000;
        // ─────────────────────────────────────────────────────────────────────

        if (datos != null) {
            // Calcular edad
            int edad = calcularEdad(datos.fechaNacimiento);

            // Actualizar strip
            String iniciales = iniciales(datos.nombre);
            lblAvatarInicial.setText(iniciales);
            lblPaxNombre.setText(datos.nombre);
            lblPaxSub.setText(dpi + " · " + datos.fechaNacimiento + " · " + datos.genero);

            // Autocompletar campos
            if (campoNombre != null) {
                campoNombre.setText(datos.nombre);
                campoNombre.setEditable(false);
                campoNombre.setStyle(estiloInputOscuro() +
                        "-fx-background-color: rgba(255,255,255,0.07);");
            }
            if (campoNombreZone != null) { campoNombreZone.setVisible(false); campoNombreZone.setManaged(false); }
            if (campoFecha != null) campoFecha.setText(datos.fechaNacimiento);
            if (campoGenero != null) campoGenero.setText(datos.genero);

            // Tipo automático
            TipoAtencion tipoAuto = edad >= 60 ? TipoAtencion.PRIORITARIA : TipoAtencion.GENERAL;
            seleccionarTipo(tipoAuto);
            razonTipo = edad >= 60
                    ? "Adulto mayor (" + edad + " años)"
                    : "Sin condición prioritaria";
            nombreDesdeRenap = true;

            // Mostrar tiempo de búsqueda en el banner de éxito
            mostrarBanner(true,
                "Datos precargados automáticamente — búsqueda completada en " + tiempoBusquedaMs + " ms");
            lblNotaDpi.setText("");

            // Nota bajo tipos
            if (tipoAuto != TipoAtencion.GENERAL) {
                mostrarAlertaTipo("⊙  Prioritaria asignada automáticamente — " + razonTipo, C_PRIOR_T);
            } else {
                ocultarAlertaTipo();
            }

        } else {
            // No encontrado — mostrar tiempo de búsqueda también para dar feedback
            lblAvatarInicial.setText("?");
            lblPaxNombre.setText("DPI no encontrado");
            lblPaxSub.setText(dpi + " · No registrado en RENAP");
            limpiarDatos();
            ocultarBanner();
            lblNotaDpi.setText("DPI no encontrado en RENAP (" + tiempoBusquedaMs + " ms) — ingresa el nombre manualmente");
            lblNotaDpi.setTextFill(Color.web("#FF9F0A"));
            ocultarAlertaTipo();
            if (campoNombreZone != null) { campoNombreZone.setVisible(true); campoNombreZone.setManaged(true); }
            if (campoNombre != null) campoNombre.requestFocus();
        }

        actualizarTicketPreview();
    }

    private void seleccionarTipo(TipoAtencion tipo) {
        switch (tipo) {
            case PRIORITARIA -> rbPrioritaria.setSelected(true);
            case ESPECIAL    -> rbEspecial.setSelected(true);
            default          -> rbGeneral.setSelected(true);
        }
        // Mostrar/ocultar badge AUTO
        for (RadioButton rb : new RadioButton[]{ rbGeneral, rbPrioritaria, rbEspecial }) {
            Label badge = (Label) rb.getProperties().get("autoBadge");
            if (badge == null) continue;
            boolean esAuto = rb.isSelected() && tipo != TipoAtencion.GENERAL
                    && !razonTipo.contains("Sin condición");
            badge.setVisible(esAuto);
            badge.setManaged(esAuto);
            if (esAuto) {
                String key = (String) rb.getProperties().get("tipoKey");
                String[] c = coloresParaTipo(key);
                badge.setTextFill(Color.web(c[2]));
                badge.setStyle(
                        "-fx-background-color: " + c[3] + ";" +
                                "-fx-border-color: " + c[4] + ";" +
                                "-fx-border-radius: 20;" +
                                "-fx-background-radius: 20;" +
                                "-fx-border-width: 1;" +
                                "-fx-padding: 3 9 3 9;" +
                                "-fx-letter-spacing: 1px;"
                );
            }
        }
        refrescarTipos();
    }

    private void mostrarBanner(boolean exito, String sub) {
        lblBannerTitulo.setText(exito ? "DPI encontrado en registro RENAP" : "Error del servidor");
        lblBannerTitulo.setTextFill(Color.web(exito ? C_CONN_T : "rgba(180,60,60,0.85)"));
        lblBannerIco.setTextFill(Color.web(exito ? C_CONN_T : "rgba(180,60,60,0.85)"));
        lblBannerSub.setText(sub);
        bannerRenap.setStyle(exito
                ? "-fx-background-color: " + C_CONN_BG + ";-fx-border-color: " + C_CONN_BR + ";-fx-border-radius: 9;-fx-background-radius: 9;-fx-border-width: 1;"
                : "-fx-background-color: #FFF0F0;-fx-border-color: #FF3B30;-fx-border-radius: 9;-fx-background-radius: 9;-fx-border-width: 1;");

        if (!bannerRenap.isVisible()) {
            bannerRenap.setVisible(true);
            bannerRenap.setManaged(true);
            bannerRenap.setOpacity(0);
            anim_fade(bannerRenap, 0, 1, 220).play();
        }
    }

    private void ocultarBanner() {
        bannerRenap.setVisible(false);
        bannerRenap.setManaged(false);
    }

    private void mostrarAlertaTipo(String texto, String color) {
        lblNotaTipo.setText(texto);
        lblNotaTipo.setTextFill(Color.web(color));
        lblNotaTipo.setFont(Font.font("Segoe UI", 10));
        lblNotaTipo.setStyle(
                "-fx-background-color: " + C_PRIOR_BG + ";" +
                        "-fx-border-color: " + C_PRIOR_BR + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 9 12 9 12;"
        );
        lblNotaTipo.setVisible(true);
        lblNotaTipo.setManaged(true);
    }

    private void ocultarAlertaTipo() {
        lblNotaTipo.setVisible(false);
        lblNotaTipo.setManaged(false);
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    private void registrar() {
        String dpi    = campoDpi.getText().trim().replaceAll("\\s+", "");
        String nombre = campoNombre != null ? campoNombre.getText().trim() : "";
        TipoAtencion tipo = grupoTipo.getSelectedToggle() != null
                ? (TipoAtencion) grupoTipo.getSelectedToggle().getUserData()
                : TipoAtencion.GENERAL;

        if (!dpi.matches("\\d{13}")) { agitar(campoDpi); campoDpi.requestFocus(); return; }
        if (nombre.isEmpty()) {
            if (campoNombre != null) agitar(campoNombre);
            return;
        }

        String necesidades   = campoNecesidades   != null ? campoNecesidades.getText().trim()   : "";
        String observaciones = campoObservaciones != null ? campoObservaciones.getText().trim() : "";

        botonRegistrar.setDisable(true);
        botonRegistrar.setText("Enviando...");

        new Thread(() -> {
            try {
                Mensaje resp = conexion.enviarYRecibir(Mensaje.registro(dpi, nombre, tipo));
                Platform.runLater(() -> {
                    botonRegistrar.setDisable(false);
                    botonRegistrar.setText("Generar ticket →");
                    if (resp.getTipo() == TipoMensaje.CONFIRMACION) {
                        int turno = Integer.parseInt(resp.getCampo(1));
                        TicketStage.mostrar(
                                primaryStage, dpi, nombre, tipo, turno,
                                razonTipo, necesidades, this::limpiarFormulario
                        );
                    } else {
                        mostrarBanner(false, resp.getCampo(0));
                    }
                });
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    botonRegistrar.setDisable(false);
                    botonRegistrar.setText("Generar ticket →");
                    mostrarBanner(false, ConexionServidor.mensajeError(ex));
                });
            }
        }).start();
    }

    private void limpiarFormulario() {
        campoDpi.clear();
        limpiarDatos();
        if (campoNombreZone != null) { campoNombreZone.setVisible(false); campoNombreZone.setManaged(false); }
        lblAvatarInicial.setText("—");
        lblPaxNombre.setText("—");
        lblPaxSub.setText("Ingresa un DPI para continuar");
        ocultarBanner();
        ocultarAlertaTipo();
        lblNotaDpi.setText("Ingresa el DPI para consultar el registro RENAP");
        lblNotaDpi.setTextFill(Color.web(T_DIM));
        razonTipo        = "";
        nombreDesdeRenap = false;
        actualizarTicketPreview();
        campoDpi.requestFocus();
    }

    private void limpiarDatos() {
        if (campoNombre != null) {
            campoNombre.clear();
            campoNombre.setEditable(true);
            campoNombre.setStyle(estiloInputOscuro());
        }
        if (campoNecesidades   != null) campoNecesidades.clear();
        if (campoObservaciones != null) campoObservaciones.clear();
        seleccionarTipo(TipoAtencion.GENERAL);
        razonTipo = "";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int calcularEdad(String fechaNacimiento) {
        try {
            LocalDate nac = LocalDate.parse(fechaNacimiento,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return Period.between(nac, LocalDate.now()).getYears();
        } catch (Exception e) { return 0; }
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "?";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) return partes[0].substring(0, 1).toUpperCase();
        return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
    }

    private void agitar(Control ctrl) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(45), ctrl);
        tt.setFromX(0); tt.setByX(5); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.play();
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

    private boolean intentarConexion() {
        try {
            String pc = java.net.InetAddress.getLocalHost().getHostName();

            // Registrar callbacks de reconexión ANTES de conectar
            conexion.setOnConexionPerdida(() -> javafx.application.Platform.runLater(() ->
                mostrarBanner(false, "Conexión perdida con el servidor. Reconectando automáticamente...")));

            conexion.setOnConexionRestaurada(() -> javafx.application.Platform.runLater(() -> {
                ocultarBanner();
                lblNotaDpi.setText("Conexión restaurada. El sistema está listo.");
                lblNotaDpi.setTextFill(Color.web("#4CD964"));
            }));

            conexion.conectarEIdentificar("REGISTRO", pc);
            return true;
        } catch (java.net.ConnectException e) {
            System.out.println("[REGISTRO] Servidor no encontrado: " + e.getMessage());
            mostrarBanner(false, "Servidor no accesible en " + HOST + ":" + PUERTO + ". Verifique que esté encendido.");
            return false;
        } catch (IOException e) {
            System.out.println("[REGISTRO] Sin servidor: " + e.getMessage());
            mostrarBanner(false, "Error al conectar: " + ConexionServidor.mensajeError(e));
            return false;
        }
    }

    public static void main(String[] args) { launch(args); }
}