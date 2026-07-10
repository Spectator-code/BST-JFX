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
        setScene(new LoginView().getView());
        
        stage.setWidth(1000);
        stage.setHeight(700);
        stage.show();
    }

    public static void setScene(javafx.scene.Parent root) {
        Scene scene = new Scene(root);
        // We can add a global stylesheet later if needed
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
