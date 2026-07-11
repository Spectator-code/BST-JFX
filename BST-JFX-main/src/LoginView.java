import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class LoginView {

    // Track which mode is active
    private boolean isSignInMode = true;

    // Panels that will be swapped
    private VBox signInForm;
    private VBox signUpForm;
    private StackPane rightPanel;

    // Left panel buttons (kept as fields so we can restyle on toggle)
    private Button leftSignInBtn;
    private Button leftSignUpBtn;

    public Parent getView() {
        // ── Root: horizontal split ─────────────────────────────────────────
        HBox root = new HBox();
        root.setMinSize(900, 580);
        root.setPrefSize(900, 580);

        // ══════════════════════════════════════════════════════════════════
        //  LEFT PANEL – green gradient with toggle buttons
        // ══════════════════════════════════════════════════════════════════
        StackPane leftPanel = new StackPane();
        leftPanel.setPrefWidth(360);
        leftPanel.setMinWidth(360);

        LinearGradient greenGrad = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2e9e60")),
                new Stop(1, Color.web("#1a7a44"))
        );
        leftPanel.setBackground(new Background(
                new BackgroundFill(greenGrad, CornerRadii.EMPTY, Insets.EMPTY)));

        VBox leftContent = new VBox(28);
        leftContent.setAlignment(Pos.CENTER);
        leftContent.setPadding(new Insets(60, 50, 60, 50));

        Label welcome = new Label("Welcome Back!");
        welcome.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        welcome.setTextFill(Color.WHITE);

        Label subtitle = new Label("To keep connected with us\nplease login with your personal info");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#d4f0e4"));
        subtitle.setTextAlignment(TextAlignment.CENTER);
        subtitle.setWrapText(true);

        // ── Left "SIGN IN" button (outline, active by default) ─────────────
        leftSignInBtn = makeLeftBtn("SIGN IN", true);
        leftSignUpBtn = makeLeftBtn("SIGN UP", false);

        leftSignInBtn.setOnAction(e -> switchMode(true));
        leftSignUpBtn.setOnAction(e -> switchMode(false));

        leftContent.getChildren().addAll(welcome, subtitle, leftSignInBtn, leftSignUpBtn);
        leftPanel.getChildren().add(leftContent);

        // ══════════════════════════════════════════════════════════════════
        //  RIGHT PANEL – white, shows sign-in or sign-up form
        // ══════════════════════════════════════════════════════════════════
        rightPanel = new StackPane();
        rightPanel.setStyle("-fx-background-color: white;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        signInForm = buildSignInForm();
        signUpForm  = buildSignUpForm();

        // Start with sign-in visible
        signUpForm.setVisible(false);
        signUpForm.setManaged(false);

        rightPanel.getChildren().addAll(signInForm, signUpForm);

        root.getChildren().addAll(leftPanel, rightPanel);
        return root;
    }

    // ── Switch between Sign In / Sign Up ────────────────────────────────────
    private void switchMode(boolean toSignIn) {
        if (isSignInMode == toSignIn) return;
        isSignInMode = toSignIn;

        // Restyle left buttons
        leftSignInBtn.setStyle(activeBtnStyle(toSignIn));
        leftSignUpBtn.setStyle(activeBtnStyle(!toSignIn));

        VBox showForm = toSignIn ? signInForm : signUpForm;
        VBox hideForm = toSignIn ? signUpForm  : signInForm;

        // Fade out old, fade in new
        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), hideForm);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(ev -> {
            hideForm.setVisible(false);
            hideForm.setManaged(false);
            showForm.setManaged(true);
            showForm.setVisible(true);
            showForm.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), showForm);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SIGN IN FORM
    // ══════════════════════════════════════════════════════════════════════
    private VBox buildSignInForm() {
        VBox form = new VBox(18);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(360);
        StackPane.setAlignment(form, Pos.CENTER);

        Label title = new Label("Sign In");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#1a7a44"));

        Label msg = new Label();
        msg.setTextFill(Color.web("#c0392b"));
        msg.setFont(Font.font("Arial", 13));
        msg.setWrapText(true);
        msg.setMaxWidth(300);

        TextField userField = buildTextField("Username");
        PasswordField passField = buildPassField("Password");
        applyFocusStyle(userField);
        applyFocusStylePass(passField);

        Button loginBtn = makeSolidBtn("SIGN IN");

        loginBtn.setOnAction(e -> {
            msg.setText("");
            String user = userField.getText().trim();
            String pass = passField.getText();
            if (user.isEmpty() || pass.isEmpty()) {
                msg.setText("Username and password cannot be empty.");
                return;
            }
            if (App.db.login(user, pass)) {
                App.changeScene(new DashboardView().getView());
            } else {
                msg.setText("Invalid username or password.");
                shakeNode(form);
            }
        });

        passField.setOnAction(e -> loginBtn.fire());

        // "Don't have an account?" link
        Label switchLink = new Label("Don't have an account?  Sign Up →");
        switchLink.setFont(Font.font("Arial", 13));
        switchLink.setTextFill(Color.web("#1a9e5c"));
        switchLink.setStyle("-fx-cursor: hand;");
        switchLink.setOnMouseClicked(e -> switchMode(false));
        switchLink.setOnMouseEntered(e -> switchLink.setStyle(
                "-fx-cursor: hand; -fx-underline: true;"));
        switchLink.setOnMouseExited(e -> switchLink.setStyle(
                "-fx-cursor: hand; -fx-underline: false;"));

        form.getChildren().addAll(title, msg, userField, passField, loginBtn, switchLink);
        return form;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SIGN UP FORM
    // ══════════════════════════════════════════════════════════════════════
    private VBox buildSignUpForm() {
        VBox form = new VBox(18);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(360);
        StackPane.setAlignment(form, Pos.CENTER);

        Label title = new Label("Create Account");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#1a7a44"));

        Label msg = new Label();
        msg.setTextFill(Color.web("#c0392b"));
        msg.setFont(Font.font("Arial", 13));
        msg.setWrapText(true);
        msg.setMaxWidth(300);

        TextField userField  = buildTextField("Username");
        PasswordField passField  = buildPassField("Password");
        PasswordField passField2 = buildPassField("Confirm Password");
        applyFocusStyle(userField);
        applyFocusStylePass(passField);
        applyFocusStylePass(passField2);

        Button regBtn = makeSolidBtn("SIGN UP");

        regBtn.setOnAction(e -> {
            msg.setText("");
            String user  = userField.getText().trim();
            String pass  = passField.getText();
            String pass2 = passField2.getText();
            if (user.isEmpty() || pass.isEmpty()) {
                msg.setText("All fields are required.");
                return;
            }
            if (!pass.equals(pass2)) {
                msg.setText("Passwords do not match.");
                shakeNode(form);
                return;
            }
            if (App.db.register(user, pass)) {
                App.changeScene(new DashboardView().getView());
            } else {
                msg.setText("Username already exists. Try another.");
                shakeNode(form);
            }
        });

        passField2.setOnAction(e -> regBtn.fire());

        // "Already have an account?" link
        Label switchLink = new Label("Already have an account?  Sign In →");
        switchLink.setFont(Font.font("Arial", 13));
        switchLink.setTextFill(Color.web("#1a9e5c"));
        switchLink.setStyle("-fx-cursor: hand;");
        switchLink.setOnMouseClicked(e -> switchMode(true));
        switchLink.setOnMouseEntered(e -> switchLink.setStyle(
                "-fx-cursor: hand; -fx-underline: true;"));
        switchLink.setOnMouseExited(e -> switchLink.setStyle(
                "-fx-cursor: hand; -fx-underline: false;"));

        form.getChildren().addAll(title, msg, userField, passField, passField2, regBtn, switchLink);
        return form;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Button makeLeftBtn(String text, boolean active) {
        Button btn = new Button(text);
        btn.setPrefWidth(210);
        btn.setPrefHeight(44);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btn.setStyle(activeBtnStyle(active));
        return btn;
    }

    private String activeBtnStyle(boolean active) {
        if (active) {
            return "-fx-background-color: white;" +
                   "-fx-text-fill: #1a7a44;" +
                   "-fx-border-color: white;" +
                   "-fx-border-width: 2;" +
                   "-fx-border-radius: 25;" +
                   "-fx-background-radius: 25;" +
                   "-fx-cursor: hand;" +
                   "-fx-font-weight: bold;";
        } else {
            return "-fx-background-color: transparent;" +
                   "-fx-text-fill: white;" +
                   "-fx-border-color: white;" +
                   "-fx-border-width: 2;" +
                   "-fx-border-radius: 25;" +
                   "-fx-background-radius: 25;" +
                   "-fx-cursor: hand;" +
                   "-fx-font-weight: bold;";
        }
    }

    private Button makeSolidBtn(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(48);
        btn.setMaxWidth(300);
        btn.setPrefWidth(300);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        String base = "-fx-background-color: #1a9e5c;" +
                      "-fx-text-fill: white;" +
                      "-fx-background-radius: 25;" +
                      "-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #157a47;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 25;" +
                "-fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private TextField buildTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(46);
        tf.setPrefWidth(300);
        tf.setMaxWidth(300);
        tf.setStyle(fieldStyle());
        return tf;
    }

    private PasswordField buildPassField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefHeight(46);
        pf.setPrefWidth(300);
        pf.setMaxWidth(300);
        pf.setStyle(fieldStyle());
        return pf;
    }

    private String fieldStyle() {
        return "-fx-background-color: #eaf6f0;" +
               "-fx-background-radius: 6;" +
               "-fx-border-color: #c5e8d8;" +
               "-fx-border-radius: 6;" +
               "-fx-border-width: 1.5;" +
               "-fx-text-fill: #2d2d2d;" +
               "-fx-prompt-text-fill: #7a9e8e;" +
               "-fx-font-size: 14;" +
               "-fx-padding: 0 12 0 12;";
    }

    private String fieldFocusStyle() {
        return "-fx-background-color: #d6f2e6;" +
               "-fx-background-radius: 6;" +
               "-fx-border-color: #1a9e5c;" +
               "-fx-border-radius: 6;" +
               "-fx-border-width: 2;" +
               "-fx-text-fill: #2d2d2d;" +
               "-fx-prompt-text-fill: #7a9e8e;" +
               "-fx-font-size: 14;" +
               "-fx-padding: 0 12 0 12;";
    }

    private void applyFocusStyle(TextField tf) {
        tf.focusedProperty().addListener((obs, was, now) ->
            tf.setStyle(now ? fieldFocusStyle() : fieldStyle()));
    }

    private void applyFocusStylePass(PasswordField pf) {
        pf.focusedProperty().addListener((obs, was, now) ->
            pf.setStyle(now ? fieldFocusStyle() : fieldStyle()));
    }

    /** Quick horizontal shake animation for error feedback */
    private void shakeNode(javafx.scene.Node node) {
        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0); tt.setByX(10); tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }
}
