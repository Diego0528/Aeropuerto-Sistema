package com.aeropuerto.registro;

import com.aeropuerto.common.CommonPlaceholder;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Interfaz de Registro de Pasajeros — placeholder mínimo para verificar que JavaFX compila.
 *
 * REEMPLAZAR con la UI real cuando corresponda.
 */
public class RegistroApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label(CommonPlaceholder.version());
        Label sublabel = new Label("[Registro de Pasajeros] — UI en construcción");

        VBox root = new VBox(12, label, sublabel);
        root.setAlignment(Pos.CENTER);

        primaryStage.setTitle("Aeropuerto :: Registro de Pasajeros");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
