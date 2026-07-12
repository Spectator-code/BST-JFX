import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DashboardView {

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");

        // Top Nav
        HBox nav = new HBox();
        nav.setStyle("-fx-background-color: #16213e; -fx-padding: 15;");
        nav.setAlignment(Pos.CENTER_LEFT);
        
        Label brand = new Label("Send Node");
        brand.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        brand.setTextFill(Color.web("#e94560"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label userLabel = new Label("Welcome, " + App.db.getCurrentUser());
        userLabel.setTextFill(Color.WHITE);
        userLabel.setStyle("-fx-padding: 0 15 0 0;");
        
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            App.db.logout();
            App.setScene(new LoginView().getView());
        });
        
        nav.getChildren().addAll(brand, spacer, userLabel, logoutBtn);
        root.setTop(nav);

        // Center Content
        VBox center = new VBox(30);
        center.setAlignment(Pos.CENTER);

        int solved = App.db.getSolvedProblems().size();
        Label stats = new Label("Problems Solved: " + solved + " / 14");
        stats.setFont(Font.font("Arial", 24));
        stats.setTextFill(Color.web("#e94560"));

        HBox modes = new HBox(40);
        modes.setAlignment(Pos.CENTER);

        VBox expCard = createModeCard("Explorer Mode", "Interactive BST Visualizer with Add/Delete/Search.", "#0f3460");
        expCard.setOnMouseClicked(e -> {
            App.setScene(new ExplorerView().getView());
        });

        VBox chaCard = createModeCard("Challenge Mode", "Solve 14 BST problems in the built-in IDE.", "#e94560");
        chaCard.setOnMouseClicked(e -> {
            App.setScene(new ChallengeView().getView());
        });

        modes.getChildren().addAll(expCard, chaCard);
        center.getChildren().addAll(stats, modes);

        root.setCenter(center);
        return root;
    }

    private VBox createModeCard(String titleStr, String descStr, String color) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setPrefSize(300, 200);
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10; -fx-cursor: hand;");

        Label title = new Label(titleStr);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        title.setTextFill(Color.WHITE);

        Label desc = new Label(descStr);
        desc.setWrapText(true);
        desc.setTextFill(Color.LIGHTGRAY);
        desc.setAlignment(Pos.CENTER);

        card.getChildren().addAll(title, desc);
        return card;
    }
}
