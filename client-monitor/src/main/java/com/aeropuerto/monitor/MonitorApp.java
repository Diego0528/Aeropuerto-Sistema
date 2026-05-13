package com.aeropuerto.monitor;

import com.aeropuerto.common.LogEntry;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitor de módulos — Task Manager estilo árbol.
 *
 * Funciones:
 * - Árbol de conexiones en tiempo real por tipo de módulo
 * - Auto-reconexión al servidor (cada 5 s)
 * - Panel de control del servidor: Start / Stop via ProcessBuilder,
 *   con mini-consola para ver el output del proceso
 */
public class MonitorApp extends Application {

    private static final String HOST   = com.aeropuerto.common.ConfigServidor.getInstance().getHost();
    private static final int    PUERTO = com.aeropuerto.common.ConfigServidor.getInstance().getPuerto();

    // ── Paleta dark navy glassmorphism ────────────────────────────────────────
    static final String BG_DARK   = "#0f1629";
    static final String BG_CARD   = "rgba(255,255,255,0.04)";
    static final String BG_BORDER = "rgba(255,255,255,0.10)";
    static final String T_BRIGHT  = "rgba(255,255,255,0.95)";
    static final String T_MED     = "rgba(255,255,255,0.65)";
    static final String T_DIM     = "rgba(255,255,255,0.40)";
    static final String ACC       = "#FF9F0A"; // ámbar — identidad monitor

    static final String C_REGISTRO    = "#FF9F0A";
    static final String C_GENERAL     = "#32D74B";
    static final String C_PRIORITARIA = "#FF9F0A";
    static final String C_ESPECIAL    = "#64AAFF";
    static final String C_MONITOR     = "#BF5AF2";
    static final String C_CONN        = "#32D74B";

    // ── Estado del árbol ──────────────────────────────────────────────────────

    private final Map<String, TreeItem<NodoModulo>> grupos     = new HashMap<>();
    private final Map<String, TreeItem<NodoModulo>> instancias = new HashMap<>();

    private TreeView<NodoModulo> arbol;
    private VBox                 panelDetalle;
    private Label                lblConexiones;
    private Label                lblEstado;
    private Circle               dotEstado;
    private int                  totalLogs = 0;
    private Label                lblLogs;

    private ConexionMonitor conexion;

    // ── Control del servidor ──────────────────────────────────────────────────
    private Process    procesosServidor;
    private TextField  rutaJar;
    private TextArea   consola;
    private Button     btnIniciar;
    private Button     btnDetener;

    // ── Tipos de módulo en orden de presentación ──────────────────────────────
    private static final String[] TIPOS_ORDEN = {
            "REGISTRO", "GENERAL", "PRIORITARIA", "ESPECIAL", "LOGS", "MONITOR"
    };

    // ── Aplicación ────────────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        stage.setTitle("AeroQueue — Monitor de Módulos");
        stage.setMinWidth(800);
        stage.setMinHeight(560);

        VBox root = construirUI();
        Scene scene = new Scene(root);
        scene.setFill(Color.web(BG_DARK));
        stage.setScene(scene);
        stage.setWidth(1060);
        stage.setHeight(740);
        stage.setOnCloseRequest(e -> {
            if (conexion != null) conexion.desconectar();
            detenerServidor();
        });
        stage.show();

