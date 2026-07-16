import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Interpolator;
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
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.animation.ParallelTransition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Binary Search Tree Visualizer with panning, zooming, and a Java code editor.
 */
public class ExplorerView {

    // ── Constants ──────────────────────────────────────────────
    private static final double ROOT_Y       = 60;
    private static final double VERTICAL_GAP = 80;
    private static final double MIN_H_GAP    = 60;
    private static final double ANIM_MS      = 350;
    private static final double EXEC_DELAY   = 700;

    // ── State ──────────────────────────────────────────────────
    private TreeNode root;
    private BTreeNode bTreeRoot;
    private Pane canvas;
    private Pane viewport;
    private Label statusLabel;
    private TextArea codeArea;
    private VBox codePanel;
    private Button toggleCodeBtn;
    private boolean codePanelVisible = true;
    private Label activeRotationOverlay = null;
    private final List<Timeline> activeTimelines = new ArrayList<>();
    private final List<javafx.animation.Animation> activeAnimations = new CopyOnWriteArrayList<>();

    private void registerAnimation(javafx.animation.Animation anim) {
        if (anim == null) return;
        activeAnimations.add(anim);
        anim.statusProperty().addListener((obs, oldStatus, newStatus) -> {
            if (newStatus == javafx.animation.Animation.Status.STOPPED) {
                activeAnimations.remove(anim);
            }
        });
    }
    private int executionSessionId = 0;
    private int treeSessionId = 0;
    private TextArea outputArea;
    private String currentMode = "BST"; // "BST" or "AVL"
    private int executingLine = -1;
    private int animationSpeed = 4;
    private String accentColorHex = Theme.TEAL;
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
            Pattern.compile("^\\s*(?:BinarySearchTree|BST|AVLTree|AVL|RedBlackTree|RBT|RedBlack|BTree|23Tree|TreeNode)\\s+\\w+\\s*=\\s*new\\s+(?:BinarySearchTree|BST|AVLTree|AVL|RedBlackTree|RBT|RedBlack|BTree|23Tree|TreeNode)\\s*\\(\\s*\\)\\s*;?\\s*$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern SYSOUT =
            Pattern.compile("^\\s*System\\.out\\.println\\s*\\((.*)\\)\\s*;?\\s*$");
    private static final Pattern ARRAY_DECL =
            Pattern.compile("^\\s*int\\[\\]\\s*\\w+\\s*=\\s*(?:new\\s+int\\[\\]\\s*)?\\{([^}]+)\\}\\s*;?\\s*$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern FOR_EACH =
            Pattern.compile("^\\s*for\\s*\\(\\s*int\\s+\\w+\\s*:\\s*(\\w+)\\s*\\)\\s*\\{?\\s*(?:\\w+\\.)(insert|delete|search)\\s*\\(\\s*\\w+\\s*\\)\\s*;?\\s*\\}?\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private java.util.Map<String, int[]> declaredArrays = new java.util.HashMap<>();
    
    private boolean hideManualControls;

    // Node Inspector Fields
    private VBox inspectorCard;
    private Label inspTitle;
    private Label inspValue;
    private Label inspHeight;
    private Label inspDepth;
    private Label inspBF;
    private Label inspParent;
    private Label inspChildren;

    public int getRootValue() {
        if ("B-Tree".equals(currentMode)) {
            return bTreeRoot == null || bTreeRoot.keys.isEmpty() ? -1 : bTreeRoot.keys.get(0);
        }
        return root == null ? -1 : root.value;
    }

    public int getHeight() {
        if ("B-Tree".equals(currentMode)) {
            return getBTreeHeight(bTreeRoot);
        }
        return root == null ? 0 : root.height;
    }

    private int getBTreeHeight(BTreeNode node) {
        if (node == null) return 0;
        if (node.isLeaf()) return 1;
        return 1 + getBTreeHeight(node.children.get(0));
    }

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

        Button centerBtn = styledButton("🔍 Reset", "#34495e");
        centerBtn.setOnAction(e -> {
            scaleTransform.setX(1.0);
            scaleTransform.setY(1.0);
            translateTransform.setX(viewport.getWidth() / 2 - 2000);
            translateTransform.setY(20);
            status("View reset to center");
        });

        toggleCodeBtn = styledButton("◀ Code", "#e67e22");
        toggleCodeBtn.setOnAction(e -> toggleCodePanel());

        Button backBtn = styledButton("← Back", "#555555");
        backBtn.setOnAction(e -> {
            stopAllTimelines();
            treeSessionId++;
            executionSessionId++;
            if ("teacher".equals(App.db.getCurrentUserRole())) {
                App.changeScene(new TeacherDashboardView().getView());
            } else {
                App.changeScene(new DashboardView().getView());
            }
        });

        HBox controls = new HBox(10, backBtn);
        controls.getChildren().addAll(zoomInBtn, zoomOutBtn, centerBtn, new Separator(Orientation.VERTICAL), toggleCodeBtn);
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

        // ── Initialize Node Inspector Card (Viewport Overlay) ──
        inspectorCard = buildInspectorCard();
        // Removed viewport overlay addition to hide inspector card from all pages as requested.
        // viewport.getChildren().add(inspectorCard);
        inspectorCard.setLayoutX(14);
        inspectorCard.layoutYProperty().bind(viewport.heightProperty().subtract(170));

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
            "-fx-background-color: " + Theme.TEAL + ";" +
            "-fx-text-fill: " + Theme.BG + ";" +
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
        Button btreeBtn = createSidebarOutlineButton("🌳 B-Tree");
        
        treeModeButtons = new Button[]{bstBtn, avlBtn, rbBtn, btreeBtn};
        activeModeButton = bstBtn;
        
        bstBtn.setOnAction(e -> {
            if ("B-Tree".equals(currentMode)) { handleClear(); }
            setTreeModeActive(bstBtn, new Button[]{avlBtn, rbBtn, btreeBtn});
            currentMode = "BST";
            toggleBalanceLabels(false);
            status("Tree Mode: Binary Search Tree (BST)");
            resetColors();
        });
        avlBtn.setOnAction(e -> {
            if ("B-Tree".equals(currentMode)) { handleClear(); }
            treeSessionId++;
            executionSessionId++;
            int sessionId = treeSessionId;
            setTreeModeActive(avlBtn, new Button[]{bstBtn, rbBtn, btreeBtn});
            currentMode = "AVL";
            recomputeHeights(root);
            toggleBalanceLabels(true);
            status("Switched to AVL Tree Mode. Balancing the tree...");
            stepwiseBalanceBST(null, sessionId);
        });
        rbBtn.setOnAction(e -> {
            if ("B-Tree".equals(currentMode)) { handleClear(); }
            treeSessionId++;
            executionSessionId++;
            int sessionId = treeSessionId;
            setTreeModeActive(rbBtn, new Button[]{bstBtn, avlBtn, btreeBtn});
            currentMode = "Red-Black";
            toggleBalanceLabels(false);
            status("Switched to Red-Black Tree Mode. Rebuilding tree step-by-step...");
            convertToRedBlackStepwise(null, sessionId);
        });
        btreeBtn.setOnAction(e -> {
            treeSessionId++;
            executionSessionId++;
            setTreeModeActive(btreeBtn, new Button[]{bstBtn, avlBtn, rbBtn});
            currentMode = "B-Tree";
            toggleBalanceLabels(false);
            handleClear();
            status("Switched to B-Tree (2-3 Tree) Mode. Ready to insert nodes.");
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
            if ("B-Tree".equals(currentMode)) { status("Traversals are disabled in B-Tree mode."); return; }
            List<TreeNode> path = new ArrayList<>();
            inOrder(root, path);
            animateTraversal(path, traversalResult);
        });
        preorderBtn.setOnAction(e -> {
            if ("B-Tree".equals(currentMode)) { status("Traversals are disabled in B-Tree mode."); return; }
            List<TreeNode> path = new ArrayList<>();
            preOrder(root, path);
            animateTraversal(path, traversalResult);
        });
        postorderBtn.setOnAction(e -> {
            if ("B-Tree".equals(currentMode)) { status("Traversals are disabled in B-Tree mode."); return; }
            List<TreeNode> path = new ArrayList<>();
            postOrder(root, path);
            animateTraversal(path, traversalResult);
        });
        levelorderBtn.setOnAction(e -> {
            if ("B-Tree".equals(currentMode)) { status("Traversals are disabled in B-Tree mode."); return; }
            List<TreeNode> path = new ArrayList<>();
            levelOrder(root, path);
            animateTraversal(path, traversalResult);
        });
        
        traversalsContent.getChildren().addAll(tRow1, tRow2, resultBox);
        VBox traversalsSection = createSidebarSection("TRAVERSALS", traversalsContent);

        // 6. THEME ACCENT
        HBox accentContent = new HBox(12);
        accentContent.setAlignment(Pos.CENTER);
        Button swatchTeal = createColorSwatch(Theme.TEAL);
        Button swatchViolet = createColorSwatch("#9b5de5");
        Button swatchCoral = createColorSwatch("#ff6b6b");
        Button swatchGold = createColorSwatch("#f1c40f");
        
        swatchTeal.setOnAction(e -> updateAccentTheme(Theme.TEAL));
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
        
        Platform.runLater(() -> {
            javafx.scene.Node thumb = speedSlider.lookup(".thumb");
            if (thumb != null) thumb.setStyle("-fx-background-color: " + accentColorHex + "; -fx-background-radius: 10;");
            javafx.scene.Node track = speedSlider.lookup(".track");
            if (track != null) track.setStyle("-fx-background-color: #1e2c56; -fx-background-insets: 0; -fx-background-radius: 4;");
        });

        Label speedValueLabel = new Label("4");
        speedValueLabel.setTextFill(Color.web(Theme.TEAL));
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
            javafx.scene.Node thumb = speedSlider.lookup(".thumb");
            if (thumb != null) {
                thumb.setStyle("-fx-background-color: " + hexColor + "; -fx-background-radius: 10;");
            }
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
        treeSessionId++;
        executionSessionId++;
        int sessionId = treeSessionId;
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
        insertSequence(vals, 0, sessionId);
    }

    private void insertSequence(List<Integer> vals, int index, int sessionId) {
        if (sessionId != treeSessionId) return;
        if ("B-Tree".equals(currentMode)) {
            insertBTreeSequence(vals, index, sessionId);
            return;
        }
        if (index >= vals.size()) {
            status("Batch insertion complete");
            return;
        }
        int val = vals.get(index);
        insert(val, null, sessionId);
        highlightNode(val, Color.web("#10b981"), 800);
        
        PauseTransition pause = new PauseTransition(Duration.millis(getAnimMs() + 100));
        pause.setOnFinished(e -> {
            if (sessionId == treeSessionId) {
                insertSequence(vals, index + 1, sessionId);
            }
        });
        registerAnimation(pause);
        pause.play();
    }

    private double getAnimMs() {
        return 1400.0 / animationSpeed;
    }

    private double getExecDelay() {
        return 2800.0 / animationSpeed;
    }

    private void loadPresetBalanced() {
        treeSessionId++;
        executionSessionId++;
        int sessionId = treeSessionId;
        handleClear();
        int[] vals = {40, 20, 60, 10, 30, 50, 70};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0, sessionId);
    }

    private void loadPresetSkewed() {
        treeSessionId++;
        executionSessionId++;
        int sessionId = treeSessionId;
        handleClear();
        int[] vals = {10, 20, 30, 40, 50, 60};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0, sessionId);
    }

