import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginView {

    public Parent getView() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e;");

        Label title = new Label("Send Node");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        title.setTextFill(Color.web("#e94560"));

        VBox form = new VBox(15);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(300);
        form.setStyle("-fx-background-color: #16213e; -fx-padding: 30; -fx-background-radius: 10;");

        Label msg = new Label();
        msg.setTextFill(Color.web("#e94560"));

        TextField userField = new TextField();
        userField.setPromptText("Username");
        userField.setStyle("-fx-control-inner-background: #0f3460; -fx-text-fill: white; -fx-prompt-text-fill: gray;");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setStyle("-fx-control-inner-background: #0f3460; -fx-text-fill: white; -fx-prompt-text-fill: gray;");

        Button loginBtn = new Button("Sign In");
        loginBtn.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Button regBtn = new Button("Create Account");
        regBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e94560; -fx-cursor: hand; -fx-border-color: #e94560; -fx-border-radius: 3;");
        regBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            if(App.db.login(userField.getText(), passField.getText())) {
                App.setScene(new DashboardView().getView());
            } else {
                msg.setText("Invalid username or password.");
            }
        });

        regBtn.setOnAction(e -> {
            if(userField.getText().isEmpty() || passField.getText().isEmpty()) {
                msg.setText("Fields cannot be empty.");
                return;
            }
            if(App.db.register(userField.getText(), passField.getText())) {
                App.setScene(new DashboardView().getView());
            } else {
                msg.setText("Username already exists.");
            }
        });

        form.getChildren().addAll(msg, userField, passField, loginBtn, regBtn);
        root.getChildren().addAll(title, form);

        return root;
    }
}
