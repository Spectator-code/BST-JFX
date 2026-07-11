import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Binary Search Tree Visualizer with panning, zooming, and a Java code editor.
 */
public class ExplorerView {

    // ── Constants ──────────────────────────────────────────────
    private static final double ROOT_Y       = 60;
    private static final double VERTICAL_GAP = 70;
    private static final double MIN_H_GAP    = 50;
    private static final double ANIM_MS      = 350;
    private static final double EXEC_DELAY   = 700;

    // ── State ──────────────────────────────────────────────────
    private TreeNode root;
    private Pane canvas;
    private Pane viewport;
    private Label statusLabel;
    private TextArea codeArea;
    private VBox codePanel;
    private Button toggleCodeBtn;
    private boolean codePanelVisible = true;
    private TextArea outputArea;
    private int executingLine = -1;
    private int animationSpeed = 4;
    private String accentColorHex = "#0df2c9";
    private Button activeModeButton;
    private Button[] treeModeButtons;
    private Slider speedSlider;

    // Pan & Zoom state
    private Scale scaleTransform = new Scale(1, 1, 0, 0);
    private Translate translateTransform = new Translate(0, 0);
    private double dragStartX, dragStartY;
    private double dragTranslateX, dragTranslateY;

    // ── Regex patterns for Java-style commands ─────────────────
    private static final Pattern METHOD_CALL =
            Pattern.compile("^\\s*(?:\\w+\\.)(insert|delete|search|clear)\\s*\\(\\s*(\\d*)\\s*\\)\\s*;?\\s*$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern CONSTRUCTOR =
            Pattern.compile("^\\s*(?:BinarySearchTree|BST|TreeNode)\\s+\\w+\\s*=\\s*new\\s+(?:BinarySearchTree|BST|TreeNode)\\s*\\(\\s*\\)\\s*;?\\s*$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern SYSOUT =
            Pattern.compile("^\\s*System\\.out\\.println\\s*\\((.*)\\)\\s*;?\\s*$");
    private static final Pattern ARRAY_DECL =
            Pattern.compile("^\\s*int\\[\\]\\s+\\w+\\s*=\\s*(?:new\\s+int\\[\\]\\s*)?\\{([^}]+)\\}\\s*;?\\s*$");
    private static final Pattern FOR_EACH =
            Pattern.compile("^\\s*for\\s*\\(\\s*int\\s+\\w+\\s*:\\s*(\\w+)\\s*\\)\\s*\\{?\\s*(?:\\w+\\.)(insert|delete|search)\\s*\\(\\s*\\w+\\s*\\)\\s*;?\\s*\\}?\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private java.util.Map<String, int[]> declaredArrays = new java.util.HashMap<>();
    
    private boolean hideManualControls;

    public ExplorerView() {
        this(false);
    }

    public ExplorerView(boolean hideManualControls) {
        this.hideManualControls = hideManualControls;
    }

    // ── Application Entry ──────────────────────────────────────
    public Parent getView() {

        // ── Canvas & Viewport (Pan/Zoom) ───────────────────────
        canvas = new Pane();
        // Use a large virtual size for the canvas so it acts as an infinite board
        canvas.setPrefSize(4000, 4000);
        canvas.getTransforms().addAll(translateTransform, scaleTransform);

        viewport = new Pane(canvas);
        viewport.setStyle("-fx-background-color: #0a0f2e;");
        viewport.setMinSize(0, 0);
        viewport.setPrefSize(800, 600);
        
        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(viewport.widthProperty());
        clipRect.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clipRect);

        // Pan handling
        viewport.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown() || e.isMiddleButtonDown() || e.isSecondaryButtonDown()) {
                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
                dragTranslateX = translateTransform.getX();
                dragTranslateY = translateTransform.getY();
                viewport.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            }
        });

