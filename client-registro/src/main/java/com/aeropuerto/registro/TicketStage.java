package com.aeropuerto.registro;

import com.aeropuerto.common.TipoAtencion;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Ventana modal del ticket generado.
 *
 * Diseño: card glassmorphism sobre fondo semitransparente oscuro.
 * Cabecera de color según tipo de cola.
 * Botones: Imprimir / Listo (cierra y limpia el formulario).
 */
public class TicketStage {

    /**
     * Muestra el ticket modal.
     *
     * @param owner        Stage padre
     * @param dpi          DPI del pasajero
     * @param nombre       Nombre completo
     * @param tipo         Tipo de cola asignada
     * @param numeroCola   Número de turno del servidor
     * @param razonTipo    Por qué ese tipo (de RENAP)
     * @param necesidades  Necesidades especiales ingresadas (puede ser vacío)
     * @param onListo      Se ejecuta al presionar "Listo"
     */
    public static void mostrar(Stage owner, String dpi, String nombre,
                               TipoAtencion tipo, int numeroCola,
                               String razonTipo, String necesidades,
                               Runnable onListo) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(10,20,40,0.55);");

        VBox card = construirCard(dialog, dpi, nombre, tipo, numeroCola,
                razonTipo, necesidades, onListo);
        root.getChildren().add(card);
        StackPane.setMargin(card, new Insets(40));

        Scene scene = new Scene(root, 420, necesidades.isBlank() ? 520 : 560);
        scene.setFill(Color.web("rgba(10,20,40,0.55)"));
        dialog.setScene(scene);
        dialog.show();

        // Centrar
        dialog.setX(owner.getX() + (owner.getWidth()  - dialog.getWidth())  / 2);
        dialog.setY(owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2);

