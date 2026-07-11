import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {
    private static Stage primaryStage;
    public static DatabaseManager db = new DatabaseManager();

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("Send Node - BST Learning Platform");
        
        // Start at Login Screen
        changeScene(new LoginView().getView());
        
        stage.setMinWidth(900);
        stage.setMinHeight(580);
        stage.setWidth(1000);
        stage.setHeight(700);
        stage.show();
    }

    // Switch to a new scene
    public static void changeScene(javafx.scene.Parent root) {
        primaryStage.setScene(new Scene(root));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
