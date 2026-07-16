import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

public class DashboardView {

    public Parent getView() {
        StackPane rootStack = new StackPane();
        BSTBackgroundPane bgPane = new BSTBackgroundPane();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: transparent;");

        // ── Top Nav ───────────────────────────────────────────────────────
        HBox nav = new HBox();
        nav.setStyle("-fx-background-color: " + Theme.NAV_BG + "; -fx-padding: 14 20;");
        nav.setAlignment(Pos.CENTER_LEFT);

        ImageView miniLogo = null;
        try {
            javafx.scene.image.WritableImage logoImg = App.loadJavaFXImage("assets/logo.png");
            if (logoImg != null) {
                miniLogo = new ImageView(logoImg);
                miniLogo.setFitHeight(24);
                miniLogo.setPreserveRatio(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Label brand = new Label("  Send Node");
        brand.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        brand.setTextFill(Color.web(Theme.ACCENT));
        if (miniLogo != null) {
            brand.setGraphic(miniLogo);
            brand.setContentDisplay(ContentDisplay.LEFT);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String user = App.db.getCurrentUser();
        Label userLabel = new Label("👤  " + (user != null ? user : "Guest"));
        userLabel.setTextFill(Color.web(Theme.TEXT_LIGHT));
        userLabel.setFont(Font.font("Arial", 14));
        userLabel.setStyle("-fx-padding: 0 18 0 0;");

        Button logoutBtn = styledBtn("Logout", Theme.DANGER);
        logoutBtn.setOnAction(e -> confirmLogout());

        nav.getChildren().addAll(brand, spacer, userLabel, logoutBtn);
        root.setTop(nav);

        // ── Center ────────────────────────────────────────────────────────
        ScrollPane centerScroll = new ScrollPane();
        centerScroll.setFitToWidth(true);
        centerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox center = new VBox(32);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(36, 60, 40, 60));
        centerScroll.setContent(center);

        // ── Header ────────────────────────────────────────────────────────
        Label hello = new Label("Welcome back, " + (user != null ? user : "Learner") + "!");
        hello.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        hello.setTextFill(Color.WHITE);

        Label subtitle = new Label("Pick a mode below and keep mastering Binary Search Trees.");
        subtitle.setFont(Font.font("Arial", 15));
        subtitle.setTextFill(Color.web(Theme.TEXT_MUTED));

        // ── Progress Section ──────────────────────────────────────────────
        int totalProblems = ProblemManager.getAll().size();
        int solved = App.db.getSolvedProblems().size();
        double pct = totalProblems == 0 ? 0 : (double) solved / totalProblems;

        VBox progressBox = new VBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMaxWidth(560);
        progressBox.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 20 30;" +
            "-fx-border-color: " + Theme.BORDER + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 14;"
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
        progressPct.setTextFill(Color.web(Theme.ACCENT));

        progressHeader.getChildren().addAll(progressTitle, ph, progressPct);

        ProgressBar progressBar = new ProgressBar(pct);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(14);
        progressBar.setStyle(
            "-fx-accent: " + Theme.PRIMARY + ";" +
            "-fx-background-color: " + Theme.BORDER + ";" +
            "-fx-background-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 4, 0, 0, 0);"
        );

        // Animate progress bar fill on load
        progressBar.setProgress(0);
        javafx.animation.Timeline progAnim = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(700),
                new javafx.animation.KeyValue(progressBar.progressProperty(), pct,
                    javafx.animation.Interpolator.EASE_OUT))
        );
        javafx.application.Platform.runLater(progAnim::play);

        // ── Per-difficulty breakdown ───────────────────────────────────────
        java.util.Map<String, Integer> totalPerDiff = ProblemManager.countByDifficulty();
        java.util.List<Integer> solvedList = App.db.getSolvedProblems();
        java.util.Map<String, Integer> solvedPerDiff = new java.util.LinkedHashMap<>();
        solvedPerDiff.put("easy", 0); solvedPerDiff.put("medium", 0); solvedPerDiff.put("hard", 0);
        for (ProblemManager.Problem pr : ProblemManager.getAll()) {
            if (solvedList.contains(pr.id)) {
                String k = pr.difficulty.toLowerCase();
                solvedPerDiff.put(k, solvedPerDiff.getOrDefault(k, 0) + 1);
            }
        }

        HBox diffRow = new HBox(12);
        diffRow.setAlignment(Pos.CENTER);
        diffRow.getChildren().addAll(
            createDiffChip("Easy",   solvedPerDiff.get("easy"),   totalPerDiff.get("easy"),   Theme.SUCCESS),
            createDiffChip("Medium", solvedPerDiff.get("medium"), totalPerDiff.get("medium"), Theme.WARN),
            createDiffChip("Hard",   solvedPerDiff.get("hard"),   totalPerDiff.get("hard"),   Theme.DANGER)
        );

        progressBox.getChildren().addAll(progressHeader, progressBar, diffRow);

        // ── Quick Stats Row ───────────────────────────────────────────────
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER);

        VBox stat1 = createStatCard("Rank", DatabaseManager.getRankTitle(solved), Theme.ACCENT);
        VBox stat2 = createStatCard("XP Earned", (solved * 100) + " XP", Theme.SUCCESS);
        VBox stat3 = createStatCard("Completed", solved + " / " + totalProblems, Theme.WARN);

        statsRow.getChildren().addAll(stat1, stat2, stat3);

        // ── Continue / Reset row ──────────────────────────────────────────
        HBox quickRow = new HBox(14);
        quickRow.setAlignment(Pos.CENTER);

        int firstUnsolved = App.db.getFirstUnsolvedProblemId();
        if (firstUnsolved != -1) {
            Button continueBtn = makeContinueButton("▶  Continue — Problem " + firstUnsolved, firstUnsolved);
            quickRow.getChildren().add(continueBtn);
        } else {
            Label allDone = new Label("🏆  All problems solved! Great job!");
            allDone.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            allDone.setTextFill(Color.web(Theme.SUCCESS));
            quickRow.getChildren().add(allDone);
        }

        Button resetBtn = makeResetButton();
        quickRow.getChildren().add(resetBtn);

        // ── Mode Cards ────────────────────────────────────────────────────
        HBox modes = new HBox(30);
        modes.setAlignment(Pos.CENTER);

        VBox expCard = createModeCard(
            "🌳  Explorer Mode",
            "Interactive BST Visualizer\nInsert · Delete · Search · Zoom · Pan",
            Theme.PRIMARY, Theme.PRIMARY2
        );
        expCard.setOnMouseClicked(e -> App.changeScene(new ExplorerView().getView()));

        VBox chaCard = createModeCard(
            "⚔  Challenge Mode",
            "Solve " + totalProblems + " BST problems\nin the built-in code editor.",
            "#8e00e0", "#5500cc"
        );
        chaCard.setOnMouseClicked(e -> App.changeScene(new ChallengeView().getView()));

        modes.getChildren().addAll(expCard, chaCard);

        center.getChildren().addAll(hello, subtitle, progressBox, statsRow, quickRow, modes);
        root.setCenter(centerScroll);

        rootStack.getChildren().addAll(bgPane, root);
        return rootStack;
    }

