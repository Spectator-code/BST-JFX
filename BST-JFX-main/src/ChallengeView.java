import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ChallengeView {

    // ── Blue theme constants ─────────────────────────────────────────────
    private static final String BG      = "#0a0f2e";
    private static final String SURFACE = "#0f1847";
    private static final String NAV_BG  = "#0c1236";
    private static final String PRIMARY = "#0048ff";
    private static final String ACCENT  = "#4d80ff";
    private static final String SUCCESS = "#27ae60";
    private static final String DANGER  = "#e74c3c";

    private int currentProblemId = 1;
    private VBox leftPanel;
    private ExplorerView explorer;

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        // ── Top Nav ───────────────────────────────────────────────────────
        HBox nav = new HBox(14);
        nav.setStyle("-fx-background-color: " + NAV_BG + "; -fx-padding: 10 16;");
        nav.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button backBtn = styledBtn("← Dashboard", "#555a7a");
        backBtn.setOnAction(e -> App.changeScene(new DashboardView().getView()));

        Label title = new Label("⚔  Challenge Mode");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(ACCENT));

        // Solved counter in nav
        int totalSolved = App.db.getSolvedProblems().size();
        int total = ProblemManager.getAll().size();
        Label solvedNav = new Label("✅  " + totalSolved + " / " + total + " solved");
        solvedNav.setTextFill(Color.web("#a0b4ff"));
        solvedNav.setFont(Font.font("Arial", 13));

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);

        nav.getChildren().addAll(backBtn, title, navSpacer, solvedNav);
        root.setTop(nav);

        // ── Split Pane ────────────────────────────────────────────────────
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: " + BG + ";");

        // Left Panel
        leftPanel = new VBox(12);
        leftPanel.setPadding(new Insets(16));
        leftPanel.setStyle("-fx-background-color: " + SURFACE + ";");
        leftPanel.setMinWidth(260);

        loadProblem(currentProblemId);

        // Right Panel
        explorer = new ExplorerView(true);
        Parent rightPanel = explorer.getView();

        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.3);

        root.setCenter(splitPane);
        return root;
    }

    private void loadProblem(int id) {
        leftPanel.getChildren().clear();
        ProblemManager.Problem p = ProblemManager.get(id);
        java.util.List<Integer> solvedList = App.db.getSolvedProblems();

        // ── Problem selector with solved indicators ────────────────────────
        Label selectorLabel = new Label("Select Problem:");
        selectorLabel.setTextFill(Color.web("#a0b4ff"));
        selectorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        ComboBox<String> selector = new ComboBox<>();
        selector.setMaxWidth(Double.MAX_VALUE);
        selector.setStyle(
            "-fx-background-color: #1a2255;" +
            "-fx-text-fill: white;" +
            "-fx-border-color: " + PRIMARY + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );
        for (ProblemManager.Problem prob : ProblemManager.getAll()) {
            String mark = solvedList.contains(prob.id) ? "✅ " : "   ";
            selector.getItems().add(mark + prob.id + ". " + prob.title);
        }
        selector.getSelectionModel().select(id - 1);
        selector.setOnAction(e -> {
            int selectedId = selector.getSelectionModel().getSelectedIndex() + 1;
            loadProblem(selectedId);
        });

        // ── Problem title ─────────────────────────────────────────────────
        Label pTitle = new Label(p.id + ". " + p.title);
        pTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        pTitle.setTextFill(Color.WHITE);
        pTitle.setWrapText(true);

        // ── Difficulty badge ──────────────────────────────────────────────
        String diffColor = switch (p.difficulty.toLowerCase()) {
            case "easy"   -> SUCCESS;
            case "medium" -> "#e67e22";
            default       -> DANGER;
        };
        Label pDiff = new Label(p.difficulty.toUpperCase());
        pDiff.setTextFill(Color.web(diffColor));
        pDiff.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        pDiff.setStyle(
            "-fx-background-color: " + diffColor + "22;" +
            "-fx-border-color: " + diffColor + ";" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 3 10;"
        );

        // ── Already solved badge ──────────────────────────────────────────
        boolean isSolved = solvedList.contains(id);
        Label solvedBadge = new Label(isSolved ? "✅  Already Solved" : "");
        solvedBadge.setTextFill(Color.web(SUCCESS));
        solvedBadge.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        // ── Description ───────────────────────────────────────────────────
        Label pDesc = new Label(p.description);
        pDesc.setWrapText(true);
        pDesc.setTextFill(Color.web("#b0c4ff"));
        pDesc.setFont(Font.font("Arial", 13));

        // ── Hint ──────────────────────────────────────────────────────────
        if (!p.hints.isEmpty()) {
            Label hintLabel = new Label("💡 Hint: " + p.hints.get(0));
            hintLabel.setWrapText(true);
            hintLabel.setTextFill(Color.web("#ffd966"));
            hintLabel.setFont(Font.font("Arial", 12));
            hintLabel.setStyle(
                "-fx-background-color: #2a2000;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 8 10;" +
                "-fx-border-color: #665500;" +
                "-fx-border-radius: 8;"
            );
            leftPanel.getChildren().add(hintLabel);
        }

        // ── Submit ────────────────────────────────────────────────────────
        Button submitBtn = styledBtn("▶  Submit & Check", SUCCESS);
        submitBtn.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        submitBtn.setOnAction(e -> {
            String code = explorer != null ? explorer.getCode().trim() : "";
            if (code.isEmpty()) {
                statusLabel.setText("❌  Code editor is empty!");
                statusLabel.setTextFill(Color.web(DANGER));
                return;
            }
            boolean passed = checkCode(id);
            if (passed) {
                statusLabel.setText("✅  Tests Passed! Problem solved.");
                statusLabel.setTextFill(Color.web(SUCCESS));
                App.db.markProblemSolved(id);
                // Reload to update solved indicator
                loadProblem(id);
            } else {
                statusLabel.setText("❌  Failed. Run your code first, then submit.");
                statusLabel.setTextFill(Color.web(DANGER));
            }
        });

        // ── Navigation buttons ────────────────────────────────────────────
        HBox navBtns = new HBox(8);
        Button prevBtn = styledBtn("◀ Prev", "#555a7a");
        Button nextBtn = styledBtn("Next ▶", PRIMARY);
        prevBtn.setDisable(id <= 1);
        nextBtn.setDisable(id >= ProblemManager.getAll().size());
        prevBtn.setOnAction(e -> loadProblem(id - 1));
        nextBtn.setOnAction(e -> loadProblem(id + 1));
        navBtns.getChildren().addAll(prevBtn, nextBtn);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1a2a5a;");

        leftPanel.getChildren().addAll(
            selectorLabel, selector,
            pTitle, pDiff, solvedBadge,
            pDesc, sep, submitBtn, statusLabel, navBtns
        );

        // Pre-load starting code into explorer editor
        if (explorer != null && p.startingCode != null && !p.startingCode.isEmpty()) {
            explorer.setCode(p.startingCode);
        }
    }

    private boolean checkCode(int probId) {
        String log = explorer.getOutputLog().toLowerCase();
        return switch (probId) {
            case 1  -> explorer.hasNode(10);
            case 2  -> explorer.hasNode(20) && explorer.hasNode(30)
                        && (log.contains("found node 30") || log.contains("✔"));
            case 3  -> explorer.hasNode(50) && explorer.hasNode(70) && !explorer.hasNode(30);
            case 4  -> explorer.hasNode(15) && explorer.hasNode(25);
            case 5  -> !explorer.hasNode(40) && explorer.hasNode(20);
            case 6  -> log.contains("found node") || log.contains("✔");
            case 7  -> explorer.hasNode(100);
            case 8  -> explorer.hasNode(5) && explorer.hasNode(3) && explorer.hasNode(7);
            case 9  -> log.contains("inserted") || log.contains("inserted node");
            case 10 -> explorer.hasNode(55);
            case 11 -> !explorer.hasNode(60);
            case 12 -> explorer.hasNode(1) && explorer.hasNode(2) && explorer.hasNode(3);
            case 13 -> log.contains("build successful") || log.contains("✅");
            case 14 -> explorer.hasNode(99);
            default -> explorer.getCode().trim().length() > 5;
        };
    }

    private Button styledBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 9 16;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.82));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }
}
