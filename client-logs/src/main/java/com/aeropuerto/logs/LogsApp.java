package com.aeropuerto.logs;

import com.aeropuerto.common.ConfigServidor;
import com.aeropuerto.common.LogEntry;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Visor de logs en tiempo real y visualizador de registros de pasajeros (CSV).
 *
 * Pestaña 1 — Logs en Tiempo Real: recibe LOG_ENTRY del servidor vía push.
 * Pestaña 2 — Registros de Pasajeros: lee registros_aeropuerto.csv del servidor.
 */
public class LogsApp extends Application {

    private static final String HOST   = ConfigServidor.getInstance().getHost();
    private static final int    PUERTO = ConfigServidor.getInstance().getPuerto();

    // ── Paleta dark navy ──────────────────────────────────────────────────────
    static final String BG_DARK  = "#0f1629";
    static final String BG_BORDE = "rgba(255,255,255,0.10)";
    static final String T_FULL   = "rgba(255,255,255,0.95)";
    static final String T_MED    = "rgba(255,255,255,0.65)";
    static final String T_DIM    = "rgba(255,255,255,0.40)";
    static final String ACC      = "#BF5AF2";

    static final String C_INFO   = "#64AAFF";
    static final String C_ACTION = "#32D74B";
    static final String C_WARN   = "#FF9F0A";
    static final String C_ERROR  = "#FF453A";

    // ── Estado ────────────────────────────────────────────────────────────────
    private ConexionLogs conexion;
    private ChatPanel    chatPanel;
    private int          totalLogs = 0;
    private Label        lblTotal;
    private Label        lblEstado;
    private Circle       dotEstado;

    private final ObservableList<LogRow> todos = FXCollections.observableArrayList();
    private FilteredList<LogRow>         filtrados;

    private TextField        buscar;
    private ComboBox<String> cboModulo;
    private ComboBox<String> cboNivel;

    // Pestaña Registros de Pasajeros
    private Label                             lblRutaCSV;
    private final ObservableList<RegistroRow> registrosCSV = FXCollections.observableArrayList();

    // ── Inicio ────────────────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        ConfigServidor.crearPlantillaSiNoExiste();
        stage.setTitle("AeroQueue — Visor de Logs & Registros");
        stage.setMinWidth(820);
        stage.setMinHeight(560);

        chatPanel = new ChatPanel();

