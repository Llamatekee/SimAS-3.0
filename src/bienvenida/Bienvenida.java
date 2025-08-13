package bienvenida;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class Bienvenida extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/vistas/Bienvenida.fxml"));
        Scene scene = new Scene(loader.load());

        // Configurar la ventana de bienvenida
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setWidth(700);
        primaryStage.setHeight(600);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        
        // Centrar la ventana en la pantalla
        primaryStage.centerOnScreen();
        
        primaryStage.show();

        // Espera 2.5 segundos y lanza MenuPrincipal
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2.5), event -> {
            primaryStage.close();
            abrirMenuPrincipal();
        }));
        timeline.setCycleCount(1);
        timeline.play();
    }

    private void abrirMenuPrincipal() {
        Platform.runLater(() -> {
            try {
                new MenuPrincipal().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
