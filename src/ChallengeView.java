import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ChallengeView {

    private int currentProblemId = 1;
    private VBox leftPanel;
    private ExplorerView explorer;

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");

        // Top Nav
        HBox nav = new HBox(15);
        nav.setStyle("-fx-background-color: #16213e; -fx-padding: 10;");
        Button backBtn = new Button("← Dashboard");
        backBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-cursor: hand;");
        backBtn.setOnAction(e -> App.setScene(new DashboardView().getView()));

        Label title = new Label("Challenge Mode");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#e94560"));

        nav.getChildren().addAll(backBtn, title);
        root.setTop(nav);

        // Split Pane
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #1a1a2e;");

        // Left Panel (Problem Selection & Description)
        leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setStyle("-fx-background-color: #16213e;");
        leftPanel.setMinWidth(250);

        loadProblem(currentProblemId);

        // Right Panel (Explorer View acts as our IDE and visualizer)
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

        Label pTitle = new Label(p.id + ". " + p.title);
        pTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        pTitle.setTextFill(Color.WHITE);

        Label pDiff = new Label(p.difficulty.toUpperCase());
        pDiff.setTextFill(p.difficulty.equals("easy") ? Color.web("#27ae60") : Color.web("#e67e22"));
        pDiff.setStyle("-fx-background-color: #333; -fx-padding: 3 8 3 8; -fx-background-radius: 12; -fx-font-size: 11px;");

        Label pDesc = new Label(p.description);
        pDesc.setWrapText(true);
        pDesc.setTextFill(Color.LIGHTGRAY);

        // Problem list for navigation
        ComboBox<String> selector = new ComboBox<>();
        for(ProblemManager.Problem prob : ProblemManager.getAll()) {
            selector.getItems().add(prob.id + ". " + prob.title);
        }
        selector.getSelectionModel().select(id - 1);
        selector.setOnAction(e -> {
            int selectedId = selector.getSelectionModel().getSelectedIndex() + 1;
            loadProblem(selectedId);
        });

        Button submitBtn = new Button("Submit Code");
        submitBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        
        Label statusLabel = new Label();
        statusLabel.setTextFill(Color.WHITE);

        submitBtn.setOnAction(e -> {
            String code = explorer != null ? explorer.getCode().trim() : "";
            if (code.isEmpty()) {
                statusLabel.setText("❌ Error: Code editor is empty!");
                statusLabel.setTextFill(Color.web("#e74c3c"));
                return;
            }

            boolean passed = checkCode(id, code);

            if (passed) {
                statusLabel.setText("✅ Tests Passed! Problem solved.");
                statusLabel.setTextFill(Color.web("#27ae60"));
                App.db.markProblemSolved(id);
            } else {
                statusLabel.setText("❌ Failed. Did you click 'Run All' to build the tree?");
                statusLabel.setTextFill(Color.web("#e74c3c"));
            }
        });

        leftPanel.getChildren().addAll(selector, pTitle, pDiff, pDesc, new Separator(), submitBtn, statusLabel);
    }

    private boolean checkCode(int probId, String code) {
        String log = explorer.getOutputLog().toLowerCase();
        switch (probId) {
            case 1: // Insert 10
                return explorer.hasNode(10);
            case 2: // Search 30
                return explorer.hasNode(20) && explorer.hasNode(30) && (log.contains("found node 30") || log.contains("✔"));
            case 3: // Delete 30
                return explorer.hasNode(50) && explorer.hasNode(70) && !explorer.hasNode(30);
            default:
                // For unmocked problems, just check if they wrote something basic
                return code.length() > 5;
        }
    }
}