    // ── Per-difficulty solved chip ────────────────────────────────────────
    private HBox createDiffChip(String label, int done, int total, String color) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER);
        chip.setStyle(
            "-fx-background-color: " + color + "1a;" +
            "-fx-border-color: " + color + "88;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 5 14;"
        );
        Label lbl = new Label(label + "  " + done + "/" + total);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web(color));
        chip.getChildren().add(lbl);
        return chip;
    }

    // ── Continue button ───────────────────────────────────────────────────
    private Button makeContinueButton(String text, int problemId) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        String base =
            "-fx-background-color: " + Theme.SUCCESS + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 10 20;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.25), 6, 0, 0, 1);";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        btn.setOnAction(e -> {
            ChallengeView cv = new ChallengeView(problemId);
            // Navigate to challenge view and auto-select the correct problem
            App.changeScene(cv.getView());
        });
        return btn;
    }

    // ── Reset progress button with confirmation dialog ────────────────────
    private Button makeResetButton() {
        Button btn = new Button("🗑  Reset Progress");
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + Theme.DANGER + ";" +
            "-fx-border-color: " + Theme.DANGER + "88;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 9 16;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: " + Theme.DANGER + "22;" +
            "-fx-text-fill: " + Theme.DANGER + ";" +
            "-fx-border-color: " + Theme.DANGER + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 9 16;" +
            "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + Theme.DANGER + ";" +
            "-fx-border-color: " + Theme.DANGER + "88;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 9 16;" +
            "-fx-cursor: hand;"
        ));
        btn.setOnAction(e -> confirmResetProgress());
        return btn;
    }

    private void confirmResetProgress() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Progress");
        alert.setHeaderText("Reset all challenge progress?");
        alert.setContentText("This will clear all solved problems for your account. This cannot be undone.");
        styleDialog(alert.getDialogPane());
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                App.db.resetProgress();
                App.changeScene(new DashboardView().getView());
            }
        });
    }

    // ── Logout with confirmation ──────────────────────────────────────────
    private void confirmLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("Your progress is saved.");
        styleDialog(alert.getDialogPane());
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                App.db.logout();
                App.changeScene(new LoginView().getView());
            }
        });
    }

    private void styleDialog(DialogPane dp) {
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.PRIMARY + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        javafx.scene.Node headerPanel = dp.lookup(".header-panel");
        if (headerPanel != null) headerPanel.setStyle("-fx-background-color: " + Theme.NAV_BG + ";");
    }

    // ── Stat card ─────────────────────────────────────────────────────────
    private VBox createStatCard(String labelText, String valText, String colorHex) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 22, 14, 22));
        card.setPrefWidth(160);
        card.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + Theme.BORDER + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.15), 6, 0, 0, 2);"
        );

        Label lbl = new Label(labelText.toUpperCase());
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        lbl.setTextFill(Color.web(Theme.TEXT_MUTED));

        Label val = new Label(valText);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        val.setTextFill(Color.web(colorHex));

        card.getChildren().addAll(lbl, val);
        return card;
    }

    // ── Mode card with gradient ───────────────────────────────────────────
    private VBox createModeCard(String titleStr, String descStr, String col1, String col2) {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 30, 36, 30));
        card.setPrefSize(280, 210);

        String baseStyle =
            "-fx-background-color: linear-gradient(to bottom, " + col1 + ", " + col2 + ");" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.15);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.2;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 12, 0, 0, 4);";
        card.setStyle(baseStyle);

        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + col1 + ", " + col2 + ");" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.4);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.5;" +
            "-fx-cursor: hand;" +
            "-fx-scale-x: 1.04; -fx-scale-y: 1.04;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 20, 0, 0, 6);"
        ));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        Label title = new Label(titleStr);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);

        Label desc = new Label(descStr);
        desc.setWrapText(true);
        desc.setTextFill(Color.web("#d0d8ff"));
        desc.setFont(Font.font("Arial", 13));
        desc.setAlignment(Pos.CENTER);
        desc.setStyle("-fx-text-alignment: center;");

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