        // Fade in
        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private static VBox construirCard(Stage dialog, String dpi, String nombre,
                                      TipoAtencion tipo, int numeroCola,
                                      String razonTipo, String necesidades,
                                      Runnable onListo) {
        String colorTipo  = colorDeTipo(tipo);
        String prefijo    = prefijoDeTipo(tipo);
        String etiqueta   = etiquetaDeTipo(tipo);

        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: rgba(235,240,248,0.90);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.90);" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(10,20,60,0.30), 30, 0, 0, 8);"
        );

        // Cabecera de color
        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(24, 28, 20, 28));
        header.setStyle(
                "-fx-background-color: " + colorTipo + ";" +
                        "-fx-background-radius: 16 16 0 0;"
        );

        Label lblAeropuerto = new Label("AEROPUERTO INTERNACIONAL DE GUATEMALA");
        lblAeropuerto.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 9));
        lblAeropuerto.setTextFill(Color.web("rgba(255,255,255,0.70)"));
        lblAeropuerto.setStyle("-fx-letter-spacing: 1px;");

        Label lblComprobante = new Label("Comprobante de cola");
        lblComprobante.setFont(javafx.scene.text.Font.font("Arial", 10));
        lblComprobante.setTextFill(Color.web("rgba(255,255,255,0.50)"));

        Label lblColaTipo = new Label(etiqueta.toUpperCase() + " · TURNO");
        lblColaTipo.setFont(javafx.scene.text.Font.font("Arial", 10));
        lblColaTipo.setTextFill(Color.web("rgba(255,255,255,0.75)"));

        Label lblTurno = new Label(prefijo + "-" + String.format("%02d", numeroCola));
        lblTurno.setFont(javafx.scene.text.Font.font("Consolas",
                javafx.scene.text.FontWeight.BOLD, 62));
        lblTurno.setTextFill(Color.WHITE);

        header.getChildren().addAll(lblAeropuerto, lblComprobante, lblColaTipo, lblTurno);

        // Badge razón (si aplica)
        if (razonTipo != null && !razonTipo.contains("Sin condición")) {
            Label lblRazon = new Label(razonTipo);
            lblRazon.setFont(javafx.scene.text.Font.font("Arial",
                    javafx.scene.text.FontWeight.BOLD, 10));
            lblRazon.setTextFill(Color.web(colorTipo));
            lblRazon.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.90);" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 4 14 4 14;"
            );
            header.getChildren().add(lblRazon);
        }

        // Cuerpo
        VBox cuerpo = new VBox(0);
        cuerpo.setPadding(new Insets(18, 28, 8, 28));
        cuerpo.setStyle("-fx-background-color: transparent;");

        cuerpo.getChildren().addAll(
                filaDatos("Nombre",        nombre),
                lineaDivision(),
                filaDatos("DPI",           formatearDPI(dpi)),
                lineaDivision(),
                filaDatos("Fecha emisión", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                lineaDivision(),
                filaDatos("Hora emisión",  LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + " hrs")
        );

        if (!necesidades.isBlank()) {
            cuerpo.getChildren().addAll(
                    lineaDivision(),
                    filaDatos("Necesidades", necesidades)
            );
        }

        // Pie
        VBox pie = new VBox(14);
        pie.setAlignment(Pos.CENTER);
        pie.setPadding(new Insets(16, 28, 24, 28));

        // Línea punteada
        HBox punteada = new HBox();
        punteada.setAlignment(Pos.CENTER);
        for (int i = 0; i < 20; i++) {
            Rectangle pt = new Rectangle(4, 1, Color.web("rgba(40,55,80,0.15)"));
            HBox sp = new HBox(pt);
            sp.setPadding(new Insets(0, 3, 0, 3));
            punteada.getChildren().add(sp);
        }

        Label lblEspere = new Label("Por favor espere a ser llamado");
        lblEspere.setFont(javafx.scene.text.Font.font("Arial", 11));
        lblEspere.setTextFill(Color.web("rgba(40,55,80,0.38)"));

        HBox botones = new HBox(12);
        botones.setAlignment(Pos.CENTER);

        Button btnImprimir = new Button("⊙  Imprimir");
        btnImprimir.setFont(javafx.scene.text.Font.font("Arial",
                javafx.scene.text.FontWeight.BOLD, 12));
        btnImprimir.setStyle(
                "-fx-background-color: rgba(255,255,255,0.65);" +
                        "-fx-border-color: rgba(255,255,255,0.90);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 9;" +
                        "-fx-background-radius: 9;" +
                        "-fx-text-fill: rgba(40,55,80,0.70);" +
                        "-fx-padding: 9 20 9 20;" +
                        "-fx-cursor: hand;"
        );
        btnImprimir.setOnMouseEntered(e -> btnImprimir.setOpacity(0.85));
        btnImprimir.setOnMouseExited(e  -> btnImprimir.setOpacity(1.0));
        btnImprimir.setOnAction(e -> imprimir(card, dialog));

        Button btnListo = new Button("✓  Listo");
        btnListo.setFont(javafx.scene.text.Font.font("Arial",
                javafx.scene.text.FontWeight.BOLD, 12));
        btnListo.setStyle(
                "-fx-background-color: " + colorTipo + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-border-radius: 9;" +
                        "-fx-background-radius: 9;" +
                        "-fx-padding: 9 28 9 28;" +
                        "-fx-cursor: hand;"
        );
        btnListo.setOnMouseEntered(e -> btnListo.setOpacity(0.88));
        btnListo.setOnMouseExited(e  -> btnListo.setOpacity(1.0));
        btnListo.setOnAction(e -> {
            dialog.close();
            if (onListo != null) onListo.run();
        });

        botones.getChildren().addAll(btnImprimir, btnListo);
        pie.getChildren().addAll(punteada, lblEspere, botones);

        card.getChildren().addAll(header, cuerpo, pie);
        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static HBox filaDatos(String etiqueta, String valor) {
        Label lEtiqueta = new Label(etiqueta);
        lEtiqueta.setFont(javafx.scene.text.Font.font("Arial", 11));
        lEtiqueta.setTextFill(Color.web("rgba(40,55,80,0.45)"));
        lEtiqueta.setPrefWidth(120);

        Label lValor = new Label(valor);
        lValor.setFont(javafx.scene.text.Font.font("Arial",
                javafx.scene.text.FontWeight.BOLD, 12));
        lValor.setTextFill(Color.web("rgba(15,28,55,0.82)"));
        lValor.setWrapText(true);
        HBox.setHgrow(lValor, Priority.ALWAYS);

        HBox fila = new HBox(lEtiqueta, lValor);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(10, 0, 10, 0));
        return fila;
    }

    private static Rectangle lineaDivision() {
        Rectangle r = new Rectangle(0, 0.5, Color.web("rgba(40,55,80,0.08)"));
        r.setWidth(340);
        return r;
    }

    private static String colorDeTipo(TipoAtencion tipo) {
        return switch (tipo) {
            case GENERAL     -> "rgba(70,130,90,0.85)";
            case PRIORITARIA -> "rgba(185,100,65,0.85)";
            case ESPECIAL    -> "rgba(60,95,165,0.85)";
        };
    }

    private static String prefijoDeTipo(TipoAtencion tipo) {
        return switch (tipo) {
            case GENERAL     -> "G";
            case PRIORITARIA -> "P";
            case ESPECIAL    -> "E";
        };
    }

    private static String etiquetaDeTipo(TipoAtencion tipo) {
        return switch (tipo) {
            case GENERAL     -> "Cola General";
            case PRIORITARIA -> "Cola Prioritaria";
            case ESPECIAL    -> "Cola Especial";
        };
    }

    private static String formatearDPI(String dpi) {
        if (dpi == null || dpi.length() != 13) return dpi;
        return dpi.substring(0, 4) + " " + dpi.substring(4, 8) + " " + dpi.substring(8);
    }

    private static void imprimir(VBox card, Stage dialog) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return;
        if (job.showPrintDialog(dialog)) {
            if (job.printPage(card)) job.endJob();
        }
    }
}