        animarEntrada(root);
        conectarAsync();
    }

    // ── UI principal ──────────────────────────────────────────────────────────

    private VBox construirUI() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a2744, " + BG_DARK + ");");

        HBox cuerpo = construirCuerpo();
        VBox.setVgrow(cuerpo, Priority.ALWAYS);

        root.getChildren().addAll(construirTopBar(), cuerpo, construirPanelServidor());
        return root;
    }

    private HBox construirTopBar() {
        Label titulo = new Label("Monitor de Módulos");
        titulo.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + T_BRIGHT + ";");

        Label sub = new Label("Aeropuerto Guatemala — Conexiones en Tiempo Real");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: " + T_DIM + ";");

        VBox textos = new VBox(2, titulo, sub);

        dotEstado = new Circle(5);
        dotEstado.setFill(Color.web("#FF453A"));

        lblEstado = new Label("Desconectado");
        lblEstado.setStyle("-fx-font-size: 11px; -fx-text-fill: #FF453A;");

        HBox badgeConn = new HBox(6, dotEstado, lblEstado);
        badgeConn.setAlignment(Pos.CENTER_LEFT);
        badgeConn.setPadding(new Insets(5, 12, 5, 12));
        badgeConn.setStyle("-fx-background-color: rgba(255,69,58,0.10);" +
                "-fx-border-color: rgba(255,69,58,0.30); -fx-border-radius: 20; -fx-background-radius: 20;");

        lblConexiones = new Label("0 conexiones");
        lblConexiones.setStyle("-fx-font-size: 11px; -fx-text-fill: " + T_DIM + ";" +
                "-fx-background-color: rgba(255,255,255,0.06); -fx-padding: 4 12; -fx-background-radius: 12;");

        lblLogs = new Label("0 logs recibidos");
        lblLogs.setStyle("-fx-font-size: 11px; -fx-text-fill: " + T_DIM + ";" +
                "-fx-background-color: rgba(255,255,255,0.06); -fx-padding: 4 12; -fx-background-radius: 12;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(14, textos, spacer, lblLogs, lblConexiones, badgeConn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setStyle("-fx-background-color: rgba(255,255,255,0.03);" +
                "-fx-border-color: transparent transparent " + BG_BORDER + " transparent; -fx-border-width: 0 0 1 0;");
        return bar;
    }

    private HBox construirCuerpo() {
        arbol = construirArbol();

        Label lblArbolTitulo = new Label("MÓDULOS ACTIVOS");
        lblArbolTitulo.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + T_DIM + ";" +
                "-fx-letter-spacing: 1.5px;");

        VBox panelIzq = new VBox(10, lblArbolTitulo, arbol);
        panelIzq.setPadding(new Insets(16));
        panelIzq.setPrefWidth(380);
        panelIzq.setMinWidth(300);
        VBox.setVgrow(arbol, Priority.ALWAYS);
        panelIzq.setStyle("-fx-background-color: " + BG_CARD + ";" +
                "-fx-border-color: transparent " + BG_BORDER + " transparent transparent; -fx-border-width: 0 1 0 0;");

        panelDetalle = construirPanelDetalle(null);

        VBox panelDer = new VBox(panelDetalle);
        panelDer.setPadding(new Insets(16));
        HBox.setHgrow(panelDer, Priority.ALWAYS);
        VBox.setVgrow(panelDetalle, Priority.ALWAYS);

        HBox cuerpo = new HBox(panelIzq, panelDer);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        return cuerpo;
    }

    // ── Panel de control del servidor ─────────────────────────────────────────

    private VBox construirPanelServidor() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(12, 20, 12, 20));
        panel.setStyle(
                "-fx-background-color: rgba(0,0,0,0.25);" +
                "-fx-border-color: " + BG_BORDER + " transparent transparent transparent;" +
                "-fx-border-width: 1 0 0 0;"
        );

        Label titulo = new Label("CONTROL DEL SERVIDOR");
        titulo.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + ACC + ";" +
                "-fx-letter-spacing: 1.5px;");

        // Fila: ruta JAR + botones
        rutaJar = new TextField("server-1.0-SNAPSHOT.jar");
        rutaJar.setPromptText("Ruta al server JAR (relativa o absoluta)...");
        rutaJar.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: " + T_BRIGHT + ";" +
                "-fx-prompt-text-fill: " + T_DIM + "; -fx-border-color: " + BG_BORDER + ";" +
                "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 5 10; -fx-font-size: 12px;"
        );
        HBox.setHgrow(rutaJar, Priority.ALWAYS);

        btnIniciar = new Button("▶  Iniciar Servidor");
        btnIniciar.setStyle(estiloBoton("#32D74B44", "#32D74B"));
        btnIniciar.setOnAction(e -> iniciarServidor());

        btnDetener = new Button("■  Detener");
        btnDetener.setStyle(estiloBoton("rgba(255,69,58,0.25)", "#FF453A"));
        btnDetener.setDisable(true);
        btnDetener.setOnAction(e -> detenerServidor());

        HBox fila = new HBox(10, rutaJar, btnIniciar, btnDetener);
        fila.setAlignment(Pos.CENTER_LEFT);

        // Mini-consola del servidor
        consola = new TextArea();
        consola.setEditable(false);
        consola.setPrefRowCount(5);
        consola.setStyle(
                "-fx-control-inner-background: rgba(0,0,0,0.50);" +
                "-fx-background-color: rgba(0,0,0,0.50);" +
                "-fx-text-fill: #A0FF80; -fx-font-family: 'Consolas'; -fx-font-size: 11px;" +
                "-fx-border-color: " + BG_BORDER + "; -fx-border-radius: 6; -fx-background-radius: 6;"
        );
        consola.setPromptText("Output del servidor aparecerá aquí...");

        panel.getChildren().addAll(titulo, fila, consola);
        return panel;
    }

    private String estiloBoton(String bg, String textColor) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + textColor + ";" +
                "-fx-border-color: " + textColor + "44; -fx-border-radius: 6; -fx-background-radius: 6;" +
                "-fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;";
    }

    // ── Lógica del servidor ───────────────────────────────────────────────────

    private void iniciarServidor() {
        String jar = rutaJar.getText().trim();
        if (jar.isEmpty()) {
            appendConsola("[ERROR] Especifica la ruta al archivo JAR del servidor.");
            return;
        }
        if (procesosServidor != null && procesosServidor.isAlive()) {
            appendConsola("[INFO] El servidor ya está en ejecución.");
            return;
        }

        appendConsola("[INFO] Iniciando servidor: java -jar " + jar);
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", jar);
            pb.redirectErrorStream(true); // stderr → stdout
            procesosServidor = pb.start();

            btnIniciar.setDisable(true);
            btnDetener.setDisable(false);

            // Leer output en hilo de fondo y mostrarlo en la consola
            Thread outReader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(procesosServidor.getInputStream()))) {
                    String linea;
                    while ((linea = br.readLine()) != null) {
                        final String l = linea;
                        Platform.runLater(() -> appendConsola(l));
                    }
                } catch (IOException ignored) {}
                Platform.runLater(() -> {
                    appendConsola("[INFO] Proceso del servidor terminó.");
                    btnIniciar.setDisable(false);
                    btnDetener.setDisable(true);
                    procesosServidor = null;
                });
            }, "server-output");
            outReader.setDaemon(true);
            outReader.start();

        } catch (IOException e) {
            appendConsola("[ERROR] No se pudo iniciar el servidor: " + e.getMessage());
            appendConsola("[ERROR] Verifica que 'java' esté en el PATH y que la ruta al JAR sea correcta.");
        }
    }

    private void detenerServidor() {
        if (procesosServidor != null && procesosServidor.isAlive()) {
            procesosServidor.destroy();
            appendConsola("[INFO] Señal de detención enviada al servidor.");
        }
        btnIniciar.setDisable(false);
        btnDetener.setDisable(true);
        procesosServidor = null;
    }

    private void appendConsola(String linea) {
        consola.appendText(linea + "\n");
        consola.setScrollTop(Double.MAX_VALUE);
    }

    // ── Árbol ─────────────────────────────────────────────────────────────────

    private TreeView<NodoModulo> construirArbol() {
        TreeItem<NodoModulo> raiz = new TreeItem<>(new NodoModulo("ROOT", "root", ""));
        raiz.setExpanded(true);

        for (String tipo : TIPOS_ORDEN) {
            TreeItem<NodoModulo> grupo = new TreeItem<>(new NodoModulo("GRUPO", tipo,
                    NodoModulo.nombreTipo(tipo)));
            grupo.setExpanded(true);
            raiz.getChildren().add(grupo);
            grupos.put(tipo, grupo);
        }

        TreeView<NodoModulo> tree = new TreeView<>(raiz);
        tree.setShowRoot(false);
        tree.setStyle("-fx-background-color: transparent; -fx-text-fill: " + T_BRIGHT + ";" +
                "-fx-font-size: 13px;");

        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(NodoModulo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); setStyle(""); return; }
                if ("GRUPO".equals(item.clase)) renderGrupo(item);
                else if ("INSTANCIA".equals(item.clase)) renderInstancia(item);
                else { setText(null); setGraphic(null); }
            }

            private void renderGrupo(NodoModulo item) {
                int n = getTreeItem() != null ? getTreeItem().getChildren().size() : 0;
                String color = colorTipo(item.tipo);
                Label lbl = new Label(item.etiqueta);
                lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
                Label badge = new Label(String.valueOf(n));
                badge.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + ";" +
                        "-fx-background-color: " + color + "22; -fx-padding: 1 7;" +
                        "-fx-background-radius: 10;");
                HBox h = new HBox(8, lbl, badge);
                h.setAlignment(Pos.CENTER_LEFT);
                setGraphic(h); setText(null);
                setStyle("-fx-padding: 4 8;");
            }

            private void renderInstancia(NodoModulo item) {
                Circle dot = new Circle(4, Color.web(C_CONN));
                ScaleTransition pulse = new ScaleTransition(Duration.millis(1000), dot);
                pulse.setFromX(1.0); pulse.setToX(1.3);
                pulse.setFromY(1.0); pulse.setToY(1.3);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(Animation.INDEFINITE);
                pulse.play();
                Label lbl = new Label(item.etiqueta);
                lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + T_BRIGHT + ";");
                HBox h = new HBox(8, dot, lbl);
                h.setAlignment(Pos.CENTER_LEFT);
                setGraphic(h); setText(null);
                setStyle("-fx-padding: 2 8 2 24;");
            }
        });

        tree.getSelectionModel().selectedItemProperty().addListener((o, prev, sel) -> {
            if (sel != null && "INSTANCIA".equals(sel.getValue().clase)) {
                panelDetalle.getChildren().setAll(construirPanelDetalle(sel.getValue()).getChildren());
            } else if (sel != null && "GRUPO".equals(sel.getValue().clase)) {
                panelDetalle.getChildren().setAll(construirPanelDetalle(null).getChildren());
            }
        });

        return tree;
    }

    private VBox construirPanelDetalle(NodoModulo nodo) {
        VBox panel = new VBox(12);
        panel.setAlignment(Pos.TOP_LEFT);

        if (nodo == null) {
            Label hint = new Label("Selecciona una conexión\npara ver sus detalles");
            hint.setStyle("-fx-font-size: 14px; -fx-text-fill: " + T_DIM + "; -fx-text-alignment: center;");
            hint.setWrapText(true);
            VBox contenedor = new VBox(hint);
            contenedor.setAlignment(Pos.CENTER);
            VBox.setVgrow(contenedor, Priority.ALWAYS);
            panel.getChildren().add(contenedor);
            VBox.setVgrow(panel, Priority.ALWAYS);
            return panel;
        }

        String color = colorTipo(nodo.tipo);

        Label tipo = new Label(NodoModulo.nombreTipo(nodo.tipo));
        tipo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";" +
                "-fx-background-color: " + color + "22; -fx-padding: 4 12; -fx-background-radius: 6;" +
                "-fx-border-color: " + color + "44; -fx-border-radius: 6;");

        Circle dot = new Circle(5, Color.web(C_CONN));
        Label lblConn = new Label("CONECTADO");
        lblConn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + C_CONN + ";");
        HBox estadoRow = new HBox(8, dot, lblConn);
        estadoRow.setAlignment(Pos.CENTER_LEFT);

        VBox detalles = new VBox(8,
                filaDetalle("Nombre del equipo:", nodo.nombrePc, T_BRIGHT),
                filaDetalle("Dirección IP:", nodo.ip, ACC),
                filaDetalle("Puerto TCP:", String.valueOf(nodo.puerto), T_MED),
                filaDetalle("Tipo de módulo:", NodoModulo.nombreTipo(nodo.tipo), color),
                filaDetalle("Conectado desde:", nodo.timestamp, T_DIM)
        );
        detalles.setPadding(new Insets(16));
        detalles.setStyle("-fx-background-color: " + BG_CARD + ";" +
                "-fx-border-color: " + BG_BORDER + "; -fx-border-radius: 8; -fx-background-radius: 8;");

        panel.getChildren().addAll(tipo, estadoRow, detalles);
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private HBox filaDetalle(String etiqueta, String valor, String colorValor) {
        Label lEt = new Label(etiqueta);
        lEt.setStyle("-fx-font-size: 11px; -fx-text-fill: " + T_DIM + "; -fx-min-width: 140px;");
        Label lVal = new Label(valor);
        lVal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + colorValor + ";");
        HBox row = new HBox(8, lEt, lVal);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── Lógica de conexión ────────────────────────────────────────────────────

    private void conectarAsync() {
        Thread t = new Thread(() -> {
            try {
                ConexionMonitor cm = new ConexionMonitor();
                cm.setOnStatus(this::procesarStatus);
                cm.setOnLog(entry -> Platform.runLater(() -> {
                    totalLogs++;
                    lblLogs.setText(totalLogs + " logs recibidos");
                }));
                cm.setOnDesconexion(() -> Platform.runLater(this::marcarDesconectado));
                cm.setOnConexionRestaurada(() -> Platform.runLater(() -> {
                    limpiarArbol(); // el servidor re-enviará STATUS_UPDATE CONECTADO para cada cliente activo
                    marcarConectado();
                }));
                cm.conectar(HOST, PUERTO);
                conexion = cm;
                Platform.runLater(this::marcarConectado);
            } catch (IOException e) {
                Platform.runLater(this::marcarDesconectado);
                // La reconexión está manejada dentro de ConexionMonitor
            }
        }, "monitor-connect");
        t.setDaemon(true);
        t.start();
    }

    private void procesarStatus(ConexionMonitor.StatusMsg msg) {
        Platform.runLater(() -> {
            if ("CONECTADO".equals(msg.accion)) {
                agregarInstancia(msg);
            } else if ("DESCONECTADO".equals(msg.accion)) {
                eliminarInstancia(msg.getId());
            }
            actualizarConteos();
        });
    }

    private void agregarInstancia(ConexionMonitor.StatusMsg msg) {
        if (instancias.containsKey(msg.getId())) return;
        TreeItem<NodoModulo> grupo = grupos.get(msg.tipo);
        if (grupo == null) return;

        NodoModulo nodo = new NodoModulo("INSTANCIA", msg.tipo, msg.etiquetaUI(),
                msg.ip, msg.puerto, msg.nombrePc, msg.timestamp);
        TreeItem<NodoModulo> item = new TreeItem<>(nodo);
        grupo.getChildren().add(item);
        instancias.put(msg.getId(), item);
        grupo.setExpanded(true);
    }

    private void eliminarInstancia(String id) {
        TreeItem<NodoModulo> item = instancias.remove(id);
        if (item == null) return;
        TreeItem<NodoModulo> padre = item.getParent();
        if (padre != null) padre.getChildren().remove(item);
    }

    private void limpiarArbol() {
        instancias.clear();
        for (TreeItem<NodoModulo> grupo : grupos.values()) {
            grupo.getChildren().clear();
        }
        actualizarConteos();
    }

    private void actualizarConteos() {
        int total = instancias.size();
        lblConexiones.setText(total + " conexión" + (total != 1 ? "es" : ""));
        arbol.refresh();
    }

    private void marcarConectado() {
        dotEstado.setFill(Color.web(C_CONN));
        lblEstado.setText("Conectado");
        lblEstado.setStyle("-fx-font-size: 11px; -fx-text-fill: " + C_CONN + ";");
        dotEstado.getParent().setStyle("-fx-background-color: rgba(50,215,75,0.10);" +
                "-fx-border-color: rgba(50,215,75,0.30); -fx-border-radius: 20; -fx-background-radius: 20;");
        pulseDot();
    }

    private void marcarDesconectado() {
        dotEstado.setFill(Color.web("#FF453A"));
        lblEstado.setText("Sin conexión — reconectando...");
        lblEstado.setStyle("-fx-font-size: 11px; -fx-text-fill: #FF453A;");
        dotEstado.getParent().setStyle("-fx-background-color: rgba(255,69,58,0.10);" +
                "-fx-border-color: rgba(255,69,58,0.30); -fx-border-radius: 20; -fx-background-radius: 20;");
    }

    private void pulseDot() {
        ScaleTransition st = new ScaleTransition(Duration.millis(900), dotEstado);
        st.setFromX(1.0); st.setToX(1.4);
        st.setFromY(1.0); st.setToY(1.4);
        st.setAutoReverse(true);
        st.setCycleCount(Animation.INDEFINITE);
        st.play();
    }

    private void animarEntrada(VBox root) {
        root.setOpacity(0);
        root.setTranslateY(-6);
        new ParallelTransition(
                fade(root, 0, 1, 300),
                slide(root, -6, 0, 300)
        ).play();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static String colorTipo(String tipo) {
        return switch (tipo) {
            case "REGISTRO"    -> C_REGISTRO;
            case "GENERAL"     -> C_GENERAL;
            case "PRIORITARIA" -> C_PRIORITARIA;
            case "ESPECIAL"    -> C_ESPECIAL;
            case "LOGS", "MONITOR" -> C_MONITOR;
            default -> T_MED;
        };
    }

    private static FadeTransition fade(javafx.scene.Node n, double from, double to, double ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), n);
        ft.setFromValue(from); ft.setToValue(to);
        return ft;
    }

    private static TranslateTransition slide(javafx.scene.Node n, double from, double to, double ms) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), n);
        tt.setFromY(from); tt.setToY(to);
        return tt;
    }

    public static void main(String[] args) { launch(args); }
}
