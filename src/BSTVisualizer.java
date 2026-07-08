import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Binary Search Tree Visualizer with panning, zooming, and a Java code editor.
 */
public class BSTVisualizer extends Application {

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

    // ── Application Entry ──────────────────────────────────────
    @Override
    public void start(Stage stage) {

        // ── Canvas & Viewport (Pan/Zoom) ───────────────────────
        canvas = new Pane();
        // Use a large virtual size for the canvas so it acts as an infinite board
        canvas.setPrefSize(4000, 4000);
        canvas.getTransforms().addAll(translateTransform, scaleTransform);

        viewport = new Pane(canvas);
        viewport.setStyle("-fx-background-color: #1a1a2e;");
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

        // ── Controls Bar ───────────────────────────────────────
        TextField valueField = new TextField();
        valueField.setPromptText("Value");
        valueField.setPrefWidth(90);
        valueField.setStyle(fieldStyle());

        Button insertBtn  = styledButton("Insert",  "#27ae60");
        Button deleteBtn  = styledButton("Delete",  "#c0392b");
        Button searchBtn  = styledButton("Search",  "#2980b9");
        Button randomBtn  = styledButton("Random",  "#8e44ad");
        Button clearBtn   = styledButton("Clear",   "#7f8c8d");

        insertBtn.setOnAction(e -> handleInsert(valueField));
        deleteBtn.setOnAction(e -> handleDelete(valueField));
        searchBtn.setOnAction(e -> handleSearch(valueField));
        randomBtn.setOnAction(e -> handleRandom());
        clearBtn.setOnAction(e  -> handleClear());
        valueField.setOnAction(e -> handleInsert(valueField));

        Button zoomInBtn = styledButton("🔍+", "#8e44ad");
        Button zoomOutBtn = styledButton("🔍-", "#8e44ad");
        zoomInBtn.setOnAction(e -> doZoom(1.2, viewport.getWidth()/2, viewport.getHeight()/2));
        zoomOutBtn.setOnAction(e -> doZoom(0.8, viewport.getWidth()/2, viewport.getHeight()/2));

        toggleCodeBtn = styledButton("◀ Code", "#e67e22");
        toggleCodeBtn.setOnAction(e -> toggleCodePanel());

        HBox controls = new HBox(10, valueField, insertBtn, deleteBtn,
                                  searchBtn, randomBtn, clearBtn,
                                  new Separator(Orientation.VERTICAL),
                                  zoomInBtn, zoomOutBtn,
                                  new Separator(Orientation.VERTICAL),
                                  toggleCodeBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(12));
        controls.setStyle("-fx-background-color: #16213e;");

        // ── Status Bar ─────────────────────────────────────────
        statusLabel = new Label("Ready — enter a value or write Java code in the panel. Scroll to zoom, drag to pan.");
        statusLabel.setTextFill(Color.web("#bdc3c7"));
        statusLabel.setFont(Font.font("Consolas", 13));
        statusLabel.setPadding(new Insets(6, 14, 6, 14));
        statusLabel.setStyle("-fx-background-color: #0f3460;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        // ── Title ──────────────────────────────────────────────
        Label title = new Label("Binary Search Tree Visualizer");
        title.setTextFill(Color.web("#e94560"));
        title.setFont(Font.font("Segoe UI", 22));
        title.setPadding(new Insets(10));
        title.setStyle("-fx-background-color: #16213e; -fx-font-weight: bold;");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        // ── Code Panel ─────────────────────────────────────────
        codePanel = buildCodePanel();

        HBox centerArea = new HBox(viewport, codePanel);
        HBox.setHgrow(viewport, Priority.ALWAYS);

        VBox layout = new VBox(title, controls, centerArea, statusLabel);
        VBox.setVgrow(centerArea, Priority.ALWAYS);

        Scene scene = new Scene(layout, 1380, 780);
        stage.setTitle("BST Visualizer");
        stage.setScene(scene);
        stage.show();

        // Initial centering of the virtual canvas (X=2000 is center of our virtual space)
        Platform.runLater(() -> {
            translateTransform.setX(viewport.getWidth() / 2 - 2000);
            translateTransform.setY(20);
        });
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
        panel.setStyle("-fx-background-color: #121225; -fx-border-color: #4a6fa5; -fx-border-width: 0 0 0 2;");
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

        String result = executeSingleCommand(line);
        if (result != null) {
            outputArea.appendText("      " + padLineNum("") + " └─➤ " + result + "\n");
        }

        outputArea.positionCaret(outputArea.getLength());

        PauseTransition pause = new PauseTransition(Duration.millis(EXEC_DELAY));
        pause.setOnFinished(e -> executeLineSequence(lines, index + 1));
        pause.play();
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

        String result = executeSingleCommand(line);
        if (result != null) {
            outputArea.appendText("      " + padLineNum("") + " └─➤ " + result + "\n");
        }
        outputArea.positionCaret(outputArea.getLength());

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

    private String executeSingleCommand(String line) {
        Matcher m = METHOD_CALL.matcher(line);
        if (m.matches()) {
            String cmd = m.group(1).toLowerCase();
            String arg = m.group(2);
            return executeCommand(cmd, arg);
        }

        Matcher c = CONSTRUCTOR.matcher(line);
        if (c.matches()) {
            handleClear();
            return "BinarySearchTree initialized";
        }

        Matcher sout = SYSOUT.matcher(line);
        if (sout.matches()) {
            String content = sout.group(1).trim();
            if (content.startsWith("\"") && content.endsWith("\"")) {
                content = content.substring(1, content.length() - 1);
            }
            return ">> " + content;
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
            return "Declared array " + varName + " with " + vals.length + " elements";
        }

        Matcher forEach = FOR_EACH.matcher(line);
        if (forEach.matches()) {
            String arrayName = forEach.group(1);
            String cmd = forEach.group(2).toLowerCase();
            int[] vals = declaredArrays.get(arrayName);
            if (vals == null) {
                return "❌ Array '" + arrayName + "' not found";
            }
            StringBuilder sb = new StringBuilder();
            for (int val : vals) {
                String res = executeCommand(cmd, String.valueOf(val));
                sb.append(res).append("; ");
            }
            return sb.toString().trim();
        }

        if (line.startsWith("import ") || line.startsWith("package ") || line.startsWith("public class")
                || line.startsWith("class ") || line.contains("public static void main")
                || line.startsWith("public ") || line.startsWith("private ")
                || line.startsWith("@")) {
            return "(declaration acknowledged)";
        }

        if (line.startsWith("int ") || line.startsWith("String ") || line.startsWith("var ")) {
            return "(variable declared)";
        }

        status("Unrecognized: " + line);
        return "⚠ Unrecognized syntax — use tree.insert(n), tree.delete(n), tree.search(n), tree.clear()";
    }

    private String executeCommand(String cmd, String arg) {
        switch (cmd) {
            case "insert": {
                if (arg.isEmpty()) return "❌ insert requires a number";
                int val = Integer.parseInt(arg);
                insert(val);
                highlightNode(val, Color.web("#27ae60"), 1200);
                return "Inserted node " + val;
            }
            case "delete": {
                if (arg.isEmpty()) return "❌ delete requires a number";
                int val = Integer.parseInt(arg);
                TreeNode target = find(root, val);
                if (target != null) {
                    highlightNodeDirect(target, Color.web("#c0392b"), 500);
                    delete(val);
                    return "Deleted node " + val;
                } else {
                    return "❌ Node " + val + " not found in tree";
                }
            }
            case "search": {
                if (arg.isEmpty()) return "❌ search requires a number";
                int val = Integer.parseInt(arg);
                search(val);
                return (find(root, val) != null) ? "✔ Found node " + val : "✘ Node " + val + " not found";
            }
            case "clear": {
                handleClear();
                return "Tree cleared";
            }
            default:
                return "❌ Unknown method: " + cmd;
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
    //  Node Highlighting
    // ═══════════════════════════════════════════════════════════

    private void highlightNode(int value, Color color, double durationMs) {
        TreeNode node = find(root, value);
        if (node == null) return;
        highlightNodeDirect(node, color, durationMs);
    }

    private void highlightNodeDirect(TreeNode node, Color color, double durationMs) {
        Color originalColor = Color.web("#2e7dd6");
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
        if (root == null) {
            root = new TreeNode(value);
            // Start the root at X=2000 (center of our virtual 4000x4000 space)
            root.placeAt(2000, ROOT_Y);
            addNodeToCanvas(root);
            status("Inserted root: " + value);
            return;
        }

        TreeNode cur = root;
        while (true) {
            if (value == cur.value) {
                status("Duplicate value " + value + " — not inserted");
                return;
            }
            if (value < cur.value) {
                if (cur.left == null) {
                    cur.left = new TreeNode(value);
                    cur.left.parent = cur;
                    addNodeToCanvas(cur.left);
                    break;
                }
                cur = cur.left;
            } else {
                if (cur.right == null) {
                    cur.right = new TreeNode(value);
                    cur.right.parent = cur;
                    addNodeToCanvas(cur.right);
                    break;
                }
                cur = cur.right;
            }
        }
        relayout();
        status("Inserted: " + value);
    }

    private void delete(int value) {
        TreeNode node = find(root, value);
        if (node == null) {
            status("Value " + value + " not found");
            return;
        }
        deleteNode(node);
        relayout();
        status("Deleted: " + value);
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
        resetColors();
        TreeNode cur = root;
        List<TreeNode> path = new ArrayList<>();
        while (cur != null) {
            path.add(cur);
            if (value == cur.value) break;
            cur = (value < cur.value) ? cur.left : cur.right;
        }

        boolean found = (cur != null && cur.value == value);

        Timeline tl = new Timeline();
        for (int i = 0; i < path.size(); i++) {
            TreeNode n = path.get(i);
            boolean isLast = (i == path.size() - 1);
            Color col = isLast
                    ? (found ? Color.web("#27ae60") : Color.web("#c0392b"))
                    : Color.web("#f39c12");

            if (isLast) {
                TreeNode finalNode = n;
                Color finalCol = col;
                tl.getKeyFrames().add(new KeyFrame(
                        Duration.millis(4000 * (i + 1)),
                        e -> {
                            finalNode.circle.setFill(finalCol);
                            Glow glow = new Glow(0.8);
                            DropShadow glowShadow = new DropShadow(25, finalCol);
                            glow.setInput(glowShadow);
                            finalNode.circle.setEffect(glow);
                            finalNode.circle.setScaleX(1.3);
                            finalNode.circle.setScaleY(1.3);
                        }
                ));
            } else {
                tl.getKeyFrames().add(new KeyFrame(
                        Duration.millis(4000 * (i + 1)),
                        e -> n.circle.setFill(col)
                ));
            }
        }
        tl.getKeyFrames().add(new KeyFrame(
                Duration.millis(4000 * path.size() + 4000),
                e -> {
                    resetColors();
                    resetScales(root);
                }
        ));
        tl.play();

        status(found ? "Found " + value : "Value " + value + " not in tree");
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
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(ANIM_MS),
                new KeyValue(node.circle.centerXProperty(), node.targetX),
                new KeyValue(node.circle.centerYProperty(), node.targetY)
        ));
        tl.setOnFinished(e -> {
            node.label.setX(node.targetX - labelOffset(node));
            node.label.setY(node.targetY + 5);
            updateEdges(root);
        });
        tl.play();

        animateToTargets(node.left);
        animateToTargets(node.right);
    }

    private void updateEdges(TreeNode node) {
        if (node == null) return;
        if (node.edgeToParent != null && node.parent != null) {
            node.edgeToParent.setStartX(node.parent.circle.getCenterX());
            node.edgeToParent.setStartY(node.parent.circle.getCenterY() + 24);
            node.edgeToParent.setEndX(node.circle.getCenterX());
            node.edgeToParent.setEndY(node.circle.getCenterY() - 24);
        }
        updateEdges(node.left);
        updateEdges(node.right);
    }

    // ═══════════════════════════════════════════════════════════
    //  Canvas Helpers
    // ═══════════════════════════════════════════════════════════

    private void addNodeToCanvas(TreeNode node) {
        if (node.parent != null) {
            Line edge = new Line();
            edge.setStroke(Color.web("#4a6fa5"));
            edge.setStrokeWidth(2);
            node.edgeToParent = edge;
            canvas.getChildren().add(0, edge);
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
        n.circle.setFill(Color.web("#2e7dd6"));
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
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    private String fieldStyle() {
        return "-fx-background-color: #1a1a2e;" +
               "-fx-text-fill: white;" +
               "-fx-prompt-text-fill: #7f8c8d;" +
               "-fx-border-color: #4a6fa5;" +
               "-fx-border-radius: 4;" +
               "-fx-background-radius: 4;" +
               "-fx-padding: 8;" +
               "-fx-font-size: 13px;";
    }

    // ── Main ───────────────────────────────────────────────────
    public static void main(String[] args) {
        launch(args);
    }
}
