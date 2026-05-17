package com.aeropuerto.logs;

import com.aeropuerto.common.ChatConexion;
import com.aeropuerto.common.ConfigServidor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ChatPanel {

    private static final String BG      = "#0f1629";
    private static final String BG_CARD = "rgba(255,255,255,0.06)";
    private static final String BG_HDR  = "rgba(255,255,255,0.04)";
    private static final String BORDE   = "rgba(255,255,255,0.12)";
    private static final String T_FULL  = "rgba(255,255,255,0.95)";
    private static final String T_DIM   = "rgba(255,255,255,0.40)";
    private static final String ACC     = "#007AFF";

    private static final String[] COLORES_PC = {
        "#32D74B", "#FF9F0A", "#64AAFF", "#BF5AF2", "#FF453A", "#30D158"
    };

    private Stage          ventana;
    private VBox           mensajesBox;
    private ScrollPane     scroll;
    private TextField      inputMensaje;
    private Label          lblEstado;
    private Circle         dotEstado;
    private ChatConexion   chat;
    private int            colorIdx = 0;
    private java.util.Map<String, String> colorPorPc = new java.util.HashMap<>();

    private Label badgeNotif;
    private int   mensajesNuevos = 0;

    public ChatPanel() {
        chat = new ChatConexion();
    }

    public Button crearBotonChat() {
        badgeNotif = new Label();
        badgeNotif.setFont(Font.font("Arial", FontWeight.BOLD, 8));
        badgeNotif.setTextFill(Color.WHITE);
        badgeNotif.setStyle(
            "-fx-background-color: #FF453A; -fx-background-radius: 8;" +
            "-fx-padding: 1 5 1 5; -fx-min-width: 14;"
        );
        badgeNotif.setVisible(false);
        badgeNotif.setManaged(false);

        Label lblChat = new Label("Chat");
        lblChat.setFont(Font.font("Segoe UI", 11));
        lblChat.setTextFill(Color.web("rgba(255,255,255,0.90)"));

        StackPane btnContent = new StackPane(lblChat, badgeNotif);
        StackPane.setAlignment(badgeNotif, Pos.TOP_RIGHT);
        StackPane.setMargin(badgeNotif, new Insets(-5, -5, 0, 0));
        btnContent.setMinWidth(40);

        Button btn = new Button();
        btn.setGraphic(btnContent);
        btn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-cursor: hand; -fx-padding: 5 14;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.16);" +
            "-fx-border-color: rgba(255,255,255,0.28);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-cursor: hand; -fx-padding: 5 14;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-cursor: hand; -fx-padding: 5 14;"
        ));
        btn.setOnAction(e -> abrirOCerrar());
        return btn;
    }

    private void abrirOCerrar() {
        if (ventana == null || !ventana.isShowing()) {
            mostrar();
        } else {
            ventana.close();
        }
    }

    public void mostrar() {
        if (ventana == null) construirVentana();
        mensajesNuevos = 0;
        if (badgeNotif != null) { badgeNotif.setVisible(false); badgeNotif.setManaged(false); }
        ventana.show();
        ventana.toFront();
        conectarChat();
    }

    private void construirVentana() {
        ventana = new Stage(StageStyle.DECORATED);
        ventana.setTitle("Chat Interno — AeroQueue");
        ventana.setWidth(360);
        ventana.setHeight(520);
        ventana.setMinWidth(300);
        ventana.setMinHeight(400);
        ventana.setResizable(true);
        ventana.setOnHiding(e -> {
            if (chat != null) chat.desconectar();
        });

        dotEstado = new Circle(4, Color.web("#FF453A"));
        lblEstado = new Label("Desconectado");
        lblEstado.setFont(Font.font("Segoe UI", 10));
        lblEstado.setTextFill(Color.web(T_DIM));

        HBox estado = new HBox(5, dotEstado, lblEstado);
        estado.setAlignment(Pos.CENTER_LEFT);

        Label pcNombre = new Label(chat.getMiNombre().isBlank()
            ? obtenerNombreLocal() : chat.getMiNombre());
        pcNombre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        pcNombre.setTextFill(Color.web(T_FULL));

        Label pcIp = new Label(chat.getMiIp().isBlank()
            ? obtenerIpLocal() : chat.getMiIp());
        pcIp.setFont(Font.font("Consolas", 10));
        pcIp.setTextFill(Color.web(T_DIM));

        VBox infoPC = new VBox(2, pcNombre, pcIp);
        HBox.setHgrow(infoPC, Priority.ALWAYS);

        HBox header = new HBox(10, infoPC, estado);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 14, 10, 14));
        header.setStyle(
            "-fx-background-color: " + BG_HDR + ";" +
            "-fx-border-color: transparent transparent " + BORDE + " transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );

        mensajesBox = new VBox(8);
        mensajesBox.setPadding(new Insets(10));
        mensajesBox.setStyle("-fx-background-color: transparent;");

        scroll = new ScrollPane(mensajesBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
            "-fx-background-color: transparent; -fx-background: transparent;" +
            "-fx-border-width: 0;"
        );
        VBox.setVgrow(scroll, Priority.ALWAYS);

        inputMensaje = new TextField();
        inputMensaje.setPromptText("Escribe un mensaje...");
        inputMensaje.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: rgba(255,255,255,0.90);" +
            "-fx-prompt-text-fill: rgba(255,255,255,0.30);" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 8 12;"
        );
        HBox.setHgrow(inputMensaje, Priority.ALWAYS);

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setStyle(
            "-fx-background-color: " + ACC + "; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 8 14; -fx-cursor: hand;"
        );
        btnEnviar.setOnAction(e -> enviar());
        inputMensaje.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) enviar(); });

        HBox inputBar = new HBox(8, inputMensaje, btnEnviar);
        inputBar.setPadding(new Insets(10, 14, 12, 14));
        inputBar.setStyle(
            "-fx-background-color: " + BG_HDR + ";" +
            "-fx-border-color: " + BORDE + " transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;"
        );

        VBox root = new VBox(0, header, scroll, inputBar);
        root.setStyle("-fx-background-color: " + BG + ";");

        Scene scene = new Scene(root);
        scene.setFill(Color.web(BG));
        ventana.setScene(scene);

        Platform.runLater(() -> {
            for (String sel : new String[]{".scroll-pane", ".viewport", ".content"}) {
                javafx.scene.Node n = scroll.lookup(sel);
                if (n != null) n.setStyle("-fx-background-color: transparent;");
            }
        });
    }

    private void conectarChat() {
        ConfigServidor cfg = ConfigServidor.getInstance();
        chat = new ChatConexion();
        chat.setListener(new ChatConexion.Listener() {
            @Override
            public void onMensaje(ChatConexion.MensajeChat m) {
                Platform.runLater(() -> agregarMensaje(m));
            }
            @Override
            public void onParticipante(ChatConexion.Participante p, boolean unido) {
                Platform.runLater(() -> agregarEventoSistema(
                    p.nombre() + " (" + p.ip() + ") " + (unido ? "se unio" : "salio")));
            }
            @Override
            public void onDesconexion() {
                Platform.runLater(() -> marcarEstado(false));
            }
            @Override
            public void onConexionRestaurada() {
                Platform.runLater(() -> marcarEstado(true));
            }
        });

        new Thread(() -> {
            try {
                chat.conectar(cfg.getHost(), cfg.getPuertoChat());
                Platform.runLater(() -> marcarEstado(true));
            } catch (Exception e) {
                Platform.runLater(() -> marcarEstado(false));
            }
        }, "chat-connect").start();
    }

    private void enviar() {
        String texto = inputMensaje.getText().trim();
        if (texto.isBlank() || !chat.isActivo()) return;
        chat.enviarMensaje(texto);
        inputMensaje.clear();
    }

    private void agregarMensaje(ChatConexion.MensajeChat m) {
        String color = colorParaPc(m.nombre());

        Label lblNombre = new Label(m.nombre());
        lblNombre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblNombre.setTextFill(Color.web(color));

        Label lblIp = new Label(m.ip());
        lblIp.setFont(Font.font("Consolas", 9));
        lblIp.setTextFill(Color.web(T_DIM));

        Label lblTexto = new Label(m.texto());
        lblTexto.setFont(Font.font("Segoe UI", 12));
        lblTexto.setTextFill(Color.web(T_FULL));
        lblTexto.setWrapText(true);
        lblTexto.setMaxWidth(Double.MAX_VALUE);

        Label lblTs = new Label(m.timestamp());
        lblTs.setFont(Font.font("Consolas", 9));
        lblTs.setTextFill(Color.web(T_DIM));

        HBox encabezado = new HBox(8, lblNombre, lblTs);
        encabezado.setAlignment(Pos.BASELINE_LEFT);

        VBox burbuja = new VBox(3, encabezado, lblIp, lblTexto);
        burbuja.setPadding(new Insets(9, 12, 9, 12));
        burbuja.setStyle(
            "-fx-background-color: " + BG_CARD + ";" +
            "-fx-border-color: " + color + "22;" +
            "-fx-border-radius: 10; -fx-background-radius: 10;" +
            "-fx-border-width: 1;"
        );

        mensajesBox.getChildren().add(burbuja);
        Platform.runLater(() -> scroll.setVvalue(1.0));

        if (ventana != null && !ventana.isFocused()) {
            mensajesNuevos++;
            if (badgeNotif != null) {
                badgeNotif.setText(String.valueOf(mensajesNuevos));
                badgeNotif.setVisible(true);
                badgeNotif.setManaged(true);
            }
        }
    }

    private void agregarEventoSistema(String texto) {
        Label lbl = new Label("• " + texto);
        lbl.setFont(Font.font("Segoe UI", 10));
        lbl.setTextFill(Color.web(T_DIM));
        lbl.setPadding(new Insets(2, 0, 2, 4));
        mensajesBox.getChildren().add(lbl);
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private void marcarEstado(boolean conectado) {
        if (dotEstado == null || lblEstado == null) return;
        dotEstado.setFill(Color.web(conectado ? "#32D74B" : "#FF453A"));
        lblEstado.setText(conectado ? "Conectado" : "Sin conexion — reconectando...");
        lblEstado.setTextFill(Color.web(conectado ? "#32D74B" : "#FF453A"));
    }

    private String colorParaPc(String nombre) {
        return colorPorPc.computeIfAbsent(nombre, k -> {
            String c = COLORES_PC[colorIdx % COLORES_PC.length];
            colorIdx++;
            return c;
        });
    }

    private static String obtenerNombreLocal() {
        try { return java.net.InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { return "PC-Local"; }
    }

    private static String obtenerIpLocal() {
        try { return java.net.InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { return "0.0.0.0"; }
    }
}