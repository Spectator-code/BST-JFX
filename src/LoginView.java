import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.effect.Glow;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BoxBlur;
import javafx.util.Duration;

/**
 * Premium login / sign-up view with animated BST graphic, glassmorphism
 * form card, icon-prefixed inputs, gradient CTA, and staggered entrance.
 */
public class LoginView {

    private boolean isSignInMode = true;
    private VBox signInForm;
    private VBox signUpForm;
    private StackPane rightPanel;
    private Button leftSignInBtn;
    private Button leftSignUpBtn;

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMATED BST GRAPHIC (larger, 4-level tree with floating animation)
    // ═══════════════════════════════════════════════════════════════════════

    private Pane buildAnimatedBSTGraphic() {
        Pane pane = new Pane();
        pane.setPrefSize(280, 180);
        pane.setMinSize(280, 180);
        pane.setMaxSize(280, 180);

        // 4-level tree:  root(50)  →  left(30), right(70)  →  20,40,60,80
        double[][] positions = {
            {140, 28},   // 0: root
            {75,  78},   // 1: left child
            {205, 78},   // 2: right child
            {40,  138},  // 3: left-left
            {110, 138},  // 4: left-right
            {170, 138},  // 5: right-left
            {240, 138},  // 6: right-right
        };
        int[][] edges = {{0,1},{0,2},{1,3},{1,4},{2,5},{2,6}};
        String[] labels = {"50","30","70","20","40","60","80"};
        String[] nodeColors = {"#2563eb", "#0ea5e9", "#0ea5e9", "#3b82f6", "#3b82f6", "#3b82f6", "#3b82f6"};
        double[] radii = {16, 14, 14, 11, 11, 11, 11};

        // Draw edges first
        for (int[] edge : edges) {
            Line line = new Line(
                positions[edge[0]][0], positions[edge[0]][1],
                positions[edge[1]][0], positions[edge[1]][1]
            );
            line.setStroke(Color.web(Theme.BORDER));
            line.setStrokeWidth(2.0);
            pane.getChildren().add(line);
        }

        // Draw nodes with glow and floating animation
        for (int i = 0; i < positions.length; i++) {
            double x = positions[i][0], y = positions[i][1];
            Circle c = new Circle(x, y, radii[i]);
            c.setFill(Color.web(nodeColors[i]));
            c.setStroke(Color.web("#0c1236"));
            c.setStrokeWidth(2);

            c.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.3)));

            // Gentle floating animation (different phase per node)
            TranslateTransition float_ = new TranslateTransition(
                Duration.millis(2200 + i * 300), c
            );
            float_.setByY(i % 2 == 0 ? -6 : 6);
            float_.setCycleCount(Animation.INDEFINITE);
            float_.setAutoReverse(true);
            float_.setInterpolator(Interpolator.EASE_BOTH);
            float_.play();

            // Node value label
            javafx.scene.text.Text lbl = new javafx.scene.text.Text(labels[i]);
            lbl.setFont(Font.font("Arial", FontWeight.BOLD, i < 3 ? 12 : 10));
            lbl.setFill(Color.WHITE);
            lbl.setX(x - (labels[i].length() > 1 ? 7 : 4));
            lbl.setY(y + 4);
            lbl.setMouseTransparent(true);

            // Same float animation for label
            TranslateTransition floatLbl = new TranslateTransition(
                Duration.millis(2200 + i * 300), lbl
            );
            floatLbl.setByY(i % 2 == 0 ? -6 : 6);
            floatLbl.setCycleCount(Animation.INDEFINITE);
            floatLbl.setAutoReverse(true);
            floatLbl.setInterpolator(Interpolator.EASE_BOTH);
            floatLbl.play();

            pane.getChildren().addAll(c, lbl);
        }

        return pane;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MAIN VIEW
    // ═══════════════════════════════════════════════════════════════════════

    public Parent getView() {
        StackPane rootStack = new StackPane();
        BSTBackgroundPane bgPane = new BSTBackgroundPane();

        // ── Root: horizontal split ─────────────────────────────────────────
        HBox root = new HBox();
        root.setMinSize(900, 580);
        root.setPrefSize(900, 580);
        root.setStyle("-fx-background-color: transparent;");

        // ═══════════════════════ LEFT PANEL ═══════════════════════════════
        StackPane leftPanel = new StackPane();
        leftPanel.setPrefWidth(380);
        leftPanel.setMinWidth(380);

        LinearGradient bg = new LinearGradient(
            0, 0, 0.3, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0f172a", 0.95)),
            new Stop(0.5, Color.web("#111827", 0.95)),
            new Stop(1, Color.web("#1e293b", 0.95))
        );
        leftPanel.setBackground(new Background(new BackgroundFill(bg, CornerRadii.EMPTY, Insets.EMPTY)));
        leftPanel.setStyle("-fx-border-color: " + Theme.BORDER + "80; -fx-border-width: 0 1 0 0;");

        VBox leftContent = new VBox(20);
        leftContent.setAlignment(Pos.CENTER);
        leftContent.setPadding(new Insets(36, 36, 36, 36));

        // Logo image or animated BST
        Pane animBST = buildAnimatedBSTGraphic();
        javafx.scene.Node logoOrGraphic = animBST;
        ImageView miniLogo = null;
        try {
            javafx.scene.image.WritableImage logoImg = App.loadJavaFXImage("assets/logo.png");
            if (logoImg != null) {
                ImageView logoView = new ImageView(logoImg);
                logoView.setFitWidth(200);
                logoView.setPreserveRatio(true);
                logoView.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.3)));
                logoOrGraphic = logoView;

                miniLogo = new ImageView(logoImg);
                miniLogo.setFitHeight(32);
                miniLogo.setPreserveRatio(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Brand name
        Label brand = new Label("Send Node");
        brand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 34));
        brand.setTextFill(Color.web(Theme.ACCENT));
        brand.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.4)));

        // Tagline
        Label tagline = new Label("A Premium Visual Platform for\nMastering Binary Search Trees & AVL Trees.");
        tagline.setFont(Font.font("Arial", 13));
        tagline.setTextFill(Color.web("#7f8fb8"));
        tagline.setTextAlignment(TextAlignment.CENTER);
        tagline.setWrapText(true);

        // Removed feature pills as requested.

        // ── Toggle Buttons (Sign In / Sign Up) ───────────────────────────
        HBox toggleRow = new HBox(10);
        toggleRow.setAlignment(Pos.CENTER);

        leftSignInBtn = makeLeftBtn("SIGN IN", true);
        leftSignUpBtn = makeLeftBtn("SIGN UP", false);
        leftSignInBtn.setOnAction(e -> switchMode(true));
        leftSignUpBtn.setOnAction(e -> switchMode(false));
        toggleRow.getChildren().addAll(leftSignInBtn, leftSignUpBtn);

        // Version footer
        Label version = new Label("v2.0  ·  Send Node BST Platform");
        version.setFont(Font.font("Arial", 10));
        version.setTextFill(Color.web("#2a3564"));

        leftContent.getChildren().addAll(
            logoOrGraphic, brand, tagline, toggleRow, version
        );
        leftPanel.getChildren().add(leftContent);

        // Entrance animation for left panel
        leftContent.setOpacity(0);
        leftContent.setTranslateY(18);
        FadeTransition fadeLeft = new FadeTransition(Duration.millis(600), leftContent);
        fadeLeft.setFromValue(0); fadeLeft.setToValue(1);
        TranslateTransition slideLeft = new TranslateTransition(Duration.millis(600), leftContent);
        slideLeft.setFromY(18); slideLeft.setToY(0);
        slideLeft.setInterpolator(Interpolator.EASE_OUT);
        fadeLeft.play(); slideLeft.play();

        // ═══════════════════════ RIGHT PANEL ══════════════════════════════
        rightPanel = new StackPane();
        rightPanel.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        // Removed radial glow background behind card.

        signInForm = buildSignInForm();
        signUpForm = buildSignUpForm();
        signUpForm.setVisible(false);
        signUpForm.setManaged(false);

        rightPanel.getChildren().addAll(signInForm, signUpForm);
        root.getChildren().addAll(leftPanel, rightPanel);

        rootStack.getChildren().addAll(bgPane, root);
        return rootStack;
    }

    // Removed createPill helper as pills are removed.

    // ═══════════════════════════════════════════════════════════════════════
    //  SIGN IN FORM (glassmorphism card)
    // ═══════════════════════════════════════════════════════════════════════

    private VBox buildSignInForm() {
        VBox card = buildFormCard();

        Label title = new Label("Welcome Back");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Sign in to continue your learning journey");
        subtitle.setFont(Font.font("Arial", 13));
        subtitle.setTextFill(Color.web("#6b7db3"));

        Label msg = buildErrorLabel();

        // Icon-prefixed fields
        VBox userRow = buildIconField("👤", "Username");
        TextField userField = (TextField) userRow.getUserData();

        VBox passRow = buildIconPassField("🔑", "Password");
        PasswordField passField = (PasswordField) passRow.getUserData();

        Button loginBtn = makeGradientBtn("SIGN IN  →");

        loginBtn.setOnAction(e -> {
            msg.setText("");
            String user = userField.getText().trim();
            String pass = passField.getText();
            if (user.isEmpty() || pass.isEmpty()) {
                msg.setText("⚠  Username and password cannot be empty.");
                return;
            }
            if (App.db.login(user, pass)) {
                if ("teacher".equals(App.db.getCurrentUserRole())) {
                    App.changeScene(new TeacherDashboardView().getView());
                } else {
                    App.changeScene(new DashboardView().getView());
                }
            } else {
                msg.setText("✖  Invalid username or password.");
                shakeNode(card);
            }
        });
        passField.setOnAction(e -> loginBtn.fire());

        // Divider line
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e2c5640;");
        sep.setMaxWidth(260);

        Label switchLink = buildSwitchLink("Don't have an account?  Sign Up →", false);

        card.getChildren().addAll(title, subtitle, msg, userRow, passRow, loginBtn, sep, switchLink);
        staggerEntrance(card);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SIGN UP FORM (glassmorphism card)
    // ═══════════════════════════════════════════════════════════════════════

    private VBox buildSignUpForm() {
        VBox card = buildFormCard();

        Label title = new Label("Create Account");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Join and start mastering BST & AVL Trees");
        subtitle.setFont(Font.font("Arial", 13));
        subtitle.setTextFill(Color.web("#6b7db3"));

        Label msg = buildErrorLabel();

        VBox userRow = buildIconField("👤", "Username");
        TextField userField = (TextField) userRow.getUserData();

        VBox passRow = buildIconPassField("🔑", "Password");
        PasswordField passField = (PasswordField) passRow.getUserData();

        VBox passRow2 = buildIconPassField("🔒", "Confirm Password");
        PasswordField passField2 = (PasswordField) passRow2.getUserData();

        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton studentRadio = new RadioButton("🎓 Student");
        studentRadio.setToggleGroup(roleGroup);
        studentRadio.setSelected(true);
        studentRadio.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-font-size: 13px;" +
            "-fx-cursor: hand;"
        );

        RadioButton teacherRadio = new RadioButton("👨‍🏫 Teacher");
        teacherRadio.setToggleGroup(roleGroup);
        teacherRadio.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-font-size: 13px;" +
            "-fx-cursor: hand;"
        );

        HBox roleBox = new HBox(30, studentRadio, teacherRadio);
        roleBox.setAlignment(Pos.CENTER);
        roleBox.setPadding(new Insets(4, 0, 4, 0));

        Button regBtn = makeGradientBtn("CREATE ACCOUNT  →");

        regBtn.setOnAction(e -> {
            msg.setText("");
            String user  = userField.getText().trim();
            String pass  = passField.getText();
            String pass2 = passField2.getText();
            if (user.isEmpty() || pass.isEmpty()) {
                msg.setText("⚠  All fields are required.");
                return;
            }
            if (pass.length() < 4) {
                msg.setText("⚠  Password must be at least 4 characters.");
                shakeNode(card);
                return;
            }
            if (!pass.equals(pass2)) {
                msg.setText("✖  Passwords do not match.");
                shakeNode(card);
                return;
            }
            String selectedRole = studentRadio.isSelected() ? "student" : "teacher";
            if (App.db.register(user, pass, selectedRole)) {
                if ("teacher".equals(selectedRole)) {
                    App.changeScene(new TeacherDashboardView().getView());
                } else {
                    App.changeScene(new DashboardView().getView());
                }
            } else {
                msg.setText("✖  Username already exists. Try another.");
                shakeNode(card);
            }
        });
        passField2.setOnAction(e -> regBtn.fire());

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e2c5640;");
        sep.setMaxWidth(260);

        Label switchLink = buildSwitchLink("Already have an account?  Sign In →", true);

        card.getChildren().addAll(title, subtitle, msg, userRow, passRow, passRow2, roleBox, regBtn, sep, switchLink);
        staggerEntrance(card);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SWITCH MODE
    // ═══════════════════════════════════════════════════════════════════════

    private void switchMode(boolean toSignIn) {
        if (isSignInMode == toSignIn) return;
        isSignInMode = toSignIn;

        leftSignInBtn.setStyle(activeBtnStyle(toSignIn));
        leftSignUpBtn.setStyle(activeBtnStyle(!toSignIn));

        VBox showForm = toSignIn ? signInForm : signUpForm;
        VBox hideForm = toSignIn ? signUpForm  : signInForm;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(160), hideForm);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(ev -> {
            hideForm.setVisible(false);
            hideForm.setManaged(false);
            showForm.setManaged(true);
            showForm.setVisible(true);
            showForm.setOpacity(0);
            showForm.setTranslateY(12);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), showForm);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            TranslateTransition slideUp = new TranslateTransition(Duration.millis(250), showForm);
            slideUp.setFromY(12); slideUp.setToY(0);
            slideUp.setInterpolator(Interpolator.EASE_OUT);
            fadeIn.play(); slideUp.play();
        });
        fadeOut.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UI COMPONENT BUILDERS
    // ═══════════════════════════════════════════════════════════════════════

    /** Glassmorphism card wrapper for the form. */
    private VBox buildFormCard() {
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(380);
        card.setPadding(new Insets(40, 40, 36, 40));
        StackPane.setAlignment(card, Pos.CENTER);
        card.setStyle(
            "-fx-background-color: " + Theme.SURFACE + "ee;" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: " + Theme.BORDER + "88;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 18;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.35), 30, 0, 0, 4);"
        );
        return card;
    }

    /** Error message label. */
    private Label buildErrorLabel() {
        Label msg = new Label();
        msg.setTextFill(Color.web("#ff6b6b"));
        msg.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        msg.setWrapText(true);
        msg.setMaxWidth(300);
        msg.setAlignment(Pos.CENTER);
        msg.setStyle("-fx-padding: 0 0 4 0;");
        return msg;
    }

    /** TextField with icon prefix label, returns VBox; field stored in userData. */
    private VBox buildIconField(String icon, String prompt) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(310);

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("Segoe UI", 16));
        iconLabel.setMinWidth(42);
        iconLabel.setAlignment(Pos.CENTER);
        iconLabel.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-background-radius: 10 0 0 10;" +
            "-fx-border-color: " + Theme.BORDER + ";" +
            "-fx-border-width: 1.5 0 1.5 1.5;" +
            "-fx-border-radius: 10 0 0 10;" +
            "-fx-padding: 0 4 0 4;"
        );
        iconLabel.setPrefHeight(48);

        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(48);
        tf.setMaxWidth(268);
        HBox.setHgrow(tf, Priority.ALWAYS);
        tf.setStyle(fieldStyle());
        applyFocusGlow(tf, iconLabel);

        row.getChildren().addAll(iconLabel, tf);
        VBox wrapper = new VBox(row);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setUserData(tf);
        return wrapper;
    }

    /** PasswordField with icon prefix label. */
    private VBox buildIconPassField(String icon, String prompt) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(310);

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("Segoe UI", 16));
        iconLabel.setMinWidth(42);
        iconLabel.setAlignment(Pos.CENTER);
        iconLabel.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-background-radius: 10 0 0 10;" +
            "-fx-border-color: " + Theme.BORDER + ";" +
            "-fx-border-width: 1.5 0 1.5 1.5;" +
            "-fx-border-radius: 10 0 0 10;" +
            "-fx-padding: 0 4 0 4;"
        );
        iconLabel.setPrefHeight(48);

        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefHeight(48);
        pf.setMaxWidth(268);
        HBox.setHgrow(pf, Priority.ALWAYS);
        pf.setStyle(fieldStyle());
        applyFocusGlowPass(pf, iconLabel);

        row.getChildren().addAll(iconLabel, pf);
        VBox wrapper = new VBox(row);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setUserData(pf);
        return wrapper;
    }

    /** Gradient CTA button with hover lift + glow. */
    private Button makeGradientBtn(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(50);
        btn.setMaxWidth(310);
        btn.setPrefWidth(310);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        String base =
            "-fx-background-color: linear-gradient(to right, " + Theme.PRIMARY + ", " + Theme.ACCENT + ");" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 2);";
        btn.setStyle(base);

        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                "-fx-background-color: linear-gradient(to right, " + Theme.ACCENT + ", #60a5fa);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-scale-x: 1.02; -fx-scale-y: 1.02;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 12, 0, 0, 4);"
            );
        });
        btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnMousePressed(e -> {
            btn.setStyle(
                "-fx-background-color: linear-gradient(to right, " + Theme.PRIMARY2 + ", " + Theme.PRIMARY + ");" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-scale-x: 0.98; -fx-scale-y: 0.98;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.25), 4, 0, 0, 1);"
            );
        });
        btn.setOnMouseReleased(e -> btn.setStyle(base));

        return btn;
    }

    /** "Sign Up / Sign In" clickable link with hover underline. */
    private Label buildSwitchLink(String text, boolean toSignIn) {
        Label link = new Label(text);
        link.setFont(Font.font("Arial", 13));
        link.setTextFill(Color.web(Theme.ACCENT));
        link.setStyle("-fx-cursor: hand;");
        link.setOnMouseClicked(e -> switchMode(toSignIn));
        link.setOnMouseEntered(e -> link.setStyle("-fx-cursor: hand; -fx-underline: true;"));
        link.setOnMouseExited(e  -> link.setStyle("-fx-cursor: hand; -fx-underline: false;"));
        return link;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LEFT PANEL TOGGLE BUTTONS
    // ═══════════════════════════════════════════════════════════════════════

    private Button makeLeftBtn(String text, boolean active) {
        Button btn = new Button(text);
        btn.setPrefWidth(140);
        btn.setPrefHeight(42);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        btn.setStyle(activeBtnStyle(active));
        return btn;
    }

    private String activeBtnStyle(boolean active) {
        if (active) {
            return "-fx-background-color: linear-gradient(to right, " + Theme.PRIMARY + ", " + Theme.ACCENT + ");" +
                   "-fx-text-fill: white;" +
                   "-fx-border-color: transparent;" +
                   "-fx-border-width: 0;" +
                   "-fx-border-radius: 22;" +
                   "-fx-background-radius: 22;" +
                   "-fx-cursor: hand;" +
                   "-fx-font-weight: bold;" +
                   "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 0);";
        } else {
            return "-fx-background-color: transparent;" +
                   "-fx-text-fill: " + Theme.TEXT_MUTED + ";" +
                   "-fx-border-color: " + Theme.BORDER + ";" +
                   "-fx-border-width: 1.5;" +
                   "-fx-border-radius: 22;" +
                   "-fx-background-radius: 22;" +
                   "-fx-cursor: hand;" +
                   "-fx-font-weight: bold;";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FIELD STYLES & FOCUS EFFECTS
    // ═══════════════════════════════════════════════════════════════════════

    private String fieldStyle() {
        return "-fx-background-color: " + Theme.NAV_BG + ";" +
               "-fx-background-radius: 0 10 10 0;" +
               "-fx-border-color: " + Theme.BORDER + ";" +
               "-fx-border-radius: 0 10 10 0;" +
               "-fx-border-width: 1.5 1.5 1.5 0;" +
               "-fx-text-fill: white;" +
               "-fx-prompt-text-fill: " + Theme.TEXT_MUTED + ";" +
               "-fx-font-size: 14;" +
               "-fx-padding: 0 14 0 10;";
    }

    private String fieldFocusStyle() {
        return "-fx-background-color: " + Theme.SURFACE + ";" +
               "-fx-background-radius: 0 10 10 0;" +
               "-fx-border-color: " + Theme.ACCENT + ";" +
               "-fx-border-radius: 0 10 10 0;" +
               "-fx-border-width: 2 2 2 0;" +
               "-fx-text-fill: white;" +
               "-fx-prompt-text-fill: " + Theme.TEXT_MUTED + ";" +
               "-fx-font-size: 14;" +
               "-fx-padding: 0 14 0 10;";
    }

    private String iconFocusStyle() {
        return "-fx-background-color: " + Theme.SURFACE + ";" +
               "-fx-background-radius: 10 0 0 10;" +
               "-fx-border-color: " + Theme.ACCENT + ";" +
               "-fx-border-width: 2 0 2 2;" +
               "-fx-border-radius: 10 0 0 10;" +
               "-fx-padding: 0 4 0 4;";
    }

    private String iconNormalStyle() {
        return "-fx-background-color: " + Theme.SURFACE + ";" +
               "-fx-background-radius: 10 0 0 10;" +
               "-fx-border-color: " + Theme.BORDER + ";" +
               "-fx-border-width: 1.5 0 1.5 1.5;" +
               "-fx-border-radius: 10 0 0 10;" +
               "-fx-padding: 0 4 0 4;";
    }

    /** Applies glow on focus to both the icon prefix and the text field. */
    private void applyFocusGlow(TextField tf, Label icon) {
        tf.focusedProperty().addListener((obs, was, now) -> {
            tf.setStyle(now ? fieldFocusStyle() : fieldStyle());
            icon.setStyle(now ? iconFocusStyle() : iconNormalStyle());
        });
    }

    private void applyFocusGlowPass(PasswordField pf, Label icon) {
        pf.focusedProperty().addListener((obs, was, now) -> {
            pf.setStyle(now ? fieldFocusStyle() : fieldStyle());
            icon.setStyle(now ? iconFocusStyle() : iconNormalStyle());
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /** Stagger-fades each child of the card from top to bottom. */
    private void staggerEntrance(VBox card) {
        javafx.application.Platform.runLater(() -> {
            for (int i = 0; i < card.getChildren().size(); i++) {
                javafx.scene.Node child = card.getChildren().get(i);
                child.setOpacity(0);
                child.setTranslateY(10);

                FadeTransition ft = new FadeTransition(Duration.millis(300), child);
                ft.setFromValue(0); ft.setToValue(1);
                ft.setDelay(Duration.millis(80 + i * 65));

                TranslateTransition tt = new TranslateTransition(Duration.millis(300), child);
                tt.setFromY(10); tt.setToY(0);
                tt.setDelay(Duration.millis(80 + i * 65));
                tt.setInterpolator(Interpolator.EASE_OUT);

                ft.play(); tt.play();
            }
        });
    }

    /** Horizontal shake for error feedback. */
    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(55), node);
        tt.setFromX(0); tt.setByX(12); tt.setCycleCount(5);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }
}