    private void loadPresetRandom(int count) {
        treeSessionId++;
        executionSessionId++;
        int sessionId = treeSessionId;
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
        insertSequence(list, 0, sessionId);
    }

    private void loadPresetSorted() {
        treeSessionId++;
        executionSessionId++;
        int sessionId = treeSessionId;
        handleClear();
        int[] vals = {10, 20, 30, 40, 50, 60, 70};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0, sessionId);
    }

    private void loadPresetZigZag() {
        treeSessionId++;
        executionSessionId++;
        int sessionId = treeSessionId;
        handleClear();
        int[] vals = {50, 20, 40, 30, 35};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0, sessionId);
    }

    private void loadPresetComplete() {
        treeSessionId++;
        executionSessionId++;
        int sessionId = treeSessionId;
        handleClear();
        int[] vals = {50, 25, 75, 12, 37, 62, 87, 6, 18, 31, 43};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0, sessionId);
    }

    private void loadPresetFibonacci() {
        treeSessionId++;
        executionSessionId++;
        int sessionId = treeSessionId;
        handleClear();
        int[] vals = {13, 5, 34, 2, 8, 21, 55, 1, 3, 89};
        List<Integer> list = new ArrayList<>();
        for (int v : vals) list.add(v);
        insertSequence(list, 0, sessionId);
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
        stopAllTimelines();
        resetColors();
        resetScales(root);
        
        double stepMs = 2400.0 / animationSpeed;
        Timeline tl = new Timeline();
        activeTimelines.add(tl);
        tl.setOnFinished(e -> activeTimelines.remove(tl));
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
        if ("B-Tree".equals(currentMode)) {
            return findBTreeNode(bTreeRoot, value) != null;
        }
        return find(root, value) != null;
    }

    private BTreeNode findBTreeNode(BTreeNode node, int value) {
        if (node == null) return null;
        if (node.keys.contains(value)) return node;
        if (node.isLeaf()) return null;
        int idx = 0;
        while (idx < node.keys.size() && value > node.keys.get(idx)) {
            idx++;
        }
        return findBTreeNode(node.children.get(idx), value);
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

        HBox btnRow;
        if (hideManualControls) {
            btnRow = new HBox(6, runBtn, stepBtn, resetBtn);
        } else {
            Button exampleBtn = styledButton("📋 Example", "#8e44ad");
            exampleBtn.setPrefWidth(120);
            exampleBtn.setOnAction(e -> loadExample());
            btnRow = new HBox(6, runBtn, stepBtn, resetBtn, exampleBtn);
        }
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(6, 0, 0, 0));

        VBox panel = new VBox(6, panelTitle, hint, codeArea, btnRow, outputLabel, outputArea);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #080d28; -fx-border-color: #0048ff; -fx-border-width: 0 0 0 2;");
        panel.setPrefWidth(440);
        panel.setMinWidth(440);

        if (!hideManualControls) {
            loadExample();
        }

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

    private String preProcessCode(String code) {
        if (code == null) return "";
        // Strip multi-line comments
        code = code.replaceAll("(?s)/\\*.*?\\*/", "");
        // Strip single-line comments
        code = code.replaceAll("//.*", "");
        // Collapse multi-line for-each loops with braces
        code = code.replaceAll("(?i)for\\s*\\(\\s*int\\s+(\\w+)\\s*:\\s*(\\w+)\\s*\\)\\s*\\{\\s*\\r?\\n\\s*(\\w+\\.(?:insert|delete|search)\\(\\s*\\1\\s*\\)\\s*;?)\\s*\\r?\\n\\s*\\}", "for (int $1 : $2) { $3 }");
        // Collapse multi-line for-each loops without braces
        code = code.replaceAll("(?i)for\\s*\\(\\s*int\\s+(\\w+)\\s*:\\s*(\\w+)\\s*\\)\\s*\\r?\\n\\s*(\\w+\\.(?:insert|delete|search)\\(\\s*\\1\\s*\\)\\s*;?)", "for (int $1 : $2) { $3 }");
        
        // Collapse multi-line array declarations
        java.util.regex.Pattern arrPat = java.util.regex.Pattern.compile("(?is)int\\[\\]\\s*(\\w+)\\s*=\\s*(?:new\\s+int\\[\\]\\s*)?\\{\\s*([^}]+)\\s*\\}\\s*;?");
        java.util.regex.Matcher arrMat = arrPat.matcher(code);
        StringBuilder sb = new StringBuilder();
        while (arrMat.find()) {
            String name = arrMat.group(1);
            String vals = arrMat.group(2).replaceAll("\\r?\\n", " ");
            arrMat.appendReplacement(sb, "int[] " + name + " = {" + vals + "};");
        }
        arrMat.appendTail(sb);
        return sb.toString();
    }

    private void runCode() {
        String code = preProcessCode(codeArea.getText());
        if (code == null || code.trim().isEmpty()) {
            status("No code to run");
            return;
        }

        treeSessionId++;
        executionSessionId++;
        int sessionId = executionSessionId;

        String[] lines = code.split("\\n");
        outputArea.clear();
        declaredArrays.clear();
        executingLine = 0;
        outputArea.appendText("╔══════════════════════════════════════╗\n");
        outputArea.appendText("║       ▶  Program Execution          ║\n");
        outputArea.appendText("╚══════════════════════════════════════╝\n\n");

        executeLineSequence(lines, 0, sessionId);
    }

    private void executeLineSequence(String[] lines, int index, int sessionId) {
        if (sessionId != executionSessionId) return;

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
            pause.setOnFinished(e -> {
                if (sessionId == executionSessionId) {
                    executeLineSequence(lines, index + 1, sessionId);
                }
            });
            registerAnimation(pause);
            pause.play();
            return;
        }

        outputArea.appendText("  ►   " + padLineNum(index + 1) + " │ " + line + "\n");

        executeSingleCommand(line, (result) -> {
            if (sessionId != executionSessionId) return;
            if (result != null) {
                outputArea.appendText("      " + padLineNum("") + " └─➤ " + result + "\n");
            }
            outputArea.positionCaret(outputArea.getLength());
            
            // Wait a small buffer delay after the command finishes animating
            PauseTransition pause2 = new PauseTransition(Duration.millis(300));
            pause2.setOnFinished(e -> {
                if (sessionId == executionSessionId) {
                    executeLineSequence(lines, index + 1, sessionId);
                }
            });
            registerAnimation(pause2);
            pause2.play();
        });
    }

    private void stepNextLine() {
        String code = preProcessCode(codeArea.getText());
        if (code == null || code.trim().isEmpty()) {
            status("No code to step through");
            return;
        }

        treeSessionId++;
        executionSessionId++;

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
        treeSessionId++;
        executionSessionId++;
        executingLine = -1;
        declaredArrays.clear();
        outputArea.clear();
        outputArea.appendText("↺ Execution reset. Ready to run.\n");
        resetColors();
        resetScales(root);
        stopAllTimelines();
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
            String lower = line.toLowerCase();
            if (lower.contains("avltree") || lower.contains("new avl")) {
                Platform.runLater(() -> {
                    setTreeModeActive(treeModeButtons[1], new Button[]{treeModeButtons[0], treeModeButtons[2], treeModeButtons[3]});
                    currentMode = "AVL";
                    toggleBalanceLabels(true);
                });
                callback.accept("AVLTree initialized");
            } else if (lower.contains("redblack") || lower.contains("rbt")) {
                Platform.runLater(() -> {
                    setTreeModeActive(treeModeButtons[2], new Button[]{treeModeButtons[0], treeModeButtons[1], treeModeButtons[3]});
                    currentMode = "Red-Black";
                    toggleBalanceLabels(false);
                });
                callback.accept("RedBlackTree initialized");
            } else if (lower.contains("btree") || lower.contains("23tree")) {
                Platform.runLater(() -> {
                    setTreeModeActive(treeModeButtons[3], new Button[]{treeModeButtons[0], treeModeButtons[1], treeModeButtons[2]});
                    currentMode = "B-Tree";
                    toggleBalanceLabels(false);
                });
                callback.accept("B-Tree (2-3 Tree) initialized");
            } else {
                Platform.runLater(() -> {
                    setTreeModeActive(treeModeButtons[0], new Button[]{treeModeButtons[1], treeModeButtons[2], treeModeButtons[3]});
                    currentMode = "BST";
                    toggleBalanceLabels(false);
                });
                callback.accept("BinarySearchTree initialized");
            }
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
                if (currentMode.equals("B-Tree")) {
                    bTreeInsert(val, () -> callback.accept("Inserted node " + val), treeSessionId);
                } else {
                    insert(val, () -> callback.accept("Inserted node " + val));
                }
                break;
            }
            case "delete": {
                if (arg.isEmpty()) { callback.accept("❌ delete requires a number"); return; }
                int val = Integer.parseInt(arg);
                if (currentMode.equals("B-Tree")) {
                    bTreeDelete(val, () -> callback.accept("Deleted node " + val), treeSessionId);
                } else {
                    TreeNode target = find(root, val);
                    if (target != null) {
                        delete(val, () -> callback.accept("Deleted node " + val));
                    } else {
                        callback.accept("❌ Node " + val + " not found in tree");
                    }
                }
                break;
            }
            case "search": {
                if (arg.isEmpty()) { callback.accept("❌ search requires a number"); return; }
                int val = Integer.parseInt(arg);
                if (currentMode.equals("B-Tree")) {
                    bTreeSearch(val, (found) -> callback.accept(found ? "✔ Found node " + val : "✘ Node " + val + " not found"), treeSessionId);
                } else {
                    search(val, (found) -> callback.accept(found ? "✔ Found node " + val : "✘ Node " + val + " not found"));
                }
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

        stopAllTimelines();
        resetColors();
        resetScales(root);

        // Spawn a pointer circle on the canvas
        javafx.scene.shape.Circle pointer = new javafx.scene.shape.Circle(12);
        pointer.setFill(Color.web("#f1c40f")); // Standard gold
        pointer.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.4)));

        // Position initially at the root node's center
        TreeNode startNode = path.get(0);
        pointer.setCenterX(startNode.circle.getCenterX());
        pointer.setCenterY(startNode.circle.getCenterY());
        canvas.getChildren().add(pointer);

        Timeline tl = new Timeline();
        activeTimelines.add(tl);
        tl.setOnFinished(e -> activeTimelines.remove(tl));

        // Animate node-to-node
        for (int i = 0; i < path.size(); i++) {
            TreeNode node = path.get(i);
            final int idx = i;
            boolean isLast = (idx == path.size() - 1);

            // KeyFrame at start of each node visit: highlight colors & edge glow
            tl.getKeyFrames().add(new KeyFrame(
                Duration.millis(stepMs * idx),
                e -> {
                    Color col = isLast ? finalColor : Color.web("#f39c12");
                    node.circle.setFill(col);
                    if (node.edgeToParent != null) {
                        node.edgeToParent.setStroke(Color.web("#f39c12"));
                        node.edgeToParent.setStrokeWidth(3.5);
                    }
                }
            ));

            // Animate node circle scale-up
            tl.getKeyFrames().add(new KeyFrame(
                Duration.millis(stepMs * idx),
                new KeyValue(node.circle.scaleXProperty(), isLast ? 1.4 : 1.35, Interpolator.EASE_BOTH),
                new KeyValue(node.circle.scaleYProperty(), isLast ? 1.4 : 1.35, Interpolator.EASE_BOTH)
            ));

            // If not last, scale back down halfway to the next node visit
            if (!isLast) {
                double scaleDownTime = stepMs * idx + Math.min(300, stepMs * 0.5);
                tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(scaleDownTime),
                    new KeyValue(node.circle.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                    new KeyValue(node.circle.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
                ));
            }

            // Animate pointer translation from previous node to current node
            if (idx > 0) {
                tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(stepMs * idx),
                    new KeyValue(pointer.centerXProperty(), node.circle.getCenterX(), Interpolator.EASE_BOTH),
                    new KeyValue(pointer.centerYProperty(), node.circle.getCenterY(), Interpolator.EASE_BOTH)
                ));
            }

            // Animate pointer scale hopping (peak scale at visit start, reset scale midway)
            tl.getKeyFrames().add(new KeyFrame(
                Duration.millis(stepMs * idx),
                new KeyValue(pointer.scaleXProperty(), 1.4, Interpolator.EASE_BOTH),
                new KeyValue(pointer.scaleYProperty(), 1.4, Interpolator.EASE_BOTH)
            ));
            if (!isLast) {
                tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(stepMs * idx + stepMs * 0.5),
                    new KeyValue(pointer.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                    new KeyValue(pointer.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
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

        node.circle.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.5)));
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
        registerAnimation(reset);
        reset.play();
    }

    // ═══════════════════════════════════════════════════════════
    //  BST Operations
    // ═══════════════════════════════════════════════════════════

    private void insert(int value) {
        insert(value, null, treeSessionId);
    }

    private void insert(int value, Runnable onFinished) {
        insert(value, onFinished, treeSessionId);
    }

    private void insert(int value, Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
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
            if (sessionId != treeSessionId) return;
            TreeNode newNode = new TreeNode(value);
            newNode.parent = finalParent;
            if (value < finalParent.value) {
                finalParent.left = newNode;
            } else {
                finalParent.right = newNode;
            }
            newNode.placeAt(finalParent.circle.getCenterX(), finalParent.circle.getCenterY());
            addNodeToCanvas(newNode);

            if (currentMode.equals("AVL")) {
                balanceTreeAfterInsert(newNode, onFinished, sessionId);
            } else if (currentMode.equals("Red-Black")) {
                balanceTreeAfterInsertRB(newNode, onFinished, sessionId);
            } else {
                relayout();
                status("Inserted: " + value);
                if (onFinished != null) onFinished.run();
            }
        });
    }

    private void delete(int value) {
        delete(value, null, treeSessionId);
    }

    private void delete(int value, Runnable onFinished) {
        delete(value, onFinished, treeSessionId);
    }

    private void delete(int value, Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
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
            if (sessionId != treeSessionId) return;
            TreeNode startBalanceNode = null;
            if (target.left == null && target.right == null) {
                startBalanceNode = target.parent;
            } else if (target.left == null || target.right == null) {
                startBalanceNode = target.parent;
            } else {
                TreeNode successor = minNode(target.right);
                startBalanceNode = successor.parent;
                if (startBalanceNode == target) {
                    startBalanceNode = successor;
                }
            }

            deleteNode(target);

            if (currentMode.equals("AVL")) {
                balanceTreeAfterDelete(startBalanceNode, onFinished, sessionId);
            } else if (currentMode.equals("Red-Black")) {
                status("Deleted " + value + ". Rebuilding Red-Black Tree...");
                convertToRedBlackStepwise(onFinished, sessionId);
            } else {
                relayout();
                status("Deleted: " + value);
                if (onFinished != null) onFinished.run();
            }
        });
    }

    private void deleteNode(TreeNode node) {
        if (node.left == null && node.right == null) {
            removeFromParent(node);
            fadeOutAndRemoveNode(node);
        } else if (node.left == null || node.right == null) {
            TreeNode child = (node.left != null) ? node.left : node.right;
            replaceNode(node, child);
            fadeOutAndRemoveNode(node);
        } else {
            TreeNode successor = minNode(node.right);
            int sVal = successor.value;
            deleteNode(successor);
            node.value = sVal;
            node.label.setText(String.valueOf(sVal));
        }
    }

    private void fadeOutAndRemoveNode(TreeNode node) {
        if (node == null) return;

        FadeTransition ftCircle = new FadeTransition(Duration.millis(300), node.circle);
        ftCircle.setFromValue(1.0);
        ftCircle.setToValue(0.0);

        FadeTransition ftLabel = new FadeTransition(Duration.millis(300), node.label);
        ftLabel.setFromValue(1.0);
        ftLabel.setToValue(0.0);

        FadeTransition ftBalance = new FadeTransition(Duration.millis(300), node.balanceLabel);
        ftBalance.setFromValue(1.0);
        ftBalance.setToValue(0.0);

        if (node.edgeToParent != null) {
            FadeTransition ftEdge = new FadeTransition(Duration.millis(250), node.edgeToParent);
            ftEdge.setFromValue(1.0);
            ftEdge.setToValue(0.0);
            ftEdge.play();
        }

        ftCircle.setOnFinished(e -> removeNodeFromCanvas(node));

        ftCircle.play();
        ftLabel.play();
        ftBalance.play();
    }

    private void search(int value) {
        search(value, null, treeSessionId);
    }

    private void search(int value, java.util.function.Consumer<Boolean> onFinished) {
        search(value, onFinished, treeSessionId);
    }

    private void search(int value, java.util.function.Consumer<Boolean> onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
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
            if (sessionId != treeSessionId) return;
            status(found ? "Found " + value : "Value " + value + " not in tree");
            if (onFinished != null) onFinished.accept(found);
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  Layout Engine
    // ═══════════════════════════════════════════════════════════

    private static class LayoutData {
        double relX;
        List<Double> leftContour = new ArrayList<>();
        List<Double> rightContour = new ArrayList<>();
    }

    private LayoutData computeRelativePositions(TreeNode node, java.util.Map<TreeNode, Double> relXMap) {
        if (node == null) return null;

        LayoutData data = new LayoutData();

        if (node.left == null && node.right == null) {
            data.relX = 0;
            data.leftContour.add(0.0);
            data.rightContour.add(0.0);
            return data;
        }

        LayoutData leftData = computeRelativePositions(node.left, relXMap);
        LayoutData rightData = computeRelativePositions(node.right, relXMap);

        if (leftData != null && rightData != null) {
            double maxOverlapShift = MIN_H_GAP;
            int overlapDepth = Math.min(leftData.rightContour.size(), rightData.leftContour.size());
            for (int i = 0; i < overlapDepth; i++) {
                double leftRightContour = leftData.rightContour.get(i);
                double rightLeftContour = rightData.leftContour.get(i);
                double shift = leftRightContour - rightLeftContour + MIN_H_GAP;
                if (shift > maxOverlapShift) {
                    maxOverlapShift = shift;
                }
            }

            double leftOffset = -maxOverlapShift / 2.0;
            double rightOffset = maxOverlapShift / 2.0;

            relXMap.put(node, maxOverlapShift);

            data.relX = 0;
            data.leftContour.add(0.0);
            data.rightContour.add(0.0);

            int maxContourSize = Math.max(leftData.leftContour.size(), rightData.leftContour.size());
            for (int i = 0; i < maxContourSize; i++) {
                double leftVal = (i < leftData.leftContour.size()) 
                    ? (leftOffset + leftData.leftContour.get(i)) 
                    : (rightOffset + rightData.leftContour.get(i));
                data.leftContour.add(leftVal);

                double rightVal = (i < rightData.rightContour.size()) 
                    ? (rightOffset + rightData.rightContour.get(i)) 
                    : (leftOffset + leftData.rightContour.get(i));
                data.rightContour.add(rightVal);
            }
        } else if (leftData != null) {
            double leftOffset = -MIN_H_GAP;
            data.relX = 0;
            data.leftContour.add(0.0);
            data.rightContour.add(0.0);
            for (double val : leftData.leftContour) {
                data.leftContour.add(leftOffset + val);
            }
            for (double val : leftData.rightContour) {
                data.rightContour.add(leftOffset + val);
            }
        } else {
            double rightOffset = MIN_H_GAP;
            data.relX = 0;
            data.leftContour.add(0.0);
            data.rightContour.add(0.0);
            for (double val : rightData.leftContour) {
                data.leftContour.add(rightOffset + val);
            }
            for (double val : rightData.rightContour) {
                data.rightContour.add(rightOffset + val);
            }
        }

        return data;
    }

    private void assignAbsoluteCoordinates(TreeNode node, double absX, int depth, java.util.Map<TreeNode, Double> relXMap) {
        if (node == null) return;
        node.targetX = absX;
        node.targetY = ROOT_Y + depth * VERTICAL_GAP;

        if (node.left != null && node.right != null) {
            double shift = relXMap.getOrDefault(node, MIN_H_GAP);
            assignAbsoluteCoordinates(node.left, absX - shift / 2.0, depth + 1, relXMap);
            assignAbsoluteCoordinates(node.right, absX + shift / 2.0, depth + 1, relXMap);
        } else if (node.left != null) {
            assignAbsoluteCoordinates(node.left, absX - MIN_H_GAP, depth + 1, relXMap);
        } else if (node.right != null) {
            assignAbsoluteCoordinates(node.right, absX + MIN_H_GAP, depth + 1, relXMap);
        }
    }

    private void findMinMaxX(TreeNode node, double[] minMax) {
        if (node == null) return;
        if (node.targetX < minMax[0]) minMax[0] = node.targetX;
        if (node.targetX > minMax[1]) minMax[1] = node.targetX;
        findMinMaxX(node.left, minMax);
        findMinMaxX(node.right, minMax);
    }

    private void relayout() {
        if (root == null) return;
        recomputeHeights(root);

        java.util.Map<TreeNode, Double> relXMap = new java.util.HashMap<>();
        computeRelativePositions(root, relXMap);
        assignAbsoluteCoordinates(root, 2000, 0, relXMap);

        double[] minMax = {Double.MAX_VALUE, -Double.MAX_VALUE};
        findMinMaxX(root, minMax);
        if (minMax[0] != Double.MAX_VALUE && minMax[1] != -Double.MAX_VALUE) {
            double offset = 2000 - (minMax[0] + minMax[1]) / 2.0;
            applyOffset(root, offset);
        }

        animateToTargets(root);
    }

    private void applyOffset(TreeNode node, double dx) {
        if (node == null) return;
        node.targetX += dx;
        applyOffset(node.left, dx);
        applyOffset(node.right, dx);
    }

    private void animateToTargets(TreeNode node) {
        if (node == null) return;

        double startX = node.circle.getCenterX();
        double startY = node.circle.getCenterY();
        double targetX = node.targetX;
        double targetY = node.targetY;

        // Skip animating if it is already at the target coordinates
        if (Math.abs(startX - targetX) < 0.1 && Math.abs(startY - targetY) < 0.1) {
            animateToTargets(node.left);
            animateToTargets(node.right);
            return;
        }

        double distance = Math.abs(targetX - startX);
        double maxHop = Math.min(45.0, distance * 0.25);

        javafx.animation.Transition transition = new javafx.animation.Transition() {
            {
                setCycleDuration(Duration.millis(getAnimMs()));
                setInterpolator(Interpolator.EASE_BOTH);
            }
            @Override
            protected void interpolate(double frac) {
                double x = startX + (targetX - startX) * frac;
                double hop = -maxHop * Math.sin(Math.PI * frac);
                double y = startY + (targetY - startY) * frac + hop;
                node.circle.setCenterX(x);
                node.circle.setCenterY(y);
            }
        };
        registerAnimation(transition);
        transition.play();

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

        // Bind balance label position to the top-right of node's circle
        node.balanceLabel.xProperty().bind(node.circle.centerXProperty().add(18));
        node.balanceLabel.yProperty().bind(node.circle.centerYProperty().subtract(18));
        node.balanceLabel.setVisible(currentMode.equals("AVL"));

        if (node.parent != null) {
            Line edge = new Line();
            edge.setStroke(Color.web("#4a6fa5"));
            edge.setStrokeWidth(2);
            node.edgeToParent = edge;

            // Bind start coordinates to parent circle center
            edge.startXProperty().bind(node.parent.circle.centerXProperty());
            edge.startYProperty().bind(node.parent.circle.centerYProperty().add(24));

            // Bind end coordinates to child circle center (with offset for child node)
            edge.endXProperty().bind(node.circle.centerXProperty());
            edge.endYProperty().bind(node.circle.centerYProperty().subtract(24));

            canvas.getChildren().add(0, edge);
        }
        node.circle.setOnMouseEntered(e -> {
            updateInspector(node);
            node.circle.setStroke(Color.WHITE);
            node.circle.setStrokeWidth(3);
        });
        node.circle.setOnMouseExited(e -> {
            clearInspector();
            node.circle.setStroke(Color.web("#0d3c66"));
            node.circle.setStrokeWidth(2);
        });

        DropShadow ds = new DropShadow(10, Color.rgb(0, 0, 0, 0.4));
        node.circle.setEffect(ds);
        canvas.getChildren().addAll(node.circle, node.label, node.balanceLabel);
    }

    private void removeNodeFromCanvas(TreeNode node) {
        canvas.getChildren().removeAll(node.circle, node.label, node.balanceLabel);
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
            if (replacement.parent == null) {
                if (replacement.edgeToParent != null) {
                    canvas.getChildren().remove(replacement.edgeToParent);
                    replacement.edgeToParent = null;
                }
            } else {
                if (replacement.edgeToParent != null) {
                    canvas.getChildren().remove(replacement.edgeToParent);
                    replacement.edgeToParent = null;
                }
                if (old.edgeToParent != null) {
                    replacement.edgeToParent = old.edgeToParent;
                    old.edgeToParent = null;
                }
                if (replacement.edgeToParent != null) {
                    replacement.edgeToParent.startXProperty().bind(replacement.parent.circle.centerXProperty());
                    replacement.edgeToParent.startYProperty().bind(replacement.parent.circle.centerYProperty().add(24));
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
        if ("B-Tree".equals(currentMode)) {
            resetBTreeColors(bTreeRoot);
        } else {
            resetColorsRec(root);
        }
    }
    private void resetColorsRec(TreeNode n) {
        if (n == null) return;
        if (currentMode.equals("Red-Black")) {
            n.circle.setFill(n.isRed ? Color.web("#e74c3c") : Color.web("#1e1e24"));
            n.circle.setStroke(n.isRed ? Color.web("#c0392b") : Color.web("#ffffff"));
            n.circle.setStrokeWidth(2);
        } else {
            n.circle.setFill(Color.web(accentColorHex));
            n.circle.setStroke(Color.web("#0d3c66"));
            n.circle.setStrokeWidth(2);
        }
        n.circle.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.4)));
        if (n.edgeToParent != null) {
            n.edgeToParent.setStroke(Color.web("#4a6fa5"));
            n.edgeToParent.setStrokeWidth(2);
        }
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
            treeSessionId++;
            stopAllTimelines();
            if ("B-Tree".equals(currentMode)) {
                bTreeInsert(v, null, treeSessionId);
            } else {
                insert(v, null, treeSessionId);
                highlightNode(v, Color.web("#27ae60"), 800);
            }
        }
    }
    private void handleDelete(TextField tf) {
        Integer v = parseField(tf);
        if (v != null) {
            treeSessionId++;
            stopAllTimelines();
            if ("B-Tree".equals(currentMode)) {
                bTreeDelete(v, null, treeSessionId);
            } else {
                delete(v, null, treeSessionId);
            }
        }
    }
    private void handleSearch(TextField tf) {
        Integer v = parseField(tf);
        if (v != null) {
            treeSessionId++;
            stopAllTimelines();
            if ("B-Tree".equals(currentMode)) {
                bTreeSearch(v, null, treeSessionId);
            } else {
                search(v, null, treeSessionId);
            }
        }
    }
    private void handleRandom() {
        treeSessionId++;
        executionSessionId++;
        int sessionId = treeSessionId;
        handleClear();
        java.util.Random rng = new java.util.Random();
        int count = 7 + rng.nextInt(6);
        java.util.Set<Integer> used = new java.util.HashSet<>();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int val;
            do { val = rng.nextInt(99) + 1; } while (used.contains(val));
            used.add(val);
            list.add(val);
        }
        insertSequence(list, 0, sessionId);
        status("Generated random tree with " + count + " nodes");
    }
    public void handleClear() {
        root = null;
        bTreeRoot = null;
        canvas.getChildren().clear();
        removeRotationOverlay();
        stopAllTimelines();
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

    // ── AVL Logic & Rotations ───────────────────────────────────

    private int height(TreeNode n) {
        return n == null ? 0 : n.height;
    }

    private int getBalance(TreeNode n) {
        return n == null ? 0 : height(n.left) - height(n.right);
    }

    private void updateHeight(TreeNode n) {
        if (n != null) {
            n.height = 1 + Math.max(height(n.left), height(n.right));
            int bf = getBalance(n);
            n.balanceLabel.setText((bf >= 0 ? "+" : "") + bf);
            if (bf < -1 || bf > 1) {
                n.balanceLabel.setFill(Color.web("#ef4444")); // Red for unbalanced
            } else {
                n.balanceLabel.setFill(Color.web("#a0b4ff")); // Cyan/blue for balanced
            }
        }
    }

    private int recomputeHeights(TreeNode n) {
        if (n == null) return 0;
        int lh = recomputeHeights(n.left);
        int rh = recomputeHeights(n.right);
        n.height = 1 + Math.max(lh, rh);
        int bf = lh - rh;
        n.balanceLabel.setText((bf >= 0 ? "+" : "") + bf);
        if (bf < -1 || bf > 1) {
            n.balanceLabel.setFill(Color.web("#ef4444"));
        } else {
            n.balanceLabel.setFill(Color.web("#a0b4ff"));
        }
        return n.height;
    }

    private TreeNode rotateRight(TreeNode y) {
        TreeNode x = y.left;
        TreeNode T2 = x.right;

        // Perform rotation
        x.right = y;
        y.left = T2;

        // Update parents
        x.parent = y.parent;
        y.parent = x;
        if (T2 != null) {
            T2.parent = y;
        }

        if (x.parent == null) {
            root = x;
        } else if (x.parent.left == y) {
            x.parent.left = x;
        } else {
            x.parent.right = x;
        }

        // Update heights
        updateHeight(y);
        updateHeight(x);

        return x;
    }

    private TreeNode rotateLeft(TreeNode x) {
        TreeNode y = x.right;
        TreeNode T2 = y.left;

        // Perform rotation
        y.left = x;
        x.right = T2;

        // Update parents
        y.parent = x.parent;
        x.parent = y;
        if (T2 != null) {
            T2.parent = x;
        }

        if (y.parent == null) {
            root = y;
        } else if (y.parent.left == x) {
            y.parent.left = y;
        } else {
            y.parent.right = y;
        }

        // Update heights
        updateHeight(x);
        updateHeight(y);

        return y;
    }

    private void rebindEdge(TreeNode node) {
        if (node == null) return;
        if (node.edgeToParent != null) {
            canvas.getChildren().remove(node.edgeToParent);
            node.edgeToParent = null;
        }
        if (node.parent != null) {
            Line edge = new Line();
            edge.setStroke(Color.web("#4a6fa5"));
            edge.setStrokeWidth(2);
            node.edgeToParent = edge;

            edge.startXProperty().bind(node.parent.circle.centerXProperty());
            edge.startYProperty().bind(node.parent.circle.centerYProperty().add(24));
            edge.endXProperty().bind(node.circle.centerXProperty());
            edge.endYProperty().bind(node.circle.centerYProperty().subtract(24));

            canvas.getChildren().add(0, edge);
        }
    }

    private void rebindEdgesRec(TreeNode n) {
        if (n == null) return;
        rebindEdge(n);
        rebindEdgesRec(n.left);
        rebindEdgesRec(n.right);
    }

    private void toggleBalanceLabels(boolean visible) {
        toggleBalanceLabelsRec(root, visible);
    }

    private void toggleBalanceLabelsRec(TreeNode n, boolean visible) {
        if (n == null) return;
        n.balanceLabel.setVisible(visible);
        toggleBalanceLabelsRec(n.left, visible);
        toggleBalanceLabelsRec(n.right, visible);
    }

    private void showRotationOverlay(TreeNode node, String text) {
        removeRotationOverlayImmediate();

        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        label.setTextFill(Color.WHITE);
        label.setStyle(
            "-fx-background-color: rgba(15, 23, 42, 0.95);" +
            "-fx-background-radius: 6;" +
            "-fx-border-color: #f1c40f;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 6;" +
            "-fx-padding: 6 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 2);"
        );

        label.layoutXProperty().bind(node.circle.centerXProperty().subtract(label.widthProperty().divide(2.0)));
        label.layoutYProperty().bind(node.circle.centerYProperty().subtract(60));

        label.setOpacity(0);
        label.setScaleX(0.85);
        label.setScaleY(0.85);
        canvas.getChildren().add(label);

        FadeTransition ft = new FadeTransition(Duration.millis(250), label);
        ft.setFromValue(0);
        ft.setToValue(1);

        ScaleTransition st = new ScaleTransition(Duration.millis(250), label);
        st.setFromX(0.85); st.setFromY(0.85);
        st.setToX(1.0); st.setToY(1.0);

        ParallelTransition pt = new ParallelTransition(ft, st);
        registerAnimation(pt);
        pt.play();

        activeRotationOverlay = label;
    }

    private void removeRotationOverlay() {
        if (activeRotationOverlay != null) {
            Label temp = activeRotationOverlay;
            activeRotationOverlay = null;

            FadeTransition ft = new FadeTransition(Duration.millis(200), temp);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);

            ScaleTransition st = new ScaleTransition(Duration.millis(200), temp);
            st.setFromX(1.0); st.setFromY(1.0);
            st.setToX(0.85); st.setToY(0.85);

            ParallelTransition pt = new ParallelTransition(ft, st);
            pt.setOnFinished(e -> canvas.getChildren().remove(temp));
            registerAnimation(pt);
            pt.play();
        }
    }

    private void removeRotationOverlayImmediate() {
        if (activeRotationOverlay != null) {
            canvas.getChildren().remove(activeRotationOverlay);
            activeRotationOverlay = null;
        }
    }

    public void stopAllTimelines() {
        for (javafx.animation.Animation anim : activeAnimations) {
            if (anim != null) anim.stop();
        }
        activeAnimations.clear();

        for (Timeline tl : activeTimelines) {
            if (tl != null) tl.stop();
        }
        activeTimelines.clear();

        removeRotationOverlayImmediate();
        resetVisuals();
    }

    private void resetVisuals() {
        resetColors();
        resetScales(root);
    }

    private void pulseScale(TreeNode node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(350), node.circle);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.2); st.setToY(1.2);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();

        ScaleTransition stLbl = new ScaleTransition(Duration.millis(350), node.label);
        stLbl.setFromX(1.0); stLbl.setFromY(1.0);
        stLbl.setToX(1.15); stLbl.setToY(1.15);
        stLbl.setAutoReverse(true);
        stLbl.setCycleCount(2);
        stLbl.play();
    }

    private void highlightRotationNodes(TreeNode node1, TreeNode node2, TreeNode node3) {
        resetColors();
        if (node1 != null) {
            node1.circle.setFill(Color.web("#e67e22"));
            node1.circle.setStroke(Color.web("#ff9f43"));
            node1.circle.setStrokeWidth(3.5);
            node1.circle.setEffect(new DropShadow(20, Color.web("#ff9f43")));
            pulseScale(node1);
        }
        if (node2 != null) {
            node2.circle.setFill(Color.web("#d35400"));
            node2.circle.setStroke(Color.web("#ff9f43"));
            node2.circle.setStrokeWidth(3.5);
            node2.circle.setEffect(new DropShadow(20, Color.web("#ff9f43")));
            pulseScale(node2);
        }
        if (node3 != null) {
            node3.circle.setFill(Color.web("#f1c40f"));
            node3.circle.setStroke(Color.web("#ffeaa7"));
            node3.circle.setStrokeWidth(3.5);
            node3.circle.setEffect(new DropShadow(20, Color.web("#f1c40f")));
            pulseScale(node3);
        }
    }

    private void checkAndRotateStep(TreeNode curr, Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
        if (curr == null) {
            recomputeHeights(root);
            rebindEdgesRec(root);
            relayout();
            if (onFinished != null) onFinished.run();
            return;
        }

        updateHeight(curr);
        int balance = getBalance(curr);

        if (balance >= -1 && balance <= 1) {
            checkAndRotateStep(curr.parent, onFinished, sessionId);
            return;
        }

        // Imbalance detected
        TreeNode child = null;
        TreeNode grandchild = null;
        String caseName = "";

        if (balance > 1) {
            child = curr.left;
            if (getBalance(child) >= 0) {
                caseName = "Left-Left Case (Rotate Right)";
                grandchild = child.left;
            } else {
                caseName = "Left-Right Case (Double Rotation)";
                grandchild = child.right;
            }
        } else {
            child = curr.right;
            if (getBalance(child) <= 0) {
                caseName = "Right-Right Case (Rotate Left)";
                grandchild = child.right;
            } else {
                caseName = "Right-Left Case (Double Rotation)";
                grandchild = child.left;
            }
        }

        TreeNode finalChild = child;
        TreeNode finalGrandchild = grandchild;
        String finalCaseName = caseName;
        int unbalancedVal = curr.value;

        status("⚠️ Imbalance detected at " + unbalancedVal + " (BF = " + balance + "). " + finalCaseName + ".");
        highlightRotationNodes(curr, child, grandchild);
        showRotationOverlay(curr, finalCaseName);

        double delayMs = getExecDelay();
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> {
            if (sessionId != treeSessionId) return;
            TreeNode newSubtreeRoot = null;
            if (balance > 1) {
                if (getBalance(finalChild) >= 0) {
                    newSubtreeRoot = rotateRight(curr);
                    removeRotationOverlay();
                } else {
                    status("⚠️ Left-Right Case: Step 1 - Left Rotating Child " + finalChild.value);
                    showRotationOverlay(curr, "LR Case: Step 1 - Left Rotate Child " + finalChild.value);
                    curr.left = rotateLeft(finalChild);
                    rebindEdgesRec(root);
                    relayout();

                    PauseTransition secondPause = new PauseTransition(Duration.millis(delayMs));
                    TreeNode finalCurr = curr;
                    secondPause.setOnFinished(e2 -> {
                        if (sessionId != treeSessionId) return;
                        status("⚠️ Left-Right Case: Step 2 - Right Rotating Root " + unbalancedVal);
                        showRotationOverlay(finalCurr, "LR Case: Step 2 - Right Rotate Root " + unbalancedVal);
                        highlightRotationNodes(finalCurr, finalCurr.left, finalCurr.left.left);
                        PauseTransition thirdPause = new PauseTransition(Duration.millis(delayMs));
                        thirdPause.setOnFinished(e3 -> {
                            if (sessionId != treeSessionId) return;
                            TreeNode rotNode = rotateRight(finalCurr);
                            rebindEdgesRec(root);
                            relayout();
                            removeRotationOverlay();
                            checkAndRotateStep(rotNode.parent, onFinished, sessionId);
                        });
                        registerAnimation(thirdPause);
                        thirdPause.play();
                    });
                    registerAnimation(secondPause);
                    secondPause.play();
                    return;
                }
            } else {
                if (getBalance(finalChild) <= 0) {
                    newSubtreeRoot = rotateLeft(curr);
                    removeRotationOverlay();
                } else {
                    status("⚠️ Right-Left Case: Step 1 - Right Rotating Child " + finalChild.value);
                    showRotationOverlay(curr, "RL Case: Step 1 - Right Rotate Child " + finalChild.value);
                    curr.right = rotateRight(finalChild);
                    rebindEdgesRec(root);
                    relayout();

                    PauseTransition secondPause = new PauseTransition(Duration.millis(delayMs));
                    TreeNode finalCurr = curr;
                    secondPause.setOnFinished(e2 -> {
                        if (sessionId != treeSessionId) return;
                        status("⚠️ Right-Left Case: Step 2 - Left Rotating Root " + unbalancedVal);
                        showRotationOverlay(finalCurr, "RL Case: Step 2 - Left Rotate Root " + unbalancedVal);
                        highlightRotationNodes(finalCurr, finalCurr.right, finalCurr.right.right);
                        PauseTransition thirdPause = new PauseTransition(Duration.millis(delayMs));
                        thirdPause.setOnFinished(e3 -> {
                            if (sessionId != treeSessionId) return;
                            TreeNode rotNode = rotateLeft(finalCurr);
                            rebindEdgesRec(root);
                            relayout();
                            removeRotationOverlay();
                            checkAndRotateStep(rotNode.parent, onFinished, sessionId);
                        });
                        registerAnimation(thirdPause);
                        thirdPause.play();
                    });
                    registerAnimation(secondPause);
                    secondPause.play();
                    return;
                }
            }

            rebindEdgesRec(root);
            relayout();
            checkAndRotateStep(newSubtreeRoot.parent, onFinished, sessionId);
        });
        registerAnimation(pause);
        pause.play();
    }

    private void balanceTreeAfterInsert(TreeNode newNode, Runnable onFinished, int sessionId) {
        checkAndRotateStep(newNode.parent, onFinished, sessionId);
    }

    private void balanceTreeAfterDelete(TreeNode startNode, Runnable onFinished, int sessionId) {
        checkAndRotateStep(startNode, onFinished, sessionId);
    }

    private void balanceTreeAfterInsertRB(TreeNode newNode, Runnable onFinished, int sessionId) {
        checkAndBalanceRBStep(newNode, onFinished, sessionId);
    }

    private void highlightRBNodes(TreeNode n, TreeNode p, TreeNode u, TreeNode g) {
        resetColors();
        if (n != null) {
            n.circle.setStroke(Color.web("#f1c40f")); // Gold for child
            n.circle.setStrokeWidth(3.5);
        }
        if (p != null) {
            p.circle.setStroke(Color.web("#f1c40f")); // Gold for parent
            p.circle.setStrokeWidth(3.5);
        }
        if (u != null) {
            u.circle.setStroke(Color.web("#2ecc71")); // Green for uncle
            u.circle.setStrokeWidth(3.5);
        }
        if (g != null) {
            g.circle.setStroke(Color.web("#9b5de5")); // Purple for grandparent
            g.circle.setStrokeWidth(3.5);
        }
        flashViolatingEdge(n);
        flashViolatingNodes(n, p);
    }

    private void flashViolatingEdge(TreeNode child) {
        if (child == null || child.edgeToParent == null) return;
        Line edge = child.edgeToParent;
        edge.setStroke(Color.web("#ff3333"));
        edge.setStrokeWidth(4.0);

        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(edge.strokeProperty(), Color.web("#ff3333")), 
                new KeyValue(edge.strokeWidthProperty(), 4.0)
            ),
            new KeyFrame(Duration.millis(300), 
                new KeyValue(edge.strokeProperty(), Color.web("#ffb3b3")), 
                new KeyValue(edge.strokeWidthProperty(), 2.5)
            ),
            new KeyFrame(Duration.millis(600), 
                new KeyValue(edge.strokeProperty(), Color.web("#ff3333")), 
                new KeyValue(edge.strokeWidthProperty(), 4.0)
            )
        );
        tl.setCycleCount(3);
        activeTimelines.add(tl);
        tl.setOnFinished(e -> {
            activeTimelines.remove(tl);
            edge.setStroke(Color.web("#ff3333"));
            edge.setStrokeWidth(3.5);
        });
        tl.play();
    }

    private void flashViolatingNodes(TreeNode n, TreeNode p) {
        if (n == null || p == null) return;

        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(n.circle.strokeProperty(), Color.web("#ff3333")), 
                new KeyValue(p.circle.strokeProperty(), Color.web("#ff3333")),
                new KeyValue(n.circle.strokeWidthProperty(), 3.5),
                new KeyValue(p.circle.strokeWidthProperty(), 3.5)
            ),
            new KeyFrame(Duration.millis(300), 
                new KeyValue(n.circle.strokeProperty(), Color.web("#ffffff")), 
                new KeyValue(p.circle.strokeProperty(), Color.web("#ffffff")),
                new KeyValue(n.circle.strokeWidthProperty(), 2.0),
                new KeyValue(p.circle.strokeWidthProperty(), 2.0)
            ),
            new KeyFrame(Duration.millis(600), 
                new KeyValue(n.circle.strokeProperty(), Color.web("#ff3333")), 
                new KeyValue(p.circle.strokeProperty(), Color.web("#ff3333")),
                new KeyValue(n.circle.strokeWidthProperty(), 3.5),
                new KeyValue(p.circle.strokeWidthProperty(), 3.5)
            )
        );
        tl.setCycleCount(3);
        activeTimelines.add(tl);
        tl.setOnFinished(e -> {
            activeTimelines.remove(tl);
            n.circle.setStroke(Color.web("#ff3333"));
            n.circle.setStrokeWidth(3.5);
            p.circle.setStroke(Color.web("#ff3333"));
            p.circle.setStrokeWidth(3.5);
        });
        tl.play();
    }

    private void checkAndBalanceRBStep(TreeNode n, Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
        if (n == null) {
            if (root != null && root.isRed) {
                root.isRed = false;
                resetColors();
            }
            rebindEdgesRec(root);
            relayout();
            removeRotationOverlay();
            if (onFinished != null) onFinished.run();
            return;
        }

        if (root != null && root.isRed) {
            root.isRed = false;
            resetColors();
        }

        // Check for double red violation
        if (n.isRed && n.parent != null && n.parent.isRed) {
            TreeNode p = n.parent;
            TreeNode g = p.parent; // Must exist since p is Red and root is always Black

            if (g == null) {
                p.isRed = false;
                resetColors();
                checkAndBalanceRBStep(p, onFinished, sessionId);
                return;
            }

            TreeNode u = (g.left == p) ? g.right : g.left;

            // Case 1: Uncle is RED
            if (u != null && u.isRed) {
                status("⚠️ Double Red Violation (Parent " + p.value + " & Uncle " + u.value + "). Recoloring...");
                highlightRBNodes(n, p, u, g);
                showRotationOverlay(g, "Uncle is Red: Recolor parent, uncle, and grandparent");

                double delayMs = getExecDelay();
                PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
                pause.setOnFinished(e -> {
                    if (sessionId != treeSessionId) return;
                    p.isRed = false;
                    u.isRed = false;
                    g.isRed = true;
                    resetColors();
                    removeRotationOverlay();
                    checkAndBalanceRBStep(g, onFinished, sessionId);
                });
                registerAnimation(pause);
                pause.play();
                return;
            }

            // Case 2: Uncle is BLACK or null -> Rotate & Recolor
            status("⚠️ Double Red Violation (Black/Null Uncle). Rotating and recoloring...");
            highlightRBNodes(n, p, u, g);
            showRotationOverlay(g, "Uncle is Black/Null: Rotate & Recolor");

            double delayMs = getExecDelay();
            PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
            pause.setOnFinished(e -> {
                if (sessionId != treeSessionId) return;
                if (g.left == p) {
                    if (p.right == n) {
                        // LR Case
                        status("⚠️ Left-Right Case: Rotating left at " + p.value);
                        showRotationOverlay(p, "LR Case: Rotate Left at " + p.value);
                        rotateLeft(p);
                        rebindEdgesRec(root);
                        relayout();

                        PauseTransition pause2 = new PauseTransition(Duration.millis(delayMs));
                        pause2.setOnFinished(e2 -> {
                            if (sessionId != treeSessionId) return;
                            status("⚠️ Left-Right Case: Final right rotation at " + g.value + " & recoloring.");
                            showRotationOverlay(g, "LR Case: Rotate Right at " + g.value + " & Recolor");
                            TreeNode rotNode = rotateRight(g);
                            rotNode.isRed = false;
                            if (rotNode.left != null) rotNode.left.isRed = true;
                            if (rotNode.right != null) rotNode.right.isRed = true;
                            resetColors();
                            rebindEdgesRec(root);
                            relayout();
                            removeRotationOverlay();
                            checkAndBalanceRBStep(rotNode.parent, onFinished, sessionId);
                        });
                        registerAnimation(pause2);
                        pause2.play();
                    } else {
                        // LL Case
                        status("⚠️ Left-Left Case: Rotating right at " + g.value + " & recoloring.");
                        showRotationOverlay(g, "LL Case: Rotate Right at " + g.value + " & Recolor");
                        TreeNode rotNode = rotateRight(g);
                        rotNode.isRed = false;
                        if (rotNode.left != null) rotNode.left.isRed = true;
                        if (rotNode.right != null) rotNode.right.isRed = true;
                        resetColors();
                        rebindEdgesRec(root);
                        relayout();
                        removeRotationOverlay();
                        checkAndBalanceRBStep(rotNode.parent, onFinished, sessionId);
                    }
                } else {
                    if (p.left == n) {
                        // RL Case
                        status("⚠️ Right-Left Case: Rotating right at " + p.value);
                        showRotationOverlay(p, "RL Case: Rotate Right at " + p.value);
                        rotateRight(p);
                        rebindEdgesRec(root);
                        relayout();

                        PauseTransition pause2 = new PauseTransition(Duration.millis(delayMs));
                        pause2.setOnFinished(e2 -> {
                            if (sessionId != treeSessionId) return;
                            status("⚠️ Right-Left Case: Final left rotation at " + g.value + " & recoloring.");
                            showRotationOverlay(g, "RL Case: Rotate Left at " + g.value + " & Recolor");
                            TreeNode rotNode = rotateLeft(g);
                            rotNode.isRed = false;
                            if (rotNode.left != null) rotNode.left.isRed = true;
                            if (rotNode.right != null) rotNode.right.isRed = true;
                            resetColors();
                            rebindEdgesRec(root);
                            relayout();
                            removeRotationOverlay();
                            checkAndBalanceRBStep(rotNode.parent, onFinished, sessionId);
                        });
                        registerAnimation(pause2);
                        pause2.play();
                    } else {
                        // RR Case
                        status("⚠️ Right-Right Case: Rotating left at " + g.value + " & recoloring.");
                        showRotationOverlay(g, "RR Case: Rotate Left at " + g.value + " & Recolor");
                        TreeNode rotNode = rotateLeft(g);
                        rotNode.isRed = false;
                        if (rotNode.left != null) rotNode.left.isRed = true;
                        if (rotNode.right != null) rotNode.right.isRed = true;
                        resetColors();
                        rebindEdgesRec(root);
                        relayout();
                        removeRotationOverlay();
                        checkAndBalanceRBStep(rotNode.parent, onFinished, sessionId);
                    }
                }
            });
            registerAnimation(pause);
            pause.play();
            return;
        }

        // No violation, move up to parent
        checkAndBalanceRBStep(n.parent, onFinished, sessionId);
    }

    private void stepwiseBalanceBST(Runnable onFinished) {
        stepwiseBalanceBST(onFinished, treeSessionId);
    }

    private void stepwiseBalanceBST(Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
        recomputeHeights(root);
        TreeNode unbalanced = findFirstUnbalanced(root);
        if (unbalanced == null) {
            status("Tree is fully balanced!");
            rebindEdgesRec(root);
            relayout();
            if (onFinished != null) onFinished.run();
            return;
        }

        checkAndRotateStep(unbalanced, () -> {
            if (sessionId == treeSessionId) {
                stepwiseBalanceBST(onFinished, sessionId);
            }
        }, sessionId);
    }

    private TreeNode findFirstUnbalanced(TreeNode n) {
        if (n == null) return null;
        TreeNode leftRes = findFirstUnbalanced(n.left);
        if (leftRes != null) return leftRes;

        TreeNode rightRes = findFirstUnbalanced(n.right);
        if (rightRes != null) return rightRes;

        int bf = getBalance(n);
        if (bf < -1 || bf > 1) return n;

        return null;
    }

    // ── Node Inspector Helpers ──────────────────────────────────

    private int getDepth(TreeNode node) {
        int depth = 0;
        TreeNode cur = node;
        while (cur.parent != null) {
            depth++;
            cur = cur.parent;
        }
        return depth;
    }

    private VBox buildInspectorCard() {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12));
        card.setPrefSize(180, 155);
        card.setMinSize(180, 155);
        card.setMaxSize(180, 155);
        card.setStyle(
            "-fx-background-color: rgba(12, 18, 54, 0.85);" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #1e2c56;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 8;"
        );

        inspTitle = new Label("🌳 NODE INSPECTOR");
        inspTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        inspTitle.setTextFill(Color.web(Theme.TEAL));
        inspTitle.setStyle("");

        inspValue = new Label("Hover over a node");
        inspValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        inspValue.setTextFill(Color.WHITE);

        inspHeight = new Label("to inspect tree properties");
        inspHeight.setFont(Font.font("Segoe UI", 12));
        inspHeight.setTextFill(Color.web("#7a90c0"));

        inspDepth = new Label("and node details.");
        inspDepth.setFont(Font.font("Segoe UI", 12));
        inspDepth.setTextFill(Color.web("#7a90c0"));

        inspBF = new Label("");
        inspBF.setFont(Font.font("Segoe UI", 12));
        inspBF.setTextFill(Color.web("#7a90c0"));

        inspParent = new Label("");
        inspParent.setFont(Font.font("Segoe UI", 12));
        inspParent.setTextFill(Color.web("#7a90c0"));

        inspChildren = new Label("");
        inspChildren.setFont(Font.font("Segoe UI", 12));
        inspChildren.setTextFill(Color.web("#7a90c0"));

        card.getChildren().addAll(inspTitle, inspValue, inspHeight, inspDepth, inspBF, inspParent, inspChildren);
        return card;
    }

    private void updateInspector(TreeNode node) {
        if (node == null) {
            clearInspector();
            return;
        }

        inspectorCard.setStyle(
            "-fx-background-color: rgba(12, 18, 54, 0.95);" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + accentColorHex + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 8;"
        );

        inspValue.setText("Value: " + node.value);
        inspHeight.setText("Height: " + height(node));
        inspDepth.setText("Depth: " + getDepth(node));

        int bf = getBalance(node);
        if (currentMode.equals("Red-Black")) {
            inspBF.setText("Color: " + (node.isRed ? "RED 🔴" : "BLACK ⚫"));
        } else {
            inspBF.setText("Balance Factor: " + (bf >= 0 ? "+" : "") + bf);
        }
        inspParent.setText("Parent: " + (node.parent != null ? String.valueOf(node.parent.value) : "None"));

        String childrenStr = "Children: ";
        if (node.left == null && node.right == null) {
            childrenStr += "None";
        } else {
            childrenStr += (node.left != null ? "L:" + node.left.value : "") +
                           (node.left != null && node.right != null ? ", " : "") +
                           (node.right != null ? "R:" + node.right.value : "");
        }
        inspChildren.setText(childrenStr);
    }

    private void clearInspector() {
        if (inspectorCard == null) return;

        inspectorCard.setStyle(
            "-fx-background-color: rgba(12, 18, 54, 0.85);" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #1e2c56;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 8;"
        );

        inspValue.setText("Hover over a node");
        inspHeight.setText("to inspect tree properties");
        inspDepth.setText("and node details.");
        inspBF.setText("");
        inspParent.setText("");
        inspChildren.setText("");
    }

    private void convertToRedBlackStepwise(Runnable onFinished) {
        convertToRedBlackStepwise(onFinished, treeSessionId);
    }

    private void convertToRedBlackStepwise(Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
        java.util.List<Integer> values = new ArrayList<>();
        collectValuesInOrder(root, values);

        canvas.getChildren().clear();
        root = null;
        clearInspector();

        if (values.isEmpty()) {
            if (onFinished != null) onFinished.run();
            return;
        }

        insertSequentialRB(values, 0, onFinished, sessionId);
    }

    private void insertSequentialRB(java.util.List<Integer> values, int idx, Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
        if (idx >= values.size()) {
            if (onFinished != null) onFinished.run();
            return;
        }
        insert(values.get(idx), () -> {
            if (sessionId != treeSessionId) return;
            PauseTransition nextPause = new PauseTransition(Duration.millis(getExecDelay()));
            nextPause.setOnFinished(e -> {
                if (sessionId == treeSessionId) {
                    insertSequentialRB(values, idx + 1, onFinished, sessionId);
                }
            });
            nextPause.play();
        }, sessionId);
    }

    private void collectValuesInOrder(TreeNode n, java.util.List<Integer> list) {
        if (n == null) return;
        collectValuesInOrder(n.left, list);
        list.add(n.value);
        collectValuesInOrder(n.right, list);
    }

    // ═══════════════════════════════════════════════════════════
    //  2-3 Tree (B-Tree) Visualization & Engine
    // ═══════════════════════════════════════════════════════════

    private void bTreeRelayout() {
        if (bTreeRoot == null) return;

        // 1. Find all leaves and lay them out from left to right
        java.util.List<BTreeNode> leaves = new java.util.ArrayList<>();
        findBTreeLeaves(bTreeRoot, leaves);

        double gap = 40.0; // gap between nodes
        double totalLeavesWidth = 0;
        for (int i = 0; i < leaves.size(); i++) {
            BTreeNode leaf = leaves.get(i);
            double w = leaf.keys.size() * 50.0;
            totalLeavesWidth += w;
            if (i < leaves.size() - 1) {
                totalLeavesWidth += gap;
            }
        }

        double nextX = 2000.0 - totalLeavesWidth / 2.0;
        for (BTreeNode leaf : leaves) {
            double w = leaf.keys.size() * 50.0;
            leaf.targetX = nextX + w / 2.0;
            leaf.targetY = ROOT_Y + getBTreeDepth(bTreeRoot, leaf) * VERTICAL_GAP;
            nextX += w + gap;
        }

        // 2. Compute non-leaf absolute positions bottom-up / recursively
        assignBTreeNonLeafCoordinates(bTreeRoot);

        // 3. Animate to targets and update edges
        animateBTreeToTargets(bTreeRoot);
        updateBTreeEdges(bTreeRoot);
    }

    private void findBTreeLeaves(BTreeNode node, java.util.List<BTreeNode> leaves) {
        if (node == null) return;
        if (node.isLeaf()) {
            leaves.add(node);
            return;
        }
        for (BTreeNode child : node.children) {
            findBTreeLeaves(child, leaves);
        }
    }

    private int getBTreeDepth(BTreeNode rootNode, BTreeNode node) {
        int depth = 0;
        BTreeNode cur = node;
        while (cur != rootNode && cur != null) {
            depth++;
            cur = cur.parent;
        }
        return depth;
    }

    private void assignBTreeNonLeafCoordinates(BTreeNode node) {
        if (node == null || node.isLeaf()) return;

        // Recursively layout children first
        for (BTreeNode child : node.children) {
            assignBTreeNonLeafCoordinates(child);
        }

        // Parent X is average of children's X coordinates
        double sumX = 0;
        for (BTreeNode child : node.children) {
            sumX += child.targetX;
        }
        node.targetX = sumX / node.children.size();
        
        // Parent Y based on its depth
        node.targetY = ROOT_Y + getBTreeDepth(bTreeRoot, node) * VERTICAL_GAP;
    }

    private void animateBTreeToTargets(BTreeNode node) {
        if (node == null) return;

        double startX = node.currentX.get();
        double startY = node.currentY.get();
        double targetX = node.targetX;
        double targetY = node.targetY;

        if (Math.abs(startX - targetX) < 0.1 && Math.abs(startY - targetY) < 0.1) {
            for (BTreeNode child : node.children) {
                animateBTreeToTargets(child);
            }
            return;
        }

        double distance = Math.abs(targetX - startX);
        double maxHop = Math.min(45.0, distance * 0.25);

        javafx.animation.Transition transition = new javafx.animation.Transition() {
            {
                setCycleDuration(Duration.millis(getAnimMs()));
                setInterpolator(Interpolator.EASE_BOTH);
            }
            @Override
            protected void interpolate(double frac) {
                double x = startX + (targetX - startX) * frac;
                double hop = -maxHop * Math.sin(Math.PI * frac);
                double y = startY + (targetY - startY) * frac + hop;
                node.currentX.set(x);
                node.currentY.set(y);
            }
        };
        registerAnimation(transition);
        transition.play();

        for (BTreeNode child : node.children) {
            animateBTreeToTargets(child);
        }
    }

    private void updateBTreeEdges(BTreeNode node) {
        if (node == null) return;

        for (int i = 0; i < node.children.size(); i++) {
            BTreeNode child = node.children.get(i);
            if (child.edgeToParent != null) {
                canvas.getChildren().remove(child.edgeToParent);
            }

            // Neon back line (border/glow)
            Line backEdge = new Line();
            backEdge.setStroke(Color.web(accentColorHex));
            backEdge.setStrokeWidth(5.0);
            backEdge.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            
            // Add a neon glow filter to the back edge
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow(4, Color.web(accentColorHex));
            backEdge.setEffect(glow);

            // Dark front line (core)
            Line frontEdge = new Line();
            frontEdge.setStroke(Color.web("#0a0f2e"));
            frontEdge.setStrokeWidth(2.0);
            frontEdge.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

            // Bind start and end properties for both lines
            int idx = i;
            javafx.beans.value.ObservableDoubleValue startX = node.currentX.subtract(node.bgRect.widthProperty().divide(2)).add(idx * 50);
            javafx.beans.value.ObservableDoubleValue startY = node.currentY.add(20);
            javafx.beans.value.ObservableDoubleValue endX = child.currentX;
            javafx.beans.value.ObservableDoubleValue endY = child.currentY.subtract(20);

            backEdge.startXProperty().bind(startX);
            backEdge.startYProperty().bind(startY);
            backEdge.endXProperty().bind(endX);
            backEdge.endYProperty().bind(endY);

            frontEdge.startXProperty().bind(startX);
            frontEdge.startYProperty().bind(startY);
            frontEdge.endXProperty().bind(endX);
            frontEdge.endYProperty().bind(endY);

            // Wrap in a Group
            javafx.scene.Group edgeGroup = new javafx.scene.Group(backEdge, frontEdge);
            child.edgeToParent = edgeGroup;

            canvas.getChildren().add(0, edgeGroup);

            updateBTreeEdges(child);
        }
    }

    private void bTreeInsert(int value, Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
        if (bTreeRoot == null) {
            bTreeRoot = new BTreeNode(value);
            bTreeRoot.placeAt(2000, ROOT_Y);
            bTreeRoot.redrawNode(accentColorHex);
            canvas.getChildren().add(bTreeRoot.group);
            status("Inserted root: " + value);
            bTreeRelayout();
            if (onFinished != null) onFinished.run();
            return;
        }

        BTreeNode leaf = findLeafForInsert(bTreeRoot, value);
        if (leaf.keys.contains(value)) {
            status("Duplicate value " + value + " — not inserted");
            if (onFinished != null) onFinished.run();
            return;
        }

        List<BTreeNode> path = getBTreePath(bTreeRoot, value);
        double stepMs = 2400.0 / animationSpeed;

        animateBTreePath(path, stepMs, Color.web("#f39c12"), () -> {
            if (sessionId != treeSessionId) return;
            insertKeySorted(leaf, value);
            leaf.redrawNode(accentColorHex);
            bTreeRelayout();

            checkAndSplitBTree(leaf, value, onFinished, sessionId);
        });
    }

    private void bTreeSearch(int value, java.util.function.Consumer<Boolean> onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
        if (bTreeRoot == null) {
            status("Tree is empty");
            if (onFinished != null) onFinished.accept(false);
            return;
        }

        List<BTreeNode> path = getBTreePath(bTreeRoot, value);
        BTreeNode last = path.get(path.size() - 1);
        boolean found = last.keys.contains(value);

        double stepMs = 2400.0 / animationSpeed;
        Color highlightColor = found ? Color.web("#10b981") : Color.web("#ef4444");

        animateBTreePath(path, stepMs, highlightColor, () -> {
            if (sessionId != treeSessionId) return;
            status(found ? "Found " + value : "Value " + value + " not in tree");
            highlightBTreeNode(last, highlightColor, 1000);
            if (onFinished != null) onFinished.accept(found);
        });
    }

    private List<BTreeNode> getBTreePath(BTreeNode node, int value) {
        List<BTreeNode> path = new ArrayList<>();
        BTreeNode cur = node;
        while (cur != null) {
            path.add(cur);
            if (cur.isLeaf()) break;
            int idx = 0;
            while (idx < cur.keys.size() && value > cur.keys.get(idx)) {
                idx++;
            }
            cur = cur.children.get(idx);
        }
        return path;
    }

    private BTreeNode findLeafForInsert(BTreeNode node, int value) {
        if (node.isLeaf()) return node;
        int idx = 0;
        while (idx < node.keys.size() && value > node.keys.get(idx)) {
            idx++;
        }
        return findLeafForInsert(node.children.get(idx), value);
    }

    private void insertKeySorted(BTreeNode node, int value) {
        int idx = 0;
        while (idx < node.keys.size() && value > node.keys.get(idx)) {
            idx++;
        }
        node.keys.add(idx, value);
    }

    private void checkAndSplitBTree(BTreeNode node, int insertedValue, Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
        if (node.keys.size() <= 2) {
            bTreeRelayout();
            BTreeNode targetNode = findBTreeNode(bTreeRoot, insertedValue);
            if (targetNode != null) {
                highlightBTreeNode(targetNode, Color.web("#10b981"), 800);
            }
            if (onFinished != null) onFinished.run();
            return;
        }

        node.bgRect.setStroke(Color.web("#ef4444"));
        node.bgRect.setStrokeWidth(3.5);
        status("Node overflow (" + node.keys + "). Splitting and pushing up middle key: " + node.keys.get(1));

        PauseTransition pause = new PauseTransition(Duration.millis(getExecDelay()));
        pause.setOnFinished(e -> {
            if (sessionId != treeSessionId) return;

            int midVal = node.keys.get(1);
            int leftVal = node.keys.get(0);
            int rightVal = node.keys.get(2);

            BTreeNode parent = node.parent;
            BTreeNode sibling = new BTreeNode(rightVal);
            sibling.parent = parent;

            if (!node.isLeaf()) {
                sibling.children.add(node.children.get(2));
                sibling.children.add(node.children.get(3));
                node.children.get(2).parent = sibling;
                node.children.get(3).parent = sibling;

                node.children.remove(3);
                node.children.remove(2);
            }

            node.keys.clear();
            node.keys.add(leftVal);
            node.redrawNode(accentColorHex);

            sibling.redrawNode(accentColorHex);
            canvas.getChildren().add(sibling.group);

            sibling.currentX.set(node.currentX.get() + 50);
            sibling.currentY.set(node.currentY.get());

            if (parent == null) {
                BTreeNode newRoot = new BTreeNode(midVal);
                newRoot.children.add(node);
                newRoot.children.add(sibling);
                node.parent = newRoot;
                sibling.parent = newRoot;

                newRoot.placeAt(2000, ROOT_Y);
                newRoot.redrawNode(accentColorHex);
                canvas.getChildren().add(newRoot.group);
                bTreeRoot = newRoot;

                status("Created new root: " + midVal);
                bTreeRelayout();
                BTreeNode targetNode = findBTreeNode(bTreeRoot, insertedValue);
                if (targetNode != null) {
                    highlightBTreeNode(targetNode, Color.web("#10b981"), 800);
                }
                if (onFinished != null) onFinished.run();
            } else {
                int idx = parent.children.indexOf(node);
                parent.keys.add(idx, midVal);
                parent.children.add(idx + 1, sibling);
                parent.redrawNode(accentColorHex);

                bTreeRelayout();
                checkAndSplitBTree(parent, insertedValue, onFinished, sessionId);
            }
        });
        registerAnimation(pause);
        pause.play();
    }

    private void animateBTreePath(List<BTreeNode> path, double stepMs, Color highlightColor, Runnable onFinished) {
        if (path.isEmpty()) {
            if (onFinished != null) onFinished.run();
            return;
        }
        stopAllTimelines();
        resetBTreeColors(bTreeRoot);

        Timeline tl = new Timeline();
        activeTimelines.add(tl);
        tl.setOnFinished(e -> activeTimelines.remove(tl));

        for (int i = 0; i < path.size(); i++) {
            BTreeNode n = path.get(i);
            int idx = i;
            tl.getKeyFrames().add(new KeyFrame(
                Duration.millis(stepMs * i),
                e -> {
                    n.bgRect.setStroke(highlightColor);
                    n.bgRect.setStrokeWidth(3.5);
                    if (idx > 0) {
                        BTreeNode prev = path.get(idx - 1);
                        prev.bgRect.setStroke(Color.web(accentColorHex));
                        prev.bgRect.setStrokeWidth(2);
                    }
                }
            ));
        }

        tl.getKeyFrames().add(new KeyFrame(
            Duration.millis(stepMs * path.size()),
            e -> {
                if (onFinished != null) onFinished.run();
            }
        ));
        tl.play();
    }

    private void highlightBTreeNode(BTreeNode node, Color color, double durationMs) {
        node.bgRect.setStroke(color);
        node.bgRect.setStrokeWidth(4);

        PauseTransition p = new PauseTransition(Duration.millis(durationMs));
        p.setOnFinished(e -> {
            node.bgRect.setStroke(Color.web(accentColorHex));
            node.bgRect.setStrokeWidth(2);
        });
        registerAnimation(p);
        p.play();
    }

    private void resetBTreeColors(BTreeNode node) {
        if (node == null) return;
        node.bgRect.setStroke(Color.web(accentColorHex));
        node.bgRect.setStrokeWidth(2);
        for (BTreeNode child : node.children) {
            resetBTreeColors(child);
        }
    }

    private void insertBTreeSequence(List<Integer> vals, int index, int sessionId) {
        if (sessionId != treeSessionId) return;
        if (index >= vals.size()) {
            status("B-Tree batch insertion complete");
            return;
        }
        int val = vals.get(index);
        bTreeInsert(val, () -> {
            if (sessionId == treeSessionId) {
                PauseTransition pause = new PauseTransition(Duration.millis(getAnimMs() + 100));
                pause.setOnFinished(e -> {
                    if (sessionId == treeSessionId) {
                        insertBTreeSequence(vals, index + 1, sessionId);
                    }
                });
                registerAnimation(pause);
                pause.play();
            }
        }, sessionId);
    }

    private void bTreeDelete(int value, Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;
        if (bTreeRoot == null) {
            status("Tree is empty");
            if (onFinished != null) onFinished.run();
            return;
        }

        BTreeNode targetNode = findBTreeNode(bTreeRoot, value);
        if (targetNode == null) {
            status("Value " + value + " not in tree");
            if (onFinished != null) onFinished.run();
            return;
        }

        List<BTreeNode> path;
        if (targetNode.isLeaf()) {
            path = getBTreePathToKey(bTreeRoot, value);
        } else {
            int k = targetNode.keys.indexOf(value);
            path = getBTreePathToKey(bTreeRoot, value);
            BTreeNode cur = targetNode.children.get(k);
            while (cur != null) {
                path.add(cur);
                if (cur.isLeaf()) break;
                cur = cur.children.get(cur.children.size() - 1);
            }
        }

        double stepMs = 2400.0 / animationSpeed;
        animateBTreePath(path, stepMs, Color.web("#ef4444"), () -> {
            if (sessionId != treeSessionId) return;

            BTreeNode leafNode;
            if (targetNode.isLeaf()) {
                leafNode = targetNode;
                leafNode.keys.remove(Integer.valueOf(value));
            } else {
                int k = targetNode.keys.indexOf(value);
                BTreeNode predLeaf = path.get(path.size() - 1);
                int predKey = predLeaf.keys.get(predLeaf.keys.size() - 1);

                // Swap keys
                targetNode.keys.set(k, predKey);
                predLeaf.keys.remove(predLeaf.keys.size() - 1);

                targetNode.redrawNode(accentColorHex);
                leafNode = predLeaf;
            }

            leafNode.redrawNode(accentColorHex);
            bTreeRelayout();

            if (leafNode.keys.isEmpty()) {
                resolveBTreeUnderflow(leafNode, onFinished, sessionId);
            } else {
                status("Deleted: " + value);
                highlightBTreeNode(leafNode, Color.web("#10b981"), 800);
                if (onFinished != null) onFinished.run();
            }
        });
    }

    private List<BTreeNode> getBTreePathToKey(BTreeNode node, int value) {
        List<BTreeNode> path = new ArrayList<>();
        BTreeNode cur = node;
        while (cur != null) {
            path.add(cur);
            if (cur.keys.contains(value)) break;
            if (cur.isLeaf()) break;
            int idx = 0;
            while (idx < cur.keys.size() && value > cur.keys.get(idx)) {
                idx++;
            }
            cur = cur.children.get(idx);
        }
        return path;
    }

    private void resolveBTreeUnderflow(BTreeNode node, Runnable onFinished, int sessionId) {
        if (sessionId != treeSessionId) return;

        if (node == bTreeRoot) {
            if (node.keys.isEmpty()) {
                if (!node.children.isEmpty()) {
                    bTreeRoot = node.children.get(0);
                    bTreeRoot.parent = null;
                    if (bTreeRoot.edgeToParent != null) {
                        canvas.getChildren().remove(bTreeRoot.edgeToParent);
                        bTreeRoot.edgeToParent = null;
                    }
                    canvas.getChildren().remove(node.group);
                    if (node.edgeToParent != null) {
                        canvas.getChildren().remove(node.edgeToParent);
                    }
                    status("Root underflow: child becomes new root");
                } else {
                    bTreeRoot = null;
                    canvas.getChildren().remove(node.group);
                    status("Tree is empty");
                }
                bTreeRelayout();
            }
            if (onFinished != null) onFinished.run();
            return;
        }

        BTreeNode parent = node.parent;
        int idx = parent.children.indexOf(node);
        BTreeNode leftSib = (idx > 0) ? parent.children.get(idx - 1) : null;
        BTreeNode rightSib = (idx < parent.children.size() - 1) ? parent.children.get(idx + 1) : null;

        // 1. Borrow from Left Sibling
        if (leftSib != null && leftSib.keys.size() > 1) {
            status("Underflow: borrowing from left sibling");
            int parentKey = parent.keys.get(idx - 1);
            int borrowKey = leftSib.keys.remove(leftSib.keys.size() - 1);

            parent.keys.set(idx - 1, borrowKey);
            node.keys.add(0, parentKey);

            if (!leftSib.isLeaf()) {
                BTreeNode movedChild = leftSib.children.remove(leftSib.children.size() - 1);
                node.children.add(0, movedChild);
                movedChild.parent = node;
            }

            leftSib.redrawNode(accentColorHex);
            node.redrawNode(accentColorHex);
            parent.redrawNode(accentColorHex);
            bTreeRelayout();
            updateBTreeEdges(bTreeRoot);

            if (onFinished != null) onFinished.run();
            return;
        }

        // 2. Borrow from Right Sibling
        if (rightSib != null && rightSib.keys.size() > 1) {
            status("Underflow: borrowing from right sibling");
            int parentKey = parent.keys.get(idx);
            int borrowKey = rightSib.keys.remove(0);

            parent.keys.set(idx, borrowKey);
            node.keys.add(parentKey);

            if (!rightSib.isLeaf()) {
                BTreeNode movedChild = rightSib.children.remove(0);
                node.children.add(movedChild);
                movedChild.parent = node;
            }

            rightSib.redrawNode(accentColorHex);
            node.redrawNode(accentColorHex);
            parent.redrawNode(accentColorHex);
            bTreeRelayout();
            updateBTreeEdges(bTreeRoot);

            if (onFinished != null) onFinished.run();
            return;
        }

        // 3. Merge with Left Sibling
        if (leftSib != null) {
            status("Underflow: merging with left sibling");
            int parentKey = parent.keys.remove(idx - 1);
            parent.children.remove(node);

            leftSib.keys.add(parentKey);
            // Transfer children
            for (BTreeNode child : node.children) {
                leftSib.children.add(child);
                child.parent = leftSib;
            }

            canvas.getChildren().remove(node.group);
            if (node.edgeToParent != null) {
                canvas.getChildren().remove(node.edgeToParent);
            }

            leftSib.redrawNode(accentColorHex);
            parent.redrawNode(accentColorHex);
            bTreeRelayout();
            updateBTreeEdges(bTreeRoot);

            PauseTransition pause = new PauseTransition(Duration.millis(getExecDelay()));
            pause.setOnFinished(e -> {
                resolveBTreeUnderflow(parent, onFinished, sessionId);
            });
            registerAnimation(pause);
            pause.play();
            return;
        }

        // 4. Merge with Right Sibling (leftSib == null)
        if (rightSib != null) {
            status("Underflow: merging with right sibling");
            int parentKey = parent.keys.remove(idx);
            parent.children.remove(rightSib);

            node.keys.add(parentKey);
            node.keys.addAll(rightSib.keys);
            // Transfer children
            for (BTreeNode child : rightSib.children) {
                node.children.add(child);
                child.parent = node;
            }

            canvas.getChildren().remove(rightSib.group);
            if (rightSib.edgeToParent != null) {
                canvas.getChildren().remove(rightSib.edgeToParent);
            }

            node.redrawNode(accentColorHex);
            parent.redrawNode(accentColorHex);
            bTreeRelayout();
            updateBTreeEdges(bTreeRoot);

            PauseTransition pause = new PauseTransition(Duration.millis(getExecDelay()));
            pause.setOnFinished(e -> {
                resolveBTreeUnderflow(parent, onFinished, sessionId);
            });
            registerAnimation(pause);
            pause.play();
            return;
        }
    }
}