        VBox root = construirUI(stage);
        Scene scene = new Scene(root);
        scene.setFill(Color.web(BG_DARK));
        scene.getStylesheets().add(getClass().getResource("/logs.css").toExternalForm());
        stage.setScene(scene);
        stage.setWidth(1200);
        stage.setHeight(700);
        stage.setOnCloseRequest(e -> { if (conexion != null) conexion.desconectar(); });
        stage.show();

        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        conectarAsync();
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private VBox construirUI(Stage stage) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a2744, " + BG_DARK + ");");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: transparent; -fx-tab-min-width: 140;");
        tabs.getStyleClass().add("floating");
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Tab tabLive = new Tab("Logs en Tiempo Real",    construirTabLive());
        Tab tabDB   = new Tab("Registros de Pasajeros", construirTabDB(stage));
        tabs.getTabs().addAll(tabLive, tabDB);

        root.getChildren().addAll(construirHeader(), tabs);
        return root;
    }

    private HBox construirHeader() {
        Label titulo = new Label("Visor de Logs & Registros");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + T_FULL + ";");

        Label sub = new Label("Aeropuerto Guatemala — Tiempo Real");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: " + T_DIM + ";");

        VBox textos = new VBox(2, titulo, sub);
        HBox.setHgrow(textos, Priority.ALWAYS);

        lblTotal = new Label("0 eventos");
        lblTotal.setStyle(
            "-fx-font-size: 11px; -fx-text-fill: " + T_DIM + ";" +
            "-fx-background-color: rgba(255,255,255,0.06); -fx-padding: 4 12; -fx-background-radius: 12;"
        );

        dotEstado = new Circle(5, Color.web("#FF453A"));
        lblEstado = new Label("Desconectado");
        lblEstado.setStyle("-fx-font-size: 11px; -fx-text-fill: #FF453A;");

        HBox badgeConn = new HBox(6, dotEstado, lblEstado);
        badgeConn.setAlignment(Pos.CENTER_LEFT);
        badgeConn.setPadding(new Insets(5, 12, 5, 12));
        badgeConn.setStyle(
            "-fx-background-color: rgba(255,69,58,0.10);" +
            "-fx-border-color: rgba(255,69,58,0.30);" +
            "-fx-border-radius: 20; -fx-background-radius: 20;"
        );

        Button btnLimpiar = new Button("Limpiar logs");
        btnLimpiar.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: " + T_MED + "; -fx-border-color: " + BG_BORDE + ";" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 5 12; -fx-cursor: hand;"
        );
        btnLimpiar.setOnAction(e -> { todos.clear(); totalLogs = 0; lblTotal.setText("0 eventos"); });

        Button btnChat = chatPanel.crearBotonChat();

        HBox bar = new HBox(10, textos, lblTotal, btnChat, btnLimpiar, badgeConn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setStyle(
            "-fx-background-color: rgba(255,255,255,0.03);" +
            "-fx-border-color: transparent transparent " + BG_BORDE + " transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );
        return bar;
    }

    // ── Pestaña Logs en Tiempo Real ───────────────────────────────────────────

    private VBox construirTabLive() {
        buscar = new TextField();
        buscar.setPromptText("Buscar en mensajes...");
        buscar.setStyle(estiloInput());
        buscar.setPrefWidth(200);

        cboModulo = new ComboBox<>();
        cboModulo.getItems().addAll("Todos", "SERVER", "HANDLER", "GESTOR", "CONEXION");
        cboModulo.setValue("Todos");
        cboModulo.setStyle(estiloInput());

        cboNivel = new ComboBox<>();
        cboNivel.getItems().addAll("Todos", "INFO", "ACTION", "WARN", "ERROR");
        cboNivel.setValue("Todos");
        cboNivel.setStyle(estiloInput());

        Button btnReset = new Button("Restablecer");
        btnReset.setStyle(estiloBtnSecundario());
        btnReset.setOnAction(e -> { buscar.clear(); cboModulo.setValue("Todos"); cboNivel.setValue("Todos"); });

        CheckBox cbAutoScroll = new CheckBox("Auto-scroll");
        cbAutoScroll.setSelected(true);
        cbAutoScroll.setStyle("-fx-text-fill: " + T_MED + ";");

        HBox.setHgrow(buscar, Priority.SOMETIMES);
        HBox filtros = new HBox(10, buscar, etiqueta("Modulo:"), cboModulo, etiqueta("Nivel:"), cboNivel, btnReset);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.setPadding(new Insets(10, 20, 10, 20));
        filtros.setStyle(
            "-fx-background-color: rgba(0,0,0,0.12);" +
            "-fx-border-color: transparent transparent " + BG_BORDE + " transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );

        filtrados = new FilteredList<>(todos);
        Runnable aplicarFiltro = () -> {
            String txt    = buscar.getText().toLowerCase();
            String modulo = cboModulo.getValue();
            String nivel  = cboNivel.getValue();
            filtrados.setPredicate(r -> {
                boolean pTxt = txt.isBlank() || r.getMensaje().toLowerCase().contains(txt);
                boolean pMod = "Todos".equals(modulo) || modulo.equals(r.getModulo());
                boolean pNiv = "Todos".equals(nivel)  || nivel.equals(r.getNivel());
                return pTxt && pMod && pNiv;
            });
        };
        buscar.textProperty().addListener((o, a, b) -> aplicarFiltro.run());
        cboModulo.valueProperty().addListener((o, a, b) -> aplicarFiltro.run());
        cboNivel.valueProperty().addListener((o, a, b) -> aplicarFiltro.run());

        TableView<LogRow> tabla = construirTablaLogs(filtrados);

        todos.addListener((javafx.collections.ListChangeListener<LogRow>) c -> {
            if (cbAutoScroll.isSelected() && tabla.getItems() != null)
                Platform.runLater(() -> {
                    if (!tabla.getItems().isEmpty())
                        tabla.scrollTo(tabla.getItems().size() - 1);
                });
        });

        Label lblVisibles = new Label();
        filtrados.addListener((javafx.collections.ListChangeListener<LogRow>) c ->
            lblVisibles.setText("Total: " + todos.size() + " | Visibles: " + filtrados.size()));
        lblVisibles.setStyle("-fx-font-size: 10px; -fx-text-fill: " + T_DIM + ";");

        HBox footer = new HBox(lblVisibles);
        footer.setPadding(new Insets(4, 20, 4, 20));
        footer.setStyle(
            "-fx-background-color: rgba(0,0,0,0.15);" +
            "-fx-border-color: " + BG_BORDE + " transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;"
        );

        VBox tab = new VBox(0, filtros, tabla, footer);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tab.setStyle("-fx-background-color: " + BG_DARK + ";");
        return tab;
    }

    private TableView<LogRow> construirTablaLogs(ObservableList<LogRow> fuente) {
        TableView<LogRow> tabla = new TableView<>(fuente);
        tabla.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-table-cell-border-color: " + BG_BORDE + ";"
        );
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Label ph = new Label("Sin logs todavía");
        ph.setStyle("-fx-text-fill: " + T_DIM + ";");
        tabla.setPlaceholder(ph);

        TableColumn<LogRow, String> colHora   = colL("Hora",    "hora",    90);
        TableColumn<LogRow, String> colNivel  = colL("Nivel",   "nivel",   70);
        colNivel.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                String c = switch (s) {
                    case "INFO"   -> C_INFO;
                    case "ACTION" -> C_ACTION;
                    case "WARN"   -> C_WARN;
                    case "ERROR"  -> C_ERROR;
                    default       -> T_MED;
                };
                setStyle("-fx-text-fill: " + c + "; -fx-font-weight: bold; -fx-font-size: 11px;");
            }
        });
        TableColumn<LogRow, String> colModulo  = colL("Modulo",  "modulo",  100);
        TableColumn<LogRow, String> colMensaje = colL("Mensaje", "mensaje", 600);

        tabla.getColumns().addAll(colHora, colNivel, colModulo, colMensaje);
        return tabla;
    }

    // ── Pestaña Registros de Pasajeros ────────────────────────────────────────

    private VBox construirTabDB(Stage stage) {
        lblRutaCSV = new Label("Sin archivo cargado  —  el archivo se llama 'registros_aeropuerto.csv' y está en la carpeta del servidor");
        lblRutaCSV.setStyle("-fx-text-fill: " + T_DIM + "; -fx-font-size: 11px;");
        HBox.setHgrow(lblRutaCSV, Priority.ALWAYS);

        Button btnAuto = new Button("Auto-detectar BD");
        btnAuto.setStyle(estiloBtnAcc());
        btnAuto.setOnAction(e -> autoDetectarCSV());

        Button btnExaminar = new Button("Examinar...");
        btnExaminar.setStyle(estiloBtnSecundario());
        btnExaminar.setOnAction(e -> abrirCSV(stage));

        Button btnExportar = new Button("Exportar logs → JSON");
        btnExportar.setStyle(estiloBtnSecundario());
        btnExportar.setOnAction(e -> exportarLogs(stage));

        HBox bar = new HBox(10, btnAuto, btnExaminar, lblRutaCSV, btnExportar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 20, 12, 20));
        bar.setStyle(
            "-fx-background-color: rgba(0,0,0,0.12);" +
            "-fx-border-color: transparent transparent " + BG_BORDE + " transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );

        TableView<RegistroRow> tabla = construirTablaRegistros();

        Label lblConteo = new Label("0 registros");
        registrosCSV.addListener((javafx.collections.ListChangeListener<RegistroRow>) c ->
            lblConteo.setText(registrosCSV.size() + " registros cargados"));
        lblConteo.setStyle("-fx-font-size: 10px; -fx-text-fill: " + T_DIM + ";");

        HBox footer = new HBox(lblConteo);
        footer.setPadding(new Insets(4, 20, 4, 20));
        footer.setStyle(
            "-fx-background-color: rgba(0,0,0,0.15);" +
            "-fx-border-color: " + BG_BORDE + " transparent transparent transparent;" +
            "-fx-border-width: 1 0 0 0;"
        );

        VBox tab = new VBox(0, bar, tabla, footer);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tab.setStyle("-fx-background-color: " + BG_DARK + ";");
        return tab;
    }

    private TableView<RegistroRow> construirTablaRegistros() {
        TableView<RegistroRow> tabla = new TableView<>(registrosCSV);
        tabla.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-table-cell-border-color: " + BG_BORDE + ";"
        );
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Label ph = new Label("Usa 'Auto-detectar BD' o 'Examinar...' para cargar registros_aeropuerto.csv del servidor");
        ph.setStyle("-fx-text-fill: " + T_DIM + ";");
        tabla.setPlaceholder(ph);

        TableColumn<RegistroRow, String> colDpi      = colR("DPI",          "dpi",          110);
        TableColumn<RegistroRow, String> colNombre   = colR("Nombre",       "nombre",       160);
        TableColumn<RegistroRow, String> colTipo     = colR("Cola",         "tipo",          90);
        TableColumn<RegistroRow, String> colTurno    = colR("Turno",        "turno",         55);
        TableColumn<RegistroRow, String> colRegistro = colR("Registro",     "horaRegistro", 140);
        TableColumn<RegistroRow, String> colFin      = colR("Fin Atención", "horaFin",      140);
        TableColumn<RegistroRow, String> colDuracion = colR("Duración (s)", "duracion",      90);

        colTipo.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                String c = switch (s) {
                    case "GENERAL"     -> C_INFO;
                    case "PRIORITARIA" -> C_WARN;
                    case "ESPECIAL"    -> ACC;
                    default            -> T_MED;
                };
                setStyle("-fx-text-fill: " + c + "; -fx-font-weight: bold; -fx-font-size: 11px;");
            }
        });

        tabla.getColumns().addAll(colDpi, colNombre, colTipo, colTurno, colRegistro, colFin, colDuracion);
        return tabla;
    }

    private void autoDetectarCSV() {
        String[] candidatos = {
            "registros_aeropuerto.csv",
            "../AeroQueue-Servidor/registros_aeropuerto.csv",
            "../../AeroQueue-Servidor/registros_aeropuerto.csv",
            System.getProperty("user.dir") + "/registros_aeropuerto.csv",
            System.getProperty("user.dir") + "/../AeroQueue-Servidor/registros_aeropuerto.csv"
        };
        for (String ruta : candidatos) {
            File f = new File(ruta);
            if (f.exists()) {
                cargarCSV(f);
                return;
            }
        }
        mostrarAlerta("Archivo no encontrado",
            "No se encontró 'registros_aeropuerto.csv'.\n\n" +
            "El archivo lo crea el servidor en su propia carpeta:\n" +
            "  dist/AeroQueue-Servidor/registros_aeropuerto.csv\n\n" +
            "Usa el botón 'Examinar...' para localizarlo manualmente.");
    }

    private void abrirCSV(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Abrir registros de pasajeros");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Registros CSV (*.csv)", "*.csv"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        File f = fc.showOpenDialog(stage);
        if (f != null) cargarCSV(f);
    }

    private void cargarCSV(File f) {
        registrosCSV.clear();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            String linea;
            boolean primera = true;
            while ((linea = br.readLine()) != null) {
                if (primera) { primera = false; continue; } // saltar cabecera
                if (linea.isBlank()) continue;
                String[] p = linea.split(";", -1);
                if (p.length >= 10) {
                    // Índices CSV: dpi=0, nombre=1, tipo=2, numero_cola=3,
                    //              hora_registro=4, hora_llamada=5, hora_fin=6,
                    //              vuelo=7, observaciones=8, duracion_seg=9
                    registrosCSV.add(new RegistroRow(p[0], p[1], p[2], p[3], p[4], p[6], p[9]));
                }
            }
            lblRutaCSV.setText(f.getAbsolutePath());
            lblRutaCSV.setStyle("-fx-text-fill: #32D74B; -fx-font-size: 11px;");
        } catch (IOException e) {
            mostrarAlerta("Error al leer CSV", e.getMessage());
        }
    }

    private void exportarLogs(Stage stage) {
        if (todos.isEmpty()) {
            mostrarAlerta("Sin logs", "No hay logs en esta sesión para exportar.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar logs de sesión");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Logs JSON (*.json)", "*.json"));
        fc.setInitialFileName("logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".json");
        File f = fc.showSaveDialog(stage);
        if (f == null) return;

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"))) {
            w.println("[");
            for (int i = 0; i < todos.size(); i++) {
                LogRow r = todos.get(i);
                w.printf("  {\"id\":%d,\"hora\":\"%s\",\"nivel\":\"%s\",\"modulo\":\"%s\",\"mensaje\":\"%s\"}%s%n",
                    r.getId(), r.getHora(), r.getNivel(), r.getModulo(),
                    r.getMensaje().replace("\"", "'"),
                    i < todos.size() - 1 ? "," : "");
            }
            w.println("]");
            mostrarAlerta("Exportación completada", "Logs guardados en:\n" + f.getAbsolutePath());
        } catch (IOException e) {
            mostrarAlerta("Error al exportar", e.getMessage());
        }
    }

    private void mostrarAlerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }

    // ── Conexión ──────────────────────────────────────────────────────────────

    private void conectarAsync() {
        conexion = new ConexionLogs();
        conexion.setOnLog(entry -> Platform.runLater(() -> agregarLog(entry)));
        conexion.setOnDesconexion(() -> Platform.runLater(this::marcarDesconectado));
        conexion.setOnConexionRestaurada(() -> Platform.runLater(this::marcarConectado));

        new Thread(() -> {
            try {
                conexion.conectar(HOST, PUERTO);
                Platform.runLater(this::marcarConectado);
            } catch (Exception e) {
                Platform.runLater(this::marcarDesconectado);
            }
        }, "logs-connect").start();
    }

    private void agregarLog(LogEntry entry) {
        totalLogs++;
        lblTotal.setText(totalLogs + " eventos");
        todos.add(new LogRow(entry.getId(), entry.getTimestamp(),
            entry.getNivel().name(), entry.getModulo(), entry.getMensaje()));
    }

    private void marcarConectado() {
        dotEstado.setFill(Color.web("#32D74B"));
        lblEstado.setText("Conectado");
        lblEstado.setStyle("-fx-font-size: 11px; -fx-text-fill: #32D74B;");
        dotEstado.getParent().setStyle(
            "-fx-background-color: rgba(50,215,75,0.10);" +
            "-fx-border-color: rgba(50,215,75,0.30);" +
            "-fx-border-radius: 20; -fx-background-radius: 20;"
        );
    }

    private void marcarDesconectado() {
        dotEstado.setFill(Color.web("#FF453A"));
        lblEstado.setText("Sin conexion — reconectando...");
        lblEstado.setStyle("-fx-font-size: 11px; -fx-text-fill: #FF453A;");
        dotEstado.getParent().setStyle(
            "-fx-background-color: rgba(255,69,58,0.10);" +
            "-fx-border-color: rgba(255,69,58,0.30);" +
            "-fx-border-radius: 20; -fx-background-radius: 20;"
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> TableColumn<LogRow, T> colL(String titulo, String propiedad, double ancho) {
        TableColumn<LogRow, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(propiedad));
        c.setPrefWidth(ancho);
        c.setStyle("-fx-text-fill: " + T_MED + ";");
        return c;
    }

    private <T> TableColumn<RegistroRow, T> colR(String titulo, String propiedad, double ancho) {
        TableColumn<RegistroRow, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(propiedad));
        c.setPrefWidth(ancho);
        c.setStyle("-fx-text-fill: " + T_MED + ";");
        return c;
    }

    private Label etiqueta(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + T_DIM + ";");
        return lbl;
    }

    private String estiloInput() {
        return
            "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: " + T_MED + ";" +
            "-fx-prompt-text-fill: " + T_DIM + "; -fx-border-color: " + BG_BORDE + ";" +
            "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 5 10; -fx-font-size: 12px;";
    }

    private String estiloBtnSecundario() {
        return
            "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: " + T_MED + ";" +
            "-fx-border-color: " + BG_BORDE + "; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 6 14; -fx-cursor: hand;";
    }

    private String estiloBtnAcc() {
        return
            "-fx-background-color: rgba(191,90,242,0.20); -fx-text-fill: " + ACC + ";" +
            "-fx-border-color: " + ACC + "44; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 6 14; -fx-cursor: hand; -fx-font-weight: bold;";
    }

    // ── Row models ────────────────────────────────────────────────────────────

    public static class LogRow {
        private final long   id;
        private final String hora;
        private final String nivel;
        private final String modulo;
        private final String mensaje;

        public LogRow(long id, String hora, String nivel, String modulo, String mensaje) {
            this.id = id; this.hora = hora; this.nivel = nivel;
            this.modulo = modulo; this.mensaje = mensaje;
        }

        public long   getId()      { return id; }
        public String getHora()    { return hora; }
        public String getNivel()   { return nivel; }
        public String getModulo()  { return modulo; }
        public String getMensaje() { return mensaje; }
    }

    public static class RegistroRow {
        private final String dpi;
        private final String nombre;
        private final String tipo;
        private final String turno;
        private final String horaRegistro;
        private final String horaFin;
        private final String duracion;

        public RegistroRow(String dpi, String nombre, String tipo, String turno,
                           String horaRegistro, String horaFin, String duracion) {
            this.dpi = dpi; this.nombre = nombre; this.tipo = tipo;
            this.turno = turno; this.horaRegistro = horaRegistro;
            this.horaFin = horaFin; this.duracion = duracion;
        }

        public String getDpi()          { return dpi; }
        public String getNombre()       { return nombre; }
        public String getTipo()         { return tipo; }
        public String getTurno()        { return turno; }
        public String getHoraRegistro() { return horaRegistro; }
        public String getHoraFin()      { return horaFin; }
        public String getDuracion()     { return duracion; }
    }

    public static void main(String[] args) { launch(args); }
}