        viewport.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown() || e.isMiddleButtonDown() || e.isSecondaryButtonDown()) {
                translateTransform.setX(dragTranslateX + (e.getSceneX() - dragStartX));
                translateTransform.setY(dragTranslateY + (e.getSceneY() - dragStartY));
            }
        });

        viewport.setOnMouseReleased(e -> viewport.setCursor(javafx.scene.Cursor.DEFAULT));

        // Zoom handling (Mouse Scroll)
        viewport.setOnScroll(e -> {
            e.consume();
            double zoomFactor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            doZoom(zoomFactor, e.getX(), e.getY());
        });

        // ── Controls Bar (simplified to Zoom, Back, Code editor controls) ──
        Button zoomInBtn = styledButton("🔍+", "#8e44ad");
        Button zoomOutBtn = styledButton("🔍-", "#8e44ad");
        zoomInBtn.setOnAction(e -> doZoom(1.2, viewport.getWidth()/2, viewport.getHeight()/2));
        zoomOutBtn.setOnAction(e -> doZoom(0.8, viewport.getWidth()/2, viewport.getHeight()/2));

        toggleCodeBtn = styledButton("◀ Code", "#e67e22");
        toggleCodeBtn.setOnAction(e -> toggleCodePanel());

        Button backBtn = styledButton("← Back", "#555555");
        backBtn.setOnAction(e -> App.changeScene(new DashboardView().getView()));

        HBox controls = new HBox(10, backBtn);
        controls.getChildren().addAll(zoomInBtn, zoomOutBtn, new Separator(Orientation.VERTICAL), toggleCodeBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(12));
        controls.setStyle("-fx-background-color: #0c1236;");

        // ── Status Bar ─────────────────────────────────────────
        statusLabel = new Label("Ready — enter a value or write Java code in the panel. Scroll to zoom, drag to pan.");
        statusLabel.setTextFill(Color.web("#a0b4ff"));
        statusLabel.setFont(Font.font("Consolas", 13));
        statusLabel.setPadding(new Insets(6, 14, 6, 14));
        statusLabel.setStyle("-fx-background-color: #0c1236;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        // ── Title ──────────────────────────────────────────────
        Label title = new Label("🌳  Binary Search Tree Visualizer");
        title.setTextFill(Color.web("#4d80ff"));
        title.setFont(Font.font("Arial", 22));
        title.setPadding(new Insets(10));
        title.setStyle("-fx-background-color: #0c1236; -fx-font-weight: bold;");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        // ── Code Panel ─────────────────────────────────────────
        codePanel = buildCodePanel();

        HBox centerArea;
        if (!hideManualControls) {
            ScrollPane sidebar = buildSidebar();
            centerArea = new HBox(sidebar, viewport, codePanel);
        } else {
            centerArea = new HBox(viewport, codePanel);
        }
        HBox.setHgrow(viewport, Priority.ALWAYS);

        VBox layout = new VBox(title, controls, centerArea, statusLabel);
        VBox.setVgrow(centerArea, Priority.ALWAYS);

        // Initial centering of the virtual canvas (X=2000 is center of our virtual space)
        Platform.runLater(() -> {
            translateTransform.setX(viewport.getWidth() / 2 - 2000);
            translateTransform.setY(20);
        });

        return layout;
    }

    // ═══════════════════════════════════════════════════════════
    //  Left Sidebar UI & Functionality
    // ═══════════════════════════════════════════════════════════

    private VBox createSidebarSection(String titleStr, Pane content) {
        Label title = new Label(titleStr);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#8da2fb"));
        
        VBox section = new VBox(8, title, content);
        section.setStyle(
            "-fx-background-color: #121e3d;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12;" +
            "-fx-border-color: #1e2c56;" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 1;"
        );
        return section;
    }

    private Button createSidebarButton(String text, String bgColor, String textColor) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-text-fill: " + textColor + ";" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 6 12;" +
            "-fx-background-radius: 4;" +
            "-fx-border-color: transparent;" +
            "-fx-border-radius: 4;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private Button createSidebarOutlineButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: #121e3d;" +
            "-fx-text-fill: #a0b4ff;" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 6 12;" +
            "-fx-background-radius: 4;" +
            "-fx-border-color: #2c3b6b;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 4;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                "-fx-background-color: #1a2c5c;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 4;" +
                "-fx-border-color: #4d80ff;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 4;" +
                "-fx-cursor: hand;"
            );
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(
                "-fx-background-color: #121e3d;" +
                "-fx-text-fill: #a0b4ff;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 4;" +
                "-fx-border-color: #2c3b6b;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 4;" +
                "-fx-cursor: hand;"
            );
        });
        return btn;
    }

    private void setTreeModeActive(Button activeBtn, Button[] otherBtns) {
        activeBtn.setStyle(
            "-fx-background-color: #0df2c9;" +
            "-fx-text-fill: #0a0f2e;" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 6 12;" +
            "-fx-background-radius: 4;" +
            "-fx-border-color: transparent;" +
            "-fx-cursor: hand;"
        );
        activeBtn.setOnMouseEntered(null);
        activeBtn.setOnMouseExited(null);

        for (Button btn : otherBtns) {
            btn.setStyle(
                "-fx-background-color: #121e3d;" +
                "-fx-text-fill: #a0b4ff;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 12;" +
                "-fx-background-radius: 4;" +
                "-fx-border-color: #2c3b6b;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 4;" +
                "-fx-cursor: hand;"
            );
            btn.setOnMouseEntered(e -> {
                btn.setStyle(
                    "-fx-background-color: #1a2c5c;" +
                    "-fx-text-fill: #ffffff;" +
                    "-fx-font-family: 'Segoe UI';" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 6 12;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: #4d80ff;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;"
                );
            });
            btn.setOnMouseExited(e -> {
                btn.setStyle(
                    "-fx-background-color: #121e3d;" +
                    "-fx-text-fill: #a0b4ff;" +
                    "-fx-font-family: 'Segoe UI';" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 6 12;" +
                    "-fx-background-radius: 4;" +
                    "-fx-border-color: #2c3b6b;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 4;" +
                    "-fx-cursor: hand;"
                );
            });
        }
    }

    private ScrollPane buildSidebar() {
        VBox sidebar = new VBox(14);
        sidebar.setPadding(new Insets(14));
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(260);
        sidebar.setStyle("-fx-background-color: #0c1236;");

        // 1. TREE MODE
        HBox treeModeContent = new HBox(4);
        Button bstBtn = createSidebarButton("BST", accentColorHex, "#0a0f2e");
        Button avlBtn = createSidebarOutlineButton("AVL");
        Button rbBtn = createSidebarOutlineButton("Red-Black");
        Button btreeBtn = createSidebarOutlineButton("B-Tree");
        
        treeModeButtons = new Button[]{bstBtn, avlBtn, rbBtn, btreeBtn};
        activeModeButton = bstBtn;
        
        bstBtn.setOnAction(e -> {
            setTreeModeActive(bstBtn, new Button[]{avlBtn, rbBtn, btreeBtn});
            status("Tree Mode: Binary Search Tree (BST)");
        });
        avlBtn.setOnAction(e -> {
            setTreeModeActive(avlBtn, new Button[]{bstBtn, rbBtn, btreeBtn});
            status("AVL Tree Mode is currently under development. Showing BST simulation.");
        });
        rbBtn.setOnAction(e -> {
            setTreeModeActive(rbBtn, new Button[]{bstBtn, avlBtn, btreeBtn});
            status("Red-Black Tree Mode is currently under development. Showing BST simulation.");
        });
        btreeBtn.setOnAction(e -> {
            setTreeModeActive(btreeBtn, new Button[]{bstBtn, avlBtn, rbBtn});
            status("B-Tree Mode is currently under development. Showing BST simulation.");
        });
        
        treeModeContent.getChildren().addAll(bstBtn, avlBtn, rbBtn, btreeBtn);
        VBox treeModeSection = createSidebarSection("TREE MODE", treeModeContent);

        // 2. OPERATIONS
        VBox operationsContent = new VBox(8);
        TextField valueField = new TextField();
        valueField.setPromptText("Value (1-999)");
        valueField.setStyle(sidebarFieldStyle());
        valueField.setPrefWidth(120);
        HBox.setHgrow(valueField, Priority.ALWAYS);
        
        Button insertBtn = createSidebarButton("Insert", "#10b981", "#ffffff");
        HBox row1 = new HBox(6, valueField, insertBtn);
        
        Button deleteBtn = createSidebarButton("Delete", "#ef4444", "#ffffff");
        deleteBtn.setPrefWidth(100);
        Button searchBtn = createSidebarButton("Search", "#3b82f6", "#ffffff");
        searchBtn.setPrefWidth(100);
        HBox row2 = new HBox(6, deleteBtn, searchBtn);
        row2.setAlignment(Pos.CENTER);
        
        Button randomBtn = createSidebarOutlineButton("Random");
        randomBtn.setPrefWidth(100);
        Button clearBtn = createSidebarOutlineButton("Clear");
        clearBtn.setPrefWidth(100);
        HBox row3 = new HBox(6, randomBtn, clearBtn);
        row3.setAlignment(Pos.CENTER);
        
        insertBtn.setOnAction(e -> handleInsert(valueField));
        deleteBtn.setOnAction(e -> handleDelete(valueField));
        searchBtn.setOnAction(e -> handleSearch(valueField));
        randomBtn.setOnAction(e -> handleRandom());
        clearBtn.setOnAction(e -> handleClear());
        valueField.setOnAction(e -> handleInsert(valueField));
        
        operationsContent.getChildren().addAll(row1, row2, row3);
        VBox operationsSection = createSidebarSection("OPERATIONS", operationsContent);

        // 3. BATCH INSERT
        TextField batchField = new TextField();
        batchField.setPromptText("e.g. 50,30,70,20,40");
        batchField.setStyle(sidebarFieldStyle());
        HBox.setHgrow(batchField, Priority.ALWAYS);
        Button goBtn = createSidebarButton("Go", "#10b981", "#ffffff");
        HBox batchContent = new HBox(6, batchField, goBtn);
        goBtn.setOnAction(e -> {
            handleBatchInsert(batchField.getText());
            batchField.clear();
        });
        batchField.setOnAction(e -> {
            handleBatchInsert(batchField.getText());
            batchField.clear();
        });
        VBox batchSection = createSidebarSection("BATCH INSERT", batchContent);

        // 4. PRESETS
        VBox presetsContent = new VBox(6);
        Button balancedBtn = createSidebarOutlineButton("Balanced");
        Button skewedBtn = createSidebarOutlineButton("Skewed");
        Button rand10Btn = createSidebarOutlineButton("Random 10");
        HBox pRow1 = new HBox(4, balancedBtn, skewedBtn, rand10Btn);
        
        Button rand20Btn = createSidebarOutlineButton("Random 20");
        Button sortedBtn = createSidebarOutlineButton("Sorted (Worst)");
        Button zigzagBtn = createSidebarOutlineButton("Zig-Zag");
        HBox pRow2 = new HBox(4, rand20Btn, sortedBtn, zigzagBtn);
        
        Button completeBtn = createSidebarOutlineButton("Complete");
        Button fibBtn = createSidebarOutlineButton("Fibonacci");
        HBox pRow3 = new HBox(4, completeBtn, fibBtn);
        
        balancedBtn.setOnAction(e -> loadPresetBalanced());
        skewedBtn.setOnAction(e -> loadPresetSkewed());
        rand10Btn.setOnAction(e -> loadPresetRandom(10));
        rand20Btn.setOnAction(e -> loadPresetRandom(20));
        sortedBtn.setOnAction(e -> loadPresetSorted());
        zigzagBtn.setOnAction(e -> loadPresetZigZag());
        completeBtn.setOnAction(e -> loadPresetComplete());
        fibBtn.setOnAction(e -> loadPresetFibonacci());
        
        presetsContent.getChildren().addAll(pRow1, pRow2, pRow3);
        VBox presetsSection = createSidebarSection("PRESETS", presetsContent);

        // 5. TRAVERSALS
        VBox traversalsContent = new VBox(8);
        Button inorderBtn = createSidebarOutlineButton("In-Order");
        Button preorderBtn = createSidebarOutlineButton("Pre-Order");
        Button postorderBtn = createSidebarOutlineButton("Post-Order");
        HBox tRow1 = new HBox(4, inorderBtn, preorderBtn, postorderBtn);
        
        Button levelorderBtn = createSidebarOutlineButton("Level-Order");
        HBox tRow2 = new HBox(4, levelorderBtn);
        
        Label traversalResult = new Label();
        traversalResult.setTextFill(Color.web("#a0b4ff"));
        traversalResult.setFont(Font.font("Consolas", 12));
        traversalResult.setWrapText(true);
        
        VBox resultBox = new VBox(traversalResult);
        resultBox.setMinHeight(32);
        resultBox.setPadding(new Insets(6));
        resultBox.setStyle("-fx-background-color: #0a0f2e; -fx-background-radius: 4;");
        
        inorderBtn.setOnAction(e -> {
            List<TreeNode> path = new ArrayList<>();
            inOrder(root, path);
            animateTraversal(path, traversalResult);
        });
        preorderBtn.setOnAction(e -> {
            List<TreeNode> path = new ArrayList<>();
            preOrder(root, path);
            animateTraversal(path, traversalResult);
        });
        postorderBtn.setOnAction(e -> {
            List<TreeNode> path = new ArrayList<>();
            postOrder(root, path);
            animateTraversal(path, traversalResult);
        });
        levelorderBtn.setOnAction(e -> {
            List<TreeNode> path = new ArrayList<>();
            levelOrder(root, path);
            animateTraversal(path, traversalResult);
        });
        
        traversalsContent.getChildren().addAll(tRow1, tRow2, resultBox);
        VBox traversalsSection = createSidebarSection("TRAVERSALS", traversalsContent);

        // 6. THEME ACCENT
        HBox accentContent = new HBox(12);
        accentContent.setAlignment(Pos.CENTER);
        Button swatchTeal = createColorSwatch("#0df2c9");
        Button swatchViolet = createColorSwatch("#9b5de5");
        Button swatchCoral = createColorSwatch("#ff6b6b");
        Button swatchGold = createColorSwatch("#f1c40f");
        
        swatchTeal.setOnAction(e -> updateAccentTheme("#0df2c9"));
        swatchViolet.setOnAction(e -> updateAccentTheme("#9b5de5"));
        swatchCoral.setOnAction(e -> updateAccentTheme("#ff6b6b"));
        swatchGold.setOnAction(e -> updateAccentTheme("#f1c40f"));
        
        accentContent.getChildren().addAll(swatchTeal, swatchViolet, swatchCoral, swatchGold);
        VBox accentSection = createSidebarSection("THEME ACCENT", accentContent);

        // 7. ANIMATION SPEED
        HBox speedContent = new HBox(10);
        speedContent.setAlignment(Pos.CENTER_LEFT);
        speedSlider = new Slider(1, 10, 4);
        speedSlider.setBlockIncrement(1);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setSnapToTicks(true);
        HBox.setHgrow(speedSlider, Priority.ALWAYS);
        
        speedSlider.setStyle(
            ".slider .track { -fx-background-color: #1e2c56; -fx-background-insets: 0; -fx-background-radius: 4; }" +
            ".slider .thumb { -fx-background-color: " + accentColorHex + "; -fx-background-radius: 10; }"
        );

        Label speedValueLabel = new Label("4");
        speedValueLabel.setTextFill(Color.web("#0df2c9"));
        speedValueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        speedValueLabel.setPrefWidth(20);
        speedValueLabel.setAlignment(Pos.CENTER_RIGHT);

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int speed = newVal.intValue();
            animationSpeed = speed;
            speedValueLabel.setText(String.valueOf(speed));
            speedValueLabel.setTextFill(Color.web(accentColorHex));
        });

        speedContent.getChildren().addAll(speedSlider, speedValueLabel);
        VBox speedSection = createSidebarSection("ANIMATION SPEED", speedContent);

        sidebar.getChildren().addAll(treeModeSection, operationsSection, batchSection, presetsSection, traversalsSection, accentSection, speedSection);
        
        ScrollPane sp = new ScrollPane(sidebar);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background: #0c1236; -fx-background-color: #0c1236; -fx-border-color: #1e2c56; -fx-border-width: 0 1 0 0;");
        sp.setMinWidth(290);
        sp.setPrefWidth(290);
        return sp;
    }

    private Button createColorSwatch(String colorHex) {
        Button btn = new Button();
        btn.setPrefSize(24, 24);
        btn.setMinSize(24, 24);
        btn.setMaxSize(24, 24);
        btn.setStyle(
            "-fx-background-color: " + colorHex + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #2c3b6b;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: " + colorHex + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #ffffff;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 12;" +
            "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: " + colorHex + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #2c3b6b;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-cursor: hand;"
        ));
        return btn;
    }

    private void updateAccentTheme(String hexColor) {
        accentColorHex = hexColor;
        
        // Re-apply style to active tree mode button
        if (activeModeButton != null) {
            Button[] others = java.util.Arrays.stream(treeModeButtons)
                .filter(b -> b != activeModeButton)
                .toArray(Button[]::new);
            setTreeModeActive(activeModeButton, others);
        }
        
        // Update speed slider accent color if it exists
        if (speedSlider != null) {
            speedSlider.setStyle(
                ".slider .track { -fx-background-color: #1e2c56; -fx-background-insets: 0; -fx-background-radius: 4; }" +
                ".slider .thumb { -fx-background-color: " + hexColor + "; -fx-background-radius: 10; }"
            );
        }
        
        // Refresh all tree nodes immediately to use the new base color
        resetColors();
    }

    private String sidebarFieldStyle() {
        return "-fx-background-color: #0a0f2e;" +
               "-fx-text-fill: white;" +
               "-fx-prompt-text-fill: #4d5c8a;" +
               "-fx-border-color: #1e2c56;" +
               "-fx-border-radius: 4;" +
               "-fx-background-radius: 4;" +
               "-fx-padding: 6;" +
               "-fx-font-size: 12px;";
    }

    private void handleBatchInsert(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String[] parts = text.split(",");
        List<Integer> vals = new ArrayList<>();
        for (String p : parts) {
            try {
                vals.add(Integer.parseInt(p.trim()));
            } catch (NumberFormatException e) {
                // ignore invalid numbers
            }
        }
        if (vals.isEmpty()) return;
        insertSequence(vals, 0);
    }

    private void insertSequence(List<Integer> vals, int index) {
        if (index >= vals.size()) {
            status("Batch insertion complete");
            return;
        }
        int val = vals.get(index);
        insert(val);
        highlightNode(val, Color.web("#10b981"), 800);
        
        PauseTransition pause = new PauseTransition(Duration.millis(getAnimMs() + 100));
        pause.setOnFinished(e -> insertSequence(vals, index + 1));
        pause.play();
    }

    private double getAnimMs() {
        return 1400.0 / animationSpeed;
    }

    private double getExecDelay() {
        return 2800.0 / animationSpeed;
    }

    private void loadPresetBalanced() {
        handleClear();
        int[] vals = {40, 20, 60, 10, 30, 50, 70};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0);
    }

    private void loadPresetSkewed() {
        handleClear();
        int[] vals = {10, 20, 30, 40, 50, 60};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0);
    }

    private void loadPresetRandom(int count) {
        handleClear();
        java.util.Random rng = new java.util.Random();
        java.util.Set<Integer> used = new java.util.HashSet<>();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int val;
            do { val = rng.nextInt(99) + 1; } while (used.contains(val));
            used.add(val);
            list.add(val);
        }
        insertSequence(list, 0);
    }

    private void loadPresetSorted() {
        handleClear();
        int[] vals = {10, 20, 30, 40, 50, 60, 70};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0);
    }

    private void loadPresetZigZag() {
        handleClear();
        int[] vals = {50, 20, 40, 30, 35};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0);
    }

    private void loadPresetComplete() {
        handleClear();
        int[] vals = {50, 25, 75, 12, 37, 62, 87, 6, 18, 31, 43};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0);
    }

    private void loadPresetFibonacci() {
        handleClear();
        int[] vals = {13, 5, 34, 2, 8, 21, 55, 1, 3, 89};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0);
    }

    private void inOrder(TreeNode node, List<TreeNode> list) {
        if (node == null) return;
        inOrder(node.left, list);
        list.add(node);
        inOrder(node.right, list);
    }

    private void preOrder(TreeNode node, List<TreeNode> list) {
        if (node == null) return;
        list.add(node);
        preOrder(node.left, list);
        preOrder(node.right, list);
    }

    private void postOrder(TreeNode node, List<TreeNode> list) {
        if (node == null) return;
        postOrder(node.left, list);
        postOrder(node.right, list);
        list.add(node);
    }

    private void levelOrder(TreeNode node, List<TreeNode> list) {
        if (node == null) return;
        List<TreeNode> queue = new ArrayList<>();
        queue.add(node);
        while (!queue.isEmpty()) {
            TreeNode curr = queue.remove(0);
            list.add(curr);
            if (curr.left != null) queue.add(curr.left);
            if (curr.right != null) queue.add(curr.right);
        }
    }

    private void animateTraversal(List<TreeNode> path, Label resultLabel) {
        if (path.isEmpty()) {
            resultLabel.setText("Tree is empty");
            return;
        }
        resetColors();
        resetScales(root);
        
        double stepMs = 2400.0 / animationSpeed;
        Timeline tl = new Timeline();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < path.size(); i++) {
            TreeNode n = path.get(i);
            int idx = i;
            tl.getKeyFrames().add(new KeyFrame(
                Duration.millis(stepMs * i),
                e -> {
                    n.circle.setFill(Color.web("#9b5de5"));
                    n.circle.setScaleX(1.2);
                    n.circle.setScaleY(1.2);
                    if (idx > 0) {
                        TreeNode prev = path.get(idx - 1);
                        prev.circle.setFill(Color.web("#2e7dd6"));
                        prev.circle.setScaleX(1.0);
                        prev.circle.setScaleY(1.0);
                    }
                    if (idx == 0) {
                        sb.append(n.value);
                    } else {
                        sb.append(", ").append(n.value);
                    }
                    resultLabel.setText(sb.toString());
                }
            ));
        }
        
        tl.getKeyFrames().add(new KeyFrame(
            Duration.millis(stepMs * path.size()),
            e -> {
                if (!path.isEmpty()) {
                    TreeNode last = path.get(path.size() - 1);
                    last.circle.setFill(Color.web("#2e7dd6"));
                    last.circle.setScaleX(1.0);
                    last.circle.setScaleY(1.0);
                }
            }
        ));
        tl.play();
    }

    private void doZoom(double factor, double pivotX, double pivotY) {
        double oldScale = scaleTransform.getX();
        double newScale = oldScale * factor;
        if (newScale < 0.1) newScale = 0.1;
        if (newScale > 5.0) newScale = 5.0;

        double f = newScale / oldScale;
        double dx = pivotX - translateTransform.getX();
        double dy = pivotY - translateTransform.getY();

        translateTransform.setX(pivotX - dx * f);
        translateTransform.setY(pivotY - dy * f);

        scaleTransform.setX(newScale);
        scaleTransform.setY(newScale);
    }

    // ═══════════════════════════════════════════════════════════
    //  Code Panel
    // ═══════════════════════════════════════════════════════════

    public String getCode() {
        return codeArea == null ? "" : codeArea.getText();
    }

    public void setCode(String code) {
        if (codeArea != null) codeArea.setText(code);
    }

    public boolean hasNode(int value) {
        return find(root, value) != null;
    }

    public String getOutputLog() {
        return outputArea == null ? "" : outputArea.getText();
    }

    private VBox buildCodePanel() {
        Label panelTitle = new Label("📝 Java Code Editor");
        panelTitle.setTextFill(Color.web("#e94560"));
        panelTitle.setFont(Font.font("Segoe UI", 16));
        panelTitle.setStyle("-fx-font-weight: bold;");
        panelTitle.setPadding(new Insets(0, 0, 2, 0));

        Label hint = new Label("Write Java code to build and manipulate the BST");
        hint.setTextFill(Color.web("#7f8c8d"));
        hint.setFont(Font.font("Consolas", 11));
        hint.setWrapText(true);

        codeArea = new TextArea();
        codeArea.setFont(Font.font("Consolas", 13));
        codeArea.setStyle(
                "-fx-control-inner-background: #0a0a1a;" +
                "-fx-text-fill: #00ff88;" +
                "-fx-highlight-fill: #2e7dd6;" +
                "-fx-highlight-text-fill: white;" +
                "-fx-border-color: #4a6fa5;" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;" +
                "-fx-font-family: 'Consolas';"
        );
        codeArea.setPrefRowCount(20);
        codeArea.setWrapText(false);
        VBox.setVgrow(codeArea, Priority.ALWAYS);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setFont(Font.font("Consolas", 12));
        outputArea.setStyle(
                "-fx-control-inner-background: #0d0d1a;" +
                "-fx-text-fill: #bdc3c7;" +
                "-fx-border-color: #333;" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;" +
                "-fx-font-family: 'Consolas';"
        );
        outputArea.setPrefRowCount(12);
        outputArea.setPromptText("// Console output...");
        VBox.setVgrow(outputArea, Priority.SOMETIMES);

        Label outputLabel = new Label("▸ Console Output");
        outputLabel.setTextFill(Color.web("#e94560"));
        outputLabel.setFont(Font.font("Consolas", 12));
        outputLabel.setStyle("-fx-font-weight: bold;");
        outputLabel.setPadding(new Insets(6, 0, 2, 0));

        Button runBtn = styledButton("▶ Run All", "#27ae60");
        runBtn.setPrefWidth(120);
        runBtn.setOnAction(e -> runCode());

        Button stepBtn = styledButton("⏭ Step", "#2980b9");
        stepBtn.setPrefWidth(100);
        stepBtn.setOnAction(e -> stepNextLine());

        Button resetBtn = styledButton("↺ Reset", "#7f8c8d");
        resetBtn.setPrefWidth(100);
        resetBtn.setOnAction(e -> resetExecution());

        Button exampleBtn = styledButton("📋 Example", "#8e44ad");
        exampleBtn.setPrefWidth(120);
        exampleBtn.setOnAction(e -> loadExample());

        HBox btnRow = new HBox(6, runBtn, stepBtn, resetBtn, exampleBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(6, 0, 0, 0));

        VBox panel = new VBox(6, panelTitle, hint, codeArea, btnRow, outputLabel, outputArea);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #080d28; -fx-border-color: #0048ff; -fx-border-width: 0 0 0 2;");
        panel.setPrefWidth(440);
        panel.setMinWidth(440);

        loadExample();

        return panel;
    }

    private void toggleCodePanel() {
        codePanelVisible = !codePanelVisible;
        if (codePanelVisible) {
            codePanel.setPrefWidth(440);
            codePanel.setMinWidth(440);
            codePanel.setVisible(true);
            codePanel.setManaged(true);
            toggleCodeBtn.setText("◀ Code");
        } else {
            codePanel.setPrefWidth(0);
            codePanel.setMinWidth(0);
            codePanel.setVisible(false);
            codePanel.setManaged(false);
            toggleCodeBtn.setText("▶ Code");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Code Execution Engine
    // ═══════════════════════════════════════════════════════════

    private void runCode() {
        String code = codeArea.getText();
        if (code == null || code.trim().isEmpty()) {
            status("No code to run");
            return;
        }

        String[] lines = code.split("\\n");
        outputArea.clear();
        declaredArrays.clear();
        executingLine = 0;
        outputArea.appendText("╔══════════════════════════════════════╗\n");
        outputArea.appendText("║       ▶  Program Execution          ║\n");
        outputArea.appendText("╚══════════════════════════════════════╝\n\n");

        executeLineSequence(lines, 0);
    }

    private void executeLineSequence(String[] lines, int index) {
        if (index >= lines.length) {
            outputArea.appendText("\n─────────────────────────────────────\n");
            outputArea.appendText("✅ BUILD SUCCESSFUL\n");
            outputArea.appendText("Total lines executed: " + countExecutable(lines) + "\n");
            executingLine = -1;
            status("Program execution complete");
            return;
        }

        String line = lines[index].trim();
        executingLine = index;

        if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*")
                || line.startsWith("*") || line.equals("{") || line.equals("}")) {
            if (!line.isEmpty()) {
                outputArea.appendText("      " + padLineNum(index + 1) + " │ " + line + "\n");
            }
            PauseTransition pause = new PauseTransition(Duration.millis(100));
            pause.setOnFinished(e -> executeLineSequence(lines, index + 1));
            pause.play();
            return;
        }

        outputArea.appendText("  ►   " + padLineNum(index + 1) + " │ " + line + "\n");

        executeSingleCommand(line, (result) -> {
            if (result != null) {
                outputArea.appendText("      " + padLineNum("") + " └─➤ " + result + "\n");
            }
            outputArea.positionCaret(outputArea.getLength());
            
            // Wait a small buffer delay after the command finishes animating
            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(e -> executeLineSequence(lines, index + 1));
            pause.play();
        });
    }

    private void stepNextLine() {
        String code = codeArea.getText();
        if (code == null || code.trim().isEmpty()) {
            status("No code to step through");
            return;
        }

        String[] lines = code.split("\\n");

        if (executingLine < 0) {
            executingLine = 0;
            declaredArrays.clear();
            outputArea.clear();
            outputArea.appendText("╔══════════════════════════════════════╗\n");
            outputArea.appendText("║     ⏭  Step-by-Step Debugging       ║\n");
            outputArea.appendText("╚══════════════════════════════════════╝\n\n");
        }

        while (executingLine < lines.length) {
            String line = lines[executingLine].trim();
            if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("/*")
                    && !line.startsWith("*") && !line.equals("{") && !line.equals("}")) break;
            if (!line.isEmpty()) {
                outputArea.appendText("      " + padLineNum(executingLine + 1) + " │ " + line + "\n");
            }
            executingLine++;
        }

        if (executingLine >= lines.length) {
            outputArea.appendText("\n─────────────────────────────────────\n");
            outputArea.appendText("✅ All lines executed.\n");
            executingLine = -1;
            status("Step execution complete");
            return;
        }

        String line = lines[executingLine].trim();
        outputArea.appendText("  ►   " + padLineNum(executingLine + 1) + " │ " + line + "\n");

        executeSingleCommand(line, (result) -> {
            if (result != null) {
                outputArea.appendText("      " + padLineNum("") + " └─➤ " + result + "\n");
            }
            outputArea.positionCaret(outputArea.getLength());
        });

        executingLine++;
        if (executingLine >= lines.length) {
            outputArea.appendText("\n─────────────────────────────────────\n");
            outputArea.appendText("✅ All lines executed.\n");
            executingLine = -1;
        }
    }

    private void resetExecution() {
        executingLine = -1;
        declaredArrays.clear();
        outputArea.clear();
        outputArea.appendText("↺ Execution reset. Ready to run.\n");
        resetColors();
        resetScales(root);
        status("Execution reset");
    }

    private void executeSingleCommand(String line, java.util.function.Consumer<String> callback) {
        Matcher m = METHOD_CALL.matcher(line);
        if (m.matches()) {
            String cmd = m.group(1).toLowerCase();
            String arg = m.group(2);
            executeCommandAsync(cmd, arg, callback);
            return;
        }

        Matcher c = CONSTRUCTOR.matcher(line);
        if (c.matches()) {
            handleClear();
            callback.accept("BinarySearchTree initialized");
            return;
        }

        Matcher sout = SYSOUT.matcher(line);
        if (sout.matches()) {
            String content = sout.group(1).trim();
            if (content.startsWith("\"") && content.endsWith("\"")) {
                content = content.substring(1, content.length() - 1);
            }
            callback.accept(">> " + content);
            return;
        }

        Matcher arr = ARRAY_DECL.matcher(line);
        if (arr.matches()) {
            String valuesStr = arr.group(1).trim();
            String varName = line.trim().split("\\s+")[1].replace("[]", "");
            Matcher nameMatch = Pattern.compile("int\\[\\]\\s+(\\w+)").matcher(line);
            if (nameMatch.find()) varName = nameMatch.group(1);

            String[] parts = valuesStr.split(",");
            int[] vals = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                vals[i] = Integer.parseInt(parts[i].trim());
            }
            declaredArrays.put(varName, vals);
            callback.accept("Declared array " + varName + " with " + vals.length + " elements");
            return;
        }

        Matcher forEach = FOR_EACH.matcher(line);
        if (forEach.matches()) {
            String arrayName = forEach.group(1);
            String cmd = forEach.group(2).toLowerCase();
            int[] vals = declaredArrays.get(arrayName);
            if (vals == null) {
                callback.accept("❌ Array '" + arrayName + "' not found");
                return;
            }
            List<Integer> list = new ArrayList<>();
            for (int val : vals) list.add(val);
            executeBatchCommands(cmd, list, 0, new StringBuilder(), callback);
            return;
        }

        if (line.startsWith("import ") || line.startsWith("package ") || line.startsWith("public class")
                || line.startsWith("class ") || line.contains("public static void main")
                || line.startsWith("public ") || line.startsWith("private ")
                || line.startsWith("@")) {
            callback.accept("(declaration acknowledged)");
            return;
        }

        if (line.startsWith("int ") || line.startsWith("String ") || line.startsWith("var ")) {
            callback.accept("(variable declared)");
            return;
        }

        status("Unrecognized: " + line);
        callback.accept("⚠ Unrecognized syntax — use tree.insert(n), tree.delete(n), tree.search(n), tree.clear()");
    }

    private void executeBatchCommands(String cmd, List<Integer> vals, int index, StringBuilder sb, java.util.function.Consumer<String> callback) {
        if (index >= vals.size()) {
            callback.accept(sb.toString().trim());
            return;
        }
        int val = vals.get(index);
        executeCommandAsync(cmd, String.valueOf(val), (res) -> {
            sb.append(res).append("; ");
            executeBatchCommands(cmd, vals, index + 1, sb, callback);
        });
    }

    private void executeCommandAsync(String cmd, String arg, java.util.function.Consumer<String> callback) {
        switch (cmd) {
            case "insert": {
                if (arg.isEmpty()) { callback.accept("❌ insert requires a number"); return; }
                int val = Integer.parseInt(arg);
                insert(val, () -> callback.accept("Inserted node " + val));
                break;
            }
            case "delete": {
                if (arg.isEmpty()) { callback.accept("❌ delete requires a number"); return; }
                int val = Integer.parseInt(arg);
                TreeNode target = find(root, val);
                if (target != null) {
                    delete(val, () -> callback.accept("Deleted node " + val));
                } else {
                    callback.accept("❌ Node " + val + " not found in tree");
                }
                break;
            }
            case "search": {
                if (arg.isEmpty()) { callback.accept("❌ search requires a number"); return; }
                int val = Integer.parseInt(arg);
                search(val, (found) -> callback.accept(found ? "✔ Found node " + val : "✘ Node " + val + " not found"));
                break;
            }
            case "clear": {
                handleClear();
                callback.accept("Tree cleared");
                break;
            }
            default:
                callback.accept("❌ Unknown method: " + cmd);
        }
    }

    private void loadExample() {
        codeArea.setText(
                "BinarySearchTree tree = new BinarySearchTree();\n" +
                "\n" +
                "int[] nums = {40, 20, 60, 10, 30, 50, 70};\n" +
                "for (int x : nums) { tree.insert(x); }\n" +
                "\n" +
                "tree.search(30);\n" +
                "tree.search(50);\n"
        );
        outputArea.clear();
        executingLine = -1;
        status("Example Java code loaded — click ▶ Run All or ⏭ Step");
    }

    // ═══════════════════════════════════════════════════════════
    //  Node Highlighting & Path Sliding Animation
    // ═══════════════════════════════════════════════════════════

    private void animatePointerPath(List<TreeNode> path, double stepMs, Color finalColor, Runnable onFinished) {
        if (path == null || path.isEmpty()) {
            if (onFinished != null) onFinished.run();
            return;
        }

        resetColors();
        resetScales(root);

        // Spawn a glowing pointer circle on the canvas
        javafx.scene.shape.Circle pointer = new javafx.scene.shape.Circle(12);
        pointer.setFill(Color.web("#f1c40f")); // Glowing gold
        Glow glow = new Glow(0.9);
        DropShadow ds = new DropShadow(20, Color.web("#f1c40f"));
        glow.setInput(ds);
        pointer.setEffect(glow);

        // Position initially at the root node's center
        TreeNode startNode = path.get(0);
        pointer.setCenterX(startNode.circle.getCenterX());
        pointer.setCenterY(startNode.circle.getCenterY());
        canvas.getChildren().add(pointer);

        Timeline tl = new Timeline();

        // Animate node-to-node
        for (int i = 0; i < path.size(); i++) {
            TreeNode node = path.get(i);
            final int idx = i;
            
            // KeyFrame at start of each node visit
            tl.getKeyFrames().add(new KeyFrame(
                Duration.millis(stepMs * i),
                e -> {
                    // Highlight node as pointer reaches it
                    boolean isLast = (idx == path.size() - 1);
                    Color col = isLast ? finalColor : Color.web("#f39c12");
                    node.circle.setFill(col);
                    if (isLast) {
                        node.circle.setScaleX(1.3);
                        node.circle.setScaleY(1.3);
                    }
                }
            ));

            // Animate translation from previous node to current node
            if (i > 0) {
                TreeNode prevNode = path.get(i - 1);
                tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(stepMs * i),
                    new KeyValue(pointer.centerXProperty(), node.circle.getCenterX()),
                    new KeyValue(pointer.centerYProperty(), node.circle.getCenterY())
                ));
            }
        }

        // Cleanup and finish callback
        tl.getKeyFrames().add(new KeyFrame(
            Duration.millis(stepMs * path.size()),
            e -> {
                canvas.getChildren().remove(pointer);
                if (onFinished != null) onFinished.run();
            }
        ));

        // Optional reset color timer at the very end
        tl.getKeyFrames().add(new KeyFrame(
            Duration.millis(stepMs * path.size() + stepMs * 2),
            e -> {
                resetColors();
                resetScales(root);
            }
        ));

        tl.play();
    }

    private void highlightNode(int value, Color color, double durationMs) {
        TreeNode node = find(root, value);
        if (node == null) return;
        highlightNodeDirect(node, color, durationMs);
    }

    private void highlightNodeDirect(TreeNode node, Color color, double durationMs) {
        Color originalColor = Color.web(accentColorHex);
        node.circle.setFill(color);

        Glow glow = new Glow(0.8);
        DropShadow glowShadow = new DropShadow(20, color);
        glow.setInput(glowShadow);
        node.circle.setEffect(glow);
        node.circle.setScaleX(1.3);
        node.circle.setScaleY(1.3);
        node.label.setScaleX(1.2);
        node.label.setScaleY(1.2);

        PauseTransition reset = new PauseTransition(Duration.millis(durationMs));
        reset.setOnFinished(e -> {
            node.circle.setFill(originalColor);
            node.circle.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.4)));
            node.circle.setScaleX(1.0);
            node.circle.setScaleY(1.0);
            node.label.setScaleX(1.0);
            node.label.setScaleY(1.0);
        });
        reset.play();
    }

    // ═══════════════════════════════════════════════════════════
    //  BST Operations
    // ═══════════════════════════════════════════════════════════

    private void insert(int value) {
        insert(value, null);
    }

    private void insert(int value, Runnable onFinished) {
        if (root == null) {
            root = new TreeNode(value);
            // Start the root at X=2000 (center of our virtual 4000x4000 space)
            root.placeAt(2000, ROOT_Y);
            addNodeToCanvas(root);
            status("Inserted root: " + value);
            if (onFinished != null) onFinished.run();
            return;
        }

        // Trace path to parent
        List<TreeNode> path = new ArrayList<>();
        TreeNode cur = root;
        TreeNode parent = null;
        while (cur != null) {
            path.add(cur);
            if (value == cur.value) {
                status("Duplicate value " + value + " — not inserted");
                if (onFinished != null) onFinished.run();
                return;
            }
            parent = cur;
            cur = (value < cur.value) ? cur.left : cur.right;
        }

        double stepMs = 2400.0 / animationSpeed;
        TreeNode finalParent = parent;

        animatePointerPath(path, stepMs, Color.web("#f39c12"), () -> {
            TreeNode newNode = new TreeNode(value);
            newNode.parent = finalParent;
            if (value < finalParent.value) {
                finalParent.left = newNode;
            } else {
                finalParent.right = newNode;
            }
            addNodeToCanvas(newNode);
            relayout();
            status("Inserted: " + value);
            if (onFinished != null) onFinished.run();
        });
    }

    private void delete(int value) {
        delete(value, null);
    }

    private void delete(int value, Runnable onFinished) {
        TreeNode target = find(root, value);
        if (target == null) {
            status("Value " + value + " not found");
            if (onFinished != null) onFinished.run();
            return;
        }

        // Get path to target
        List<TreeNode> path = new ArrayList<>();
        TreeNode cur = root;
        while (cur != null) {
            path.add(cur);
            if (value == cur.value) break;
            cur = (value < cur.value) ? cur.left : cur.right;
        }

        double stepMs = 2400.0 / animationSpeed;
        animatePointerPath(path, stepMs, Color.web("#ef4444"), () -> {
            deleteNode(target);
            relayout();
            status("Deleted: " + value);
            if (onFinished != null) onFinished.run();
        });
    }

    private void deleteNode(TreeNode node) {
        if (node.left == null && node.right == null) {
            removeFromParent(node);
            removeNodeFromCanvas(node);
        } else if (node.left == null || node.right == null) {
            TreeNode child = (node.left != null) ? node.left : node.right;
            replaceNode(node, child);
            removeNodeFromCanvas(node);
        } else {
            TreeNode successor = minNode(node.right);
            int sVal = successor.value;
            deleteNode(successor);
            node.value = sVal;
            node.label.setText(String.valueOf(sVal));
        }
    }

    private void search(int value) {
        search(value, null);
    }

    private void search(int value, java.util.function.Consumer<Boolean> onFinished) {
        resetColors();
        TreeNode cur = root;
        List<TreeNode> path = new ArrayList<>();
        while (cur != null) {
            path.add(cur);
            if (value == cur.value) break;
            cur = (value < cur.value) ? cur.left : cur.right;
        }

        boolean found = (cur != null && cur.value == value);
        double stepMs = 2400.0 / animationSpeed;
        Color finalCol = found ? Color.web("#27ae60") : Color.web("#c0392b");

        animatePointerPath(path, stepMs, finalCol, () -> {
            status(found ? "Found " + value : "Value " + value + " not in tree");
            if (onFinished != null) onFinished.accept(found);
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  Layout Engine
    // ═══════════════════════════════════════════════════════════

    private void relayout() {
        if (root == null) return;
        
        // We calculate distribution based on a logical width
        // and then center the entire structure around X=2000
        int count = countNodes(root);
        double logicalW = Math.max(800, count * 60);
        double spacing = Math.max(MIN_H_GAP, logicalW / (count + 1));

        int[] idx = {0};
        assignInOrder(root, idx, spacing, 0);

        double treeWidth = count * spacing;
        // Center the tree horizontally around X=2000
        double offset = 2000 - (treeWidth / 2);
        applyOffset(root, offset);

        animateToTargets(root);
    }

    private void assignInOrder(TreeNode node, int[] idx, double spacing, int depth) {
        if (node == null) return;
        assignInOrder(node.left, idx, spacing, depth + 1);
        node.targetX = spacing * (idx[0]++) + spacing / 2;
        node.targetY = ROOT_Y + depth * VERTICAL_GAP;
        assignInOrder(node.right, idx, spacing, depth + 1);
    }

    private void applyOffset(TreeNode node, double dx) {
        if (node == null) return;
        node.targetX += dx;
        applyOffset(node.left, dx);
        applyOffset(node.right, dx);
    }

    private void animateToTargets(TreeNode node) {
        if (node == null) return;

        Timeline tl = new Timeline();
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(getAnimMs()),
                new KeyValue(node.circle.centerXProperty(), node.targetX),
                new KeyValue(node.circle.centerYProperty(), node.targetY)
        ));
        tl.play();

        animateToTargets(node.left);
        animateToTargets(node.right);
    }

    // ═══════════════════════════════════════════════════════════
    //  Canvas Helpers
    // ═══════════════════════════════════════════════════════════

    private void addNodeToCanvas(TreeNode node) {
        // Bind label position dynamically to node's center so it tracks it in real-time
        node.label.xProperty().bind(node.circle.centerXProperty().subtract(labelOffset(node)));
        node.label.yProperty().bind(node.circle.centerYProperty().add(5));

        if (node.parent != null) {
            Line edge = new Line();
            edge.setStroke(Color.web("#4a6fa5"));
            edge.setStrokeWidth(2);
            node.edgeToParent = edge;

            // Bind start coordinates to parent circle center
            edge.startXProperty().bind(node.parent.circle.centerXProperty());
            edge.startYProperty().bind(node.parent.circle.centerYProperty().add(24));

            // Start end coordinates at parent's center (grow animation starts from parent)
            edge.setEndX(node.parent.circle.getCenterX());
            edge.setEndY(node.parent.circle.getCenterY() + 24);

            canvas.getChildren().add(0, edge);

            // Animate line end point drawing itself to the child node's center
            Timeline growTimeline = new Timeline();
            growTimeline.getKeyFrames().add(new KeyFrame(
                Duration.millis(getAnimMs()),
                new KeyValue(edge.endXProperty(), node.circle.getCenterX()),
                new KeyValue(edge.endYProperty(), node.circle.getCenterY() - 24)
            ));
            growTimeline.setOnFinished(e -> {
                // Bind dynamically once growth animation completes
                edge.endXProperty().bind(node.circle.centerXProperty());
                edge.endYProperty().bind(node.circle.centerYProperty().subtract(24));
            });
            growTimeline.play();
        }
        DropShadow ds = new DropShadow(10, Color.rgb(0, 0, 0, 0.4));
        node.circle.setEffect(ds);
        canvas.getChildren().addAll(node.circle, node.label);
    }

    private void removeNodeFromCanvas(TreeNode node) {
        canvas.getChildren().removeAll(node.circle, node.label);
        if (node.edgeToParent != null) {
            canvas.getChildren().remove(node.edgeToParent);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  BST Utility Methods
    // ═══════════════════════════════════════════════════════════

    private TreeNode find(TreeNode node, int value) {
        if (node == null) return null;
        if (value == node.value) return node;
        return value < node.value ? find(node.left, value) : find(node.right, value);
    }

    private TreeNode minNode(TreeNode node) {
        while (node.left != null) node = node.left;
        return node;
    }

    private void removeFromParent(TreeNode node) {
        if (node.parent == null) { root = null; return; }
        if (node.parent.left == node) node.parent.left = null;
        else node.parent.right = null;
    }

    private void replaceNode(TreeNode old, TreeNode replacement) {
        if (old.parent == null) {
            root = replacement;
        } else if (old.parent.left == old) {
            old.parent.left = replacement;
        } else {
            old.parent.right = replacement;
        }
        if (replacement != null) {
            replacement.parent = old.parent;
            if (old.edgeToParent != null) {
                if (replacement.edgeToParent != null) {
                    canvas.getChildren().remove(replacement.edgeToParent);
                }
                replacement.edgeToParent = old.edgeToParent;
                // Rebind edge start to new parent if it exists
                if (replacement.parent != null) {
                    replacement.edgeToParent.startXProperty().bind(replacement.parent.circle.centerXProperty());
                    replacement.edgeToParent.startYProperty().bind(replacement.parent.circle.centerYProperty().add(24));
                } else {
                    canvas.getChildren().remove(replacement.edgeToParent);
                    replacement.edgeToParent = null;
                }
                // Rebind edge end to replacement
                if (replacement.edgeToParent != null) {
                    replacement.edgeToParent.endXProperty().bind(replacement.circle.centerXProperty());
                    replacement.edgeToParent.endYProperty().bind(replacement.circle.centerYProperty().subtract(24));
                }
            }
        }
    }

    private int countNodes(TreeNode n) {
        if (n == null) return 0;
        return 1 + countNodes(n.left) + countNodes(n.right);
    }

    private void resetColors() {
        resetColorsRec(root);
    }
    private void resetColorsRec(TreeNode n) {
        if (n == null) return;
        n.circle.setFill(Color.web(accentColorHex));
        n.circle.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.4)));
        resetColorsRec(n.left);
        resetColorsRec(n.right);
    }

    private void resetScales(TreeNode n) {
        if (n == null) return;
        n.circle.setScaleX(1.0);
        n.circle.setScaleY(1.0);
        n.label.setScaleX(1.0);
        n.label.setScaleY(1.0);
        resetScales(n.left);
        resetScales(n.right);
    }

    private double labelOffset(TreeNode n) {
        return 5 + (String.valueOf(n.value).length() - 1) * 4;
    }

    private String padLineNum(int num) {
        return String.format("%3d", num);
    }
    private String padLineNum(String s) {
        return "   ";
    }

    private int countExecutable(String[] lines) {
        int count = 0;
        for (String l : lines) {
            String t = l.trim();
            if (!t.isEmpty() && !t.startsWith("//") && !t.startsWith("/*")
                    && !t.startsWith("*") && !t.equals("{") && !t.equals("}")) {
                count++;
            }
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════
    //  UI Handlers
    // ═══════════════════════════════════════════════════════════

    private void handleInsert(TextField tf) {
        Integer v = parseField(tf);
        if (v != null) {
            insert(v);
            highlightNode(v, Color.web("#27ae60"), 800);
        }
    }
    private void handleDelete(TextField tf) {
        Integer v = parseField(tf);
        if (v != null) delete(v);
    }
    private void handleSearch(TextField tf) {
        Integer v = parseField(tf);
        if (v != null) search(v);
    }
    private void handleRandom() {
        handleClear();
        java.util.Random rng = new java.util.Random();
        int count = 7 + rng.nextInt(6);
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (int i = 0; i < count; i++) {
            int val;
            do { val = rng.nextInt(99) + 1; } while (used.contains(val));
            used.add(val);
            insert(val);
        }
        status("Generated random tree with " + count + " nodes");
    }
    private void handleClear() {
        root = null;
        canvas.getChildren().clear();
        status("Tree cleared");
        // Optional: Reset zoom and pan on clear
        // scaleTransform.setX(1.0); scaleTransform.setY(1.0);
        // translateTransform.setX(viewport.getWidth() / 2 - 2000);
        // translateTransform.setY(20);
    }

    private Integer parseField(TextField tf) {
        String text = tf.getText().trim();
        tf.clear();
        if (text.isEmpty()) { status("Please enter a value"); return null; }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            status("Invalid number: " + text);
            return null;
        }
    }

    private void status(String msg) {
        statusLabel.setText(msg);
    }

    // ═══════════════════════════════════════════════════════════
    //  Styling Helpers
    // ═══════════════════════════════════════════════════════════

    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 18;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.82));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    private String fieldStyle() {
        return "-fx-background-color: #0a0f2e;" +
               "-fx-text-fill: white;" +
               "-fx-prompt-text-fill: #5566aa;" +
               "-fx-border-color: #0048ff;" +
               "-fx-border-radius: 6;" +
               "-fx-background-radius: 6;" +
               "-fx-padding: 8;" +
               "-fx-font-size: 13px;";
    }

    // ── Main ───────────────────────────────────────────────────

}
