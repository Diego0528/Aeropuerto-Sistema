package com.aeropuerto.especial;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Interfaz de Cola Especial — placeholder mínimo para verificar que JavaFX compila.
 *
 * REEMPLAZAR con la UI real cuando corresponda.
 */
public class EspecialApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        Label label    = new Label("Aeropuerto Guatemala — Cola Prioritaria");
        Label sublabel = new Label("UI en construcción");

        VBox root = new VBox(12, label, sublabel);
        root.setAlignment(Pos.CENTER);

        primaryStage.setTitle("Aeropuerto :: Cola Prioritaria");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
            launch(args);
        }
}
