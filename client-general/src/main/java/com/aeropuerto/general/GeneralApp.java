package com.aeropuerto.general;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Interfaz de Cola General — placeholder mínimo para verificar que JavaFX compila.
 *
 * REEMPLAZAR con la UI real cuando corresponda.
 */
public class GeneralApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label sublabel = new Label("[Cola General] — UI en construcción");

        VBox root = new VBox(12, sublabel);
        root.setAlignment(Pos.CENTER);

        primaryStage.setTitle("Aeropuerto :: Cola General");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
