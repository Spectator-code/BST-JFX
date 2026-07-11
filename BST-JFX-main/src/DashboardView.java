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

public class DashboardView {

    // ── Shared blue theme ────────────────────────────────────────────────
    private static final String BG        = "#0a0f2e";
    private static final String SURFACE   = "#0f1847";
    private static final String NAV_BG    = "#0c1236";
    private static final String PRIMARY   = "#0048ff";
    private static final String PRIMARY2  = "#1101ef";
    private static final String ACCENT    = "#4d80ff";
    private static final String SUCCESS   = "#27ae60";
    private static final String DANGER    = "#e74c3c";

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        // ── Top Nav ───────────────────────────────────────────────────────
        HBox nav = new HBox();
        nav.setStyle("-fx-background-color: " + NAV_BG + "; -fx-padding: 14 20;");
        nav.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("⬡  Send Node");
        brand.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        brand.setTextFill(Color.web(ACCENT));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String user = App.db.getCurrentUser();
        Label userLabel = new Label("👤  " + (user != null ? user : "Guest"));
        userLabel.setTextFill(Color.web("#a0b4ff"));
        userLabel.setFont(Font.font("Arial", 14));
        userLabel.setStyle("-fx-padding: 0 18 0 0;");

        Button logoutBtn = styledBtn("Logout", DANGER);
        logoutBtn.setOnAction(e -> confirmLogout());

        nav.getChildren().addAll(brand, spacer, userLabel, logoutBtn);
        root.setTop(nav);

        // ── Center ────────────────────────────────────────────────────────
        VBox center = new VBox(36);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40, 60, 40, 60));

        // ── Header ────────────────────────────────────────────────────────
        Label hello = new Label("Welcome back, " + (user != null ? user : "Learner") + "!");
        hello.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        hello.setTextFill(Color.WHITE);

        Label subtitle = new Label("Pick a mode below and keep learning Binary Search Trees.");
        subtitle.setFont(Font.font("Arial", 15));
        subtitle.setTextFill(Color.web("#7a90c0"));

        // ── Progress Section ──────────────────────────────────────────────
        int totalProblems = ProblemManager.getAll().size();
        int solved = App.db.getSolvedProblems().size();
        double pct = totalProblems == 0 ? 0 : (double) solved / totalProblems;

        VBox progressBox = new VBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMaxWidth(500);
        progressBox.setStyle(
            "-fx-background-color: " + SURFACE + ";" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 20 30;"
        );

        HBox progressHeader = new HBox();
        progressHeader.setAlignment(Pos.CENTER_LEFT);

        Label progressTitle = new Label("Challenge Progress");
        progressTitle.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        progressTitle.setTextFill(Color.WHITE);

        Region ph = new Region();
        HBox.setHgrow(ph, Priority.ALWAYS);

        Label progressPct = new Label(solved + " / " + totalProblems + "  (" + (int)(pct * 100) + "%)");
        progressPct.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        progressPct.setTextFill(Color.web(ACCENT));

        progressHeader.getChildren().addAll(progressTitle, ph, progressPct);

        ProgressBar progressBar = new ProgressBar(pct);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(14);
        progressBar.setStyle(
            "-fx-accent: " + PRIMARY + ";" +
            "-fx-background-color: #1a2255;" +
            "-fx-background-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, " + PRIMARY + ", 6, 0.4, 0, 0);"
        );

        progressBox.getChildren().addAll(progressHeader, progressBar);

        // ── Mode Cards ────────────────────────────────────────────────────
        HBox modes = new HBox(30);
        modes.setAlignment(Pos.CENTER);

        VBox expCard = createModeCard(
            "🌳  Explorer Mode",
            "Interactive BST Visualizer.\nInsert · Delete · Search · Zoom · Pan",
            PRIMARY, PRIMARY2
        );
        expCard.setOnMouseClicked(e -> App.changeScene(new ExplorerView().getView()));

        VBox chaCard = createModeCard(
            "⚔  Challenge Mode",
            "Solve " + totalProblems + " BST problems\nin the built-in code editor.",
            "#8e00e0", "#5500cc"
        );
        chaCard.setOnMouseClicked(e -> App.changeScene(new ChallengeView().getView()));

        modes.getChildren().addAll(expCard, chaCard);

        center.getChildren().addAll(hello, subtitle, progressBox, modes);
        root.setCenter(center);
        return root;
    }

    // ── Logout with confirmation ──────────────────────────────────────────
    private void confirmLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("Your progress is saved.");

        // Style the dialog
        DialogPane dp = alert.getDialogPane();
        dp.setStyle(
            "-fx-background-color: #0f1847;" +
            "-fx-border-color: #0048ff;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookup(".content.label").setStyle("-fx-text-fill: white;");
        dp.lookup(".header-panel").setStyle("-fx-background-color: #0c1236;");
        dp.lookupAll(".label").forEach(n ->
            n.setStyle("-fx-text-fill: white;"));

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                App.db.logout();
                App.changeScene(new LoginView().getView());
            }
        });
    }

    // ── Card builder with gradient ────────────────────────────────────────
    private VBox createModeCard(String titleStr, String descStr, String col1, String col2) {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 30, 36, 30));
        card.setPrefSize(280, 210);
        card.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + col1 + ", " + col2 + ");" +
            "-fx-background-radius: 18;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + col1 + ", 18, 0.35, 0, 4);"
        );

        // Hover animation
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + col1 + ", " + col2 + ");" +
            "-fx-background-radius: 18;" +
            "-fx-cursor: hand;" +
            "-fx-scale-x: 1.04; -fx-scale-y: 1.04;" +
            "-fx-effect: dropshadow(gaussian, " + col1 + ", 28, 0.5, 0, 6);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + col1 + ", " + col2 + ");" +
            "-fx-background-radius: 18;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + col1 + ", 18, 0.35, 0, 4);"
        ));

        Label title = new Label(titleStr);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);

        Label desc = new Label(descStr);
        desc.setWrapText(true);
        desc.setTextFill(Color.web("#d0d8ff"));
        desc.setFont(Font.font("Arial", 13));
        desc.setAlignment(Pos.CENTER);

        card.getChildren().addAll(title, desc);
        return card;
    }

    private Button styledBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 8 16;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.82));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }
}
