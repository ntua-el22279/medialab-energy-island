package gr.ntua.medialab.energyisland;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Entry point of the MediaLab Energy Island application.
 */
public class App extends Application {

    /** Title of the main window, as required by the brief. */
    public static final String TITLE = "MediaLab Energy Island";

    /**
     * Builds and shows the main window.
     *
     * @param stage the primary stage provided by the JavaFX runtime
     */
    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane(new Label(TITLE));
        stage.setTitle(TITLE);
        stage.setScene(new Scene(root, 1280, 800));
        stage.show();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
