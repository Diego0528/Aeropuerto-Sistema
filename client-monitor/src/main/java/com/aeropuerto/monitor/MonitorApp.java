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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitor de módulos — Task Manager estilo árbol.
 *
 * Muestra en tiempo real qué módulos están conectados al servidor,
 * cuántas instancias hay de cada uno, y detalles de cada conexión.
 *
 * Si dos PCs abren la Cola Especial, aparecen como dos subprocesos
 * bajo el nodo padre "Cola Especial".
 */
public class MonitorApp extends Application {

    private static final String HOST   = "localhost";
    private static final int    PUERTO = 5000;

    // ── Paleta dark navy glassmorphism ────────────────────────────────────────
    static final String BG_DARK   = "#0f1629";
    static final String BG_CARD   = "rgba(255,255,255,0.04)";
    static final String BG_BORDER = "rgba(255,255,255,0.10)";
    static final String T_BRIGHT  = "rgba(255,255,255,0.95)";
    static final String T_MED     = "rgba(255,255,255,0.65)";
    static final String T_DIM     = "rgba(255,255,255,0.40)";
    static final String ACC       = "#FF9F0A"; // ámbar — identidad monitor

    // Colores por tipo de módulo
    static final String C_REGISTRO   = "#FF9F0A";
    static final String C_GENERAL    = "#32D74B";
    static final String C_PRIORITARIA = "#FF9F0A";
    static final String C_ESPECIAL   = "#64AAFF";
    static final String C_MONITOR    = "#BF5AF2";
    static final String C_CONN       = "#32D74B";

    // ── Estado del árbol ──────────────────────────────────────────────────────

    // Nodos raíz por tipo (siempre presentes)
    private final Map<String, TreeItem<NodoModulo>> grupos = new HashMap<>();
    // Nodos hoja por id de conexión (ip:puerto)
    private final Map<String, TreeItem<NodoModulo>> instancias = new HashMap<>();

    private TreeView<NodoModulo> arbol;
    private VBox                 panelDetalle;
    private Label                lblConexiones;
    private Label                lblEstado;
    private Circle               dotEstado;
    private int                  totalLogs = 0;
    private Label                lblLogs;

    private ConexionMonitor conexion;

    // ── Tipos de módulo en orden de presentación ──────────────────────────────
    private static final String[] TIPOS_ORDEN = {
            "REGISTRO", "GENERAL", "PRIORITARIA", "ESPECIAL", "LOGS", "MONITOR"
    };

    // ── Aplicación ────────────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        stage.setTitle("AeroQueue — Monitor de Módulos");
        stage.setMinWidth(760);
        stage.setMinHeight(500);

        VBox root = construirUI();
        Scene scene = new Scene(root);
        scene.setFill(Color.web(BG_DARK));
        stage.setScene(scene);
        stage.setWidth(1000);
        stage.setHeight(660);
        stage.setOnCloseRequest(e -> { if (conexion != null) conexion.desconectar(); });
        stage.show();

        animarEntrada(root);
        conectarAsync();
    }

    // ── Construcción de UI ────────────────────────────────────────────────────

    private VBox construirUI() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a2744, " + BG_DARK + ");");
        HBox cuerpo = construirCuerpo(); // Solo una llamada — evita reinicializar campos
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        root.getChildren().addAll(construirTopBar(), cuerpo);
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
        // Panel izquierdo: árbol de módulos
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

        // Panel derecho: detalle de la conexión seleccionada
        panelDetalle = construirPanelDetalle(null);

        VBox panelDer = new VBox(panelDetalle);
        panelDer.setPadding(new Insets(16));
        HBox.setHgrow(panelDer, Priority.ALWAYS);
        VBox.setVgrow(panelDetalle, Priority.ALWAYS);

        HBox cuerpo = new HBox(panelIzq, panelDer);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        return cuerpo;
    }

    private TreeView<NodoModulo> construirArbol() {
        TreeItem<NodoModulo> raiz = new TreeItem<>(new NodoModulo("ROOT", "root", ""));
        raiz.setExpanded(true);

        // Crear nodos grupo para cada tipo, en orden
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

                if ("GRUPO".equals(item.clase)) {
                    renderGrupo(item);
                } else if ("INSTANCIA".equals(item.clase)) {
                    renderInstancia(item);
                } else {
                    setText(null); setGraphic(null);
                }
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
                setGraphic(h);
                setText(null);
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
                setGraphic(h);
                setText(null);
                setStyle("-fx-padding: 2 8 2 24;");
            }
        });

        // Al seleccionar una instancia → mostrar detalle
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

        // Badge de tipo
        Label tipo = new Label(NodoModulo.nombreTipo(nodo.tipo));
        tipo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";" +
                "-fx-background-color: " + color + "22; -fx-padding: 4 12; -fx-background-radius: 6;" +
                "-fx-border-color: " + color + "44; -fx-border-radius: 6;");

        // Indicador "Conectado"
        Circle dot = new Circle(5, Color.web(C_CONN));
        Label lblConn = new Label("CONECTADO");
        lblConn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + C_CONN + ";");
        HBox estadoRow = new HBox(8, dot, lblConn);
        estadoRow.setAlignment(Pos.CENTER_LEFT);

        // Campos de detalle
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
                cm.conectar(HOST, PUERTO);
                conexion = cm;
                Platform.runLater(this::marcarConectado);
            } catch (IOException e) {
                Platform.runLater(this::marcarDesconectado);
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
        if (instancias.containsKey(msg.getId())) return; // ya existe

        TreeItem<NodoModulo> grupo = grupos.get(msg.tipo);
        if (grupo == null) return; // tipo desconocido

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

    private void actualizarConteos() {
        int total = instancias.size();
        lblConexiones.setText(total + " conexión" + (total != 1 ? "es" : ""));
        // Forzar redibujado de celdas de grupo (para actualizar badges)
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
        lblEstado.setText("Desconectado");
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
