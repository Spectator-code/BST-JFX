import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * Challenge Mode view with an integrated focus-lock system.
 * Any time the user switches applications (Alt-Tab, clicks elsewhere) or
 * minimizes the window during a challenge, a full-screen warning overlay
 * is shown and a violation counter is incremented.
 */
public class ChallengeView {

    // ── Focus-lock state ───────────────────────────────────────────────────
    private int warningCount = 0;
    private static final int MAX_WARNINGS = 3;
    private StackPane warningOverlay;
    private ChangeListener<Boolean> focusListener;
    private ChangeListener<Boolean> iconifyListener;
    private boolean overlayActive = false;
    private boolean listenerArmed = false; // prevents false trigger on first load
    private javafx.scene.Node blurredNode;

    // ── Core view state ───────────────────────────────────────────────────
    private int currentProblemId = 1;
    private VBox leftPanel;
    private ExplorerView explorer;
    private Label statusLabel;

    // ── Content protection state ──────────────────────────────────────────
    private StackPane toastPane;   // top-of-screen protection toast
    private Label     toastLabel;  // reused toast label node

    public Parent getView() {
        // ── Root is StackPane so overlay sits on top ───────────────────────
        StackPane rootStack = new StackPane();
        BSTBackgroundPane bgPane = new BSTBackgroundPane();
        rootStack.getChildren().add(bgPane);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: transparent;");

        // ── Top Nav ───────────────────────────────────────────────────────
        HBox nav = new HBox(14);
        nav.setStyle("-fx-background-color: " + Theme.NAV_BG + "; -fx-padding: 10 16;");
        nav.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = styledBtn("← Dashboard", Theme.TEXT_MUTED);
        backBtn.setOnAction(e -> {
            detachListeners();
            App.changeScene(new DashboardView().getView());
        });

        Label title = new Label("⚔  Challenge Mode");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(Theme.ACCENT));

        // Solved counter + warning indicator in nav
        int totalSolved = App.db.getSolvedProblems().size();
        int total = ProblemManager.getAll().size();
        Label solvedNav = new Label("✅  " + totalSolved + " / " + total + " solved");
        solvedNav.setTextFill(Color.web(Theme.TEXT_LIGHT));
        solvedNav.setFont(Font.font("Arial", 13));

        Label warningNavLabel = new Label("🛡 Focus: OK");
        warningNavLabel.setTextFill(Color.web(Theme.SUCCESS));
        warningNavLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        warningNavLabel.setStyle(
            "-fx-background-color: #0c3321;" +
            "-fx-border-color: " + Theme.SUCCESS + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 4 10;"
        );

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);

        nav.getChildren().addAll(backBtn, title, navSpacer, solvedNav, warningNavLabel);
        root.setTop(nav);

        // ── Split Pane ────────────────────────────────────────────────────
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent;");

        explorer = new ExplorerView(true);
        Parent rightPanel = explorer.getView();

        leftPanel = new VBox(12);
        leftPanel.setPadding(new Insets(16));
        leftPanel.setStyle("-fx-background-color: " + Theme.SURFACE + "dd;");

        ScrollPane leftScroll = new ScrollPane(leftPanel);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        leftScroll.setMinWidth(270);

        loadProblem(currentProblemId);

        splitPane.getItems().addAll(leftScroll, rightPanel);
        splitPane.setDividerPositions(0.3);
        root.setCenter(splitPane);

        // ── Build warning overlay (hidden initially) ──────────────────────
        warningOverlay = buildWarningOverlay(warningNavLabel);
        warningOverlay.setVisible(false);
        warningOverlay.setManaged(false);

        rootStack.getChildren().addAll(root, warningOverlay);

        // ── Content protection layers ─────────────────────────────────────
        // 1. Diagonal username watermark (mouse-transparent, purely visual)
        Pane watermark = buildWatermarkPane();
        rootStack.getChildren().add(watermark);

        // 2. Toast notification bar (starts hidden, shown when protection fires)
        toastPane  = buildToastPane();
        toastLabel = (Label) toastPane.getChildren().get(0);
        rootStack.getChildren().add(toastPane);

        // 3. Scene-level key blocker + right-click disabler (arm after load)
        Platform.runLater(() -> {
            attachContentProtections(rootStack, leftScroll);
        });

        // ── Attach focus/iconify listeners after scene is shown ───────────
        Platform.runLater(() -> {
            // Small delay so the initial scene-set focus change doesn't trigger a warning
            PauseTransition arm = new PauseTransition(Duration.millis(800));
            arm.setOnFinished(e -> {
                attachListeners(root, warningNavLabel);
                listenerArmed = true;
            });
            arm.play();
        });

        return rootStack;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Warning Overlay Builder
    // ═══════════════════════════════════════════════════════════════════════

    private StackPane buildWarningOverlay(Label navStatusLabel) {
        // Semi-transparent dark backdrop
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.78);");

        // Warning card
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(44, 56, 44, 56));
        card.setMaxWidth(500);
        card.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: " + Theme.DANGER + ";" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.45), 25, 0, 0, 4);"
        );

        // Warning icon pulsing
        Label icon = new Label("⚠");
        icon.setFont(Font.font("Arial", 52));
        icon.setTextFill(Color.web(Theme.DANGER));
        ScaleTransition pulse = new ScaleTransition(Duration.millis(800), icon);
        pulse.setFromX(1.0); pulse.setToX(1.15);
        pulse.setFromY(1.0); pulse.setToY(1.15);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        Label heading = new Label("Focus Violation Detected!");
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        heading.setTextFill(Color.web(Theme.DANGER));

        Label countLabel = new Label(); // updated each time
        countLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        countLabel.setTextFill(Color.web("#a0b4ff"));
        countLabel.setTextAlignment(TextAlignment.CENTER);

        Label body = new Label(
            "You left the challenge window!\n" +
            "Switching tabs or minimizing the screen\n" +
            "during a challenge is not allowed."
        );
        body.setWrapText(true);
        body.setTextAlignment(TextAlignment.CENTER);
        body.setTextFill(Color.web("#b0c4ff"));
        body.setFont(Font.font("Arial", 14));
        body.setAlignment(Pos.CENTER);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a1520;");

        Label tipsLabel = new Label("Return to the challenge and focus on the problem.");
        tipsLabel.setFont(Font.font("Arial", 12));
        tipsLabel.setTextFill(Color.web(Theme.TEXT_MUTED));
        tipsLabel.setTextAlignment(TextAlignment.CENTER);

        Button dismissBtn = new Button("✔  I Understand — Resume Challenge");
        dismissBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        String dismissBase =
            "-fx-background-color: " + Theme.DANGER + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 12 28;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 0);";
        dismissBtn.setStyle(dismissBase);
        dismissBtn.setOnMouseEntered(e -> dismissBtn.setOpacity(0.85));
        dismissBtn.setOnMouseExited(e  -> dismissBtn.setOpacity(1.0));

        // Dismissed: hide overlay, update nav label
        dismissBtn.setOnAction(e -> hideOverlay(navStatusLabel));

        card.getChildren().addAll(icon, heading, countLabel, body, sep, tipsLabel, dismissBtn);

        StackPane.setAlignment(card, Pos.CENTER);
        overlay.getChildren().add(card);

        // Store countLabel reference so it can be updated when the overlay is shown
        overlay.setUserData(countLabel);
        return overlay;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Focus / Iconify Listeners
    // ═══════════════════════════════════════════════════════════════════════

    private void attachListeners(javafx.scene.Node content, Label navStatusLabel) {
        javafx.stage.Stage stage = App.getPrimaryStage();
        if (stage == null) return;

        // Triggered when the app loses OS focus (Alt-Tab, clicking another window, etc.)
        focusListener = (obs, wasFocused, isFocused) -> {
            if (!listenerArmed) return;
            if (!isFocused && !overlayActive) {
                Platform.runLater(() -> showOverlay(content, navStatusLabel));
            }
        };

        // Triggered when the window is minimized / iconified
        iconifyListener = (obs, wasIconified, isIconified) -> {
            if (!listenerArmed) return;
            if (isIconified && !overlayActive) {
                // De-iconify immediately, then show the overlay
                Platform.runLater(() -> {
                    stage.setIconified(false);
                    showOverlay(content, navStatusLabel);
                });
            }
        };

        stage.focusedProperty().addListener(focusListener);
        stage.iconifiedProperty().addListener(iconifyListener);
    }

    /** Remove listeners when navigating away so they don't fire on other views. */
    private void detachListeners() {
        javafx.stage.Stage stage = App.getPrimaryStage();
        if (stage == null) return;
        if (focusListener  != null) stage.focusedProperty().removeListener(focusListener);
        if (iconifyListener != null) stage.iconifiedProperty().removeListener(iconifyListener);
        focusListener   = null;
        iconifyListener = null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Overlay Show / Hide
    // ═══════════════════════════════════════════════════════════════════════
    private void showOverlay(javafx.scene.Node content, Label navStatusLabel) {
        if (overlayActive) return;
        overlayActive = true;
        warningCount++;

        // Immediately persist violation in database
        App.db.incrementViolationCount(App.db.getCurrentUser());
        // Update the warning count label inside the card
        Label countLabel = (Label) warningOverlay.getUserData();
        String countColor = warningCount >= MAX_WARNINGS ? "#e74c3c" : "#ffd966";
        countLabel.setText("⚠  Warning " + warningCount + " of " + MAX_WARNINGS +
            (warningCount >= MAX_WARNINGS ? " — FINAL WARNING" : ""));
        countLabel.setTextFill(Color.web(countColor));

        // Update nav label
        updateNavLabel(navStatusLabel);

        // Blur the background content
        this.blurredNode = content;
        content.setEffect(new GaussianBlur(12));

        // Fade in the overlay
        warningOverlay.setManaged(true);
        warningOverlay.setVisible(true);
        warningOverlay.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), warningOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Shake the card for emphasis
        VBox card = (VBox) warningOverlay.getChildren().get(0);
        shakeNode(card);

        // Request focus back to our window
        App.getPrimaryStage().requestFocus();
    }

    private void hideOverlay(Label navStatusLabel) {
        if (!overlayActive) return;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), warningOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            warningOverlay.setVisible(false);
            warningOverlay.setManaged(false);
            overlayActive = false;

            // Remove blur from background
            if (blurredNode != null) {
                blurredNode.setEffect(null);
                blurredNode = null;
            }
        });
        fadeOut.play();
    }

    private void updateNavLabel(Label navLabel) {
        if (warningCount == 0) {
            navLabel.setText("🛡 Focus: OK");
            navLabel.setTextFill(Color.web(Theme.SUCCESS));
            navLabel.setStyle(
                "-fx-background-color: #0c3321;" +
                "-fx-border-color: " + Theme.SUCCESS + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 4 10;"
            );
        } else if (warningCount < MAX_WARNINGS) {
            navLabel.setText("⚠ Warnings: " + warningCount + "/" + MAX_WARNINGS);
            navLabel.setTextFill(Color.web("#ffd966"));
            navLabel.setStyle(
                "-fx-background-color: #2a1e00;" +
                "-fx-border-color: #ffd966;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 4 10;"
            );
        } else {
            navLabel.setText("🚫 Violations: " + warningCount + " — Final");
            navLabel.setTextFill(Color.web(Theme.DANGER));
            navLabel.setStyle(
                "-fx-background-color: #3d0a14;" +
                "-fx-border-color: " + Theme.DANGER + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 4 10;"
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Problem Loading
    // ═══════════════════════════════════════════════════════════════════════

    private void loadProblem(int id) {
        leftPanel.getChildren().clear();
        ProblemManager.Problem p = ProblemManager.get(id);
        java.util.List<Integer> solvedList = App.db.getSolvedProblems();
        this.currentProblemId = id;

        // ── Problem selector ──────────────────────────────────────────────
        Label selectorLabel = new Label("Select Problem:");
        selectorLabel.setTextFill(Color.web(Theme.TEXT_LIGHT));
        selectorLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        ComboBox<String> selector = new ComboBox<>();
        selector.setMaxWidth(Double.MAX_VALUE);
        selector.setStyle(
            "-fx-background-color: " + Theme.NAV_BG + ";" +
            "-fx-text-fill: white;" +
            "-fx-border-color: " + Theme.BORDER + ";" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 2 6;"
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
        pTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        pTitle.setTextFill(Color.WHITE);
        pTitle.setWrapText(true);

        // ── Difficulty badge ──────────────────────────────────────────────
        String diffColor = Theme.difficultyColor(p.difficulty);
        Label pDiff = new Label(p.difficulty.toUpperCase());
        pDiff.setTextFill(Color.web(diffColor));
        pDiff.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        pDiff.setStyle(
            "-fx-background-color: " + diffColor + "22;" +
            "-fx-border-color: " + diffColor + "44;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 3 10;"
        );

        // ── Already solved badge ──────────────────────────────────────────
        boolean isSolved = solvedList.contains(id);
        Label solvedBadge = new Label(isSolved ? "✅  Already Solved" : "");
        solvedBadge.setTextFill(Color.web(Theme.SUCCESS));
        solvedBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        // ── Description ───────────────────────────────────────────────────
        Label pDesc = new Label(p.description);
        pDesc.setWrapText(true);
        pDesc.setTextFill(Color.web("#b0c4ff"));
        pDesc.setFont(Font.font("Arial", 13));

        // ── AVL mode banner ───────────────────────────────────────────────
        if (id >= 15 && id <= 17) {
            Label avlBanner = new Label("🔀  This problem uses AVL Trees.\nSelect AVL mode in the code editor toolbar.");
            avlBanner.setWrapText(true);
            avlBanner.setTextFill(Color.web("#ffd966"));
            avlBanner.setFont(Font.font("Arial", 12));
            avlBanner.setStyle(
                "-fx-background-color: #2a2000;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 10 12;" +
                "-fx-border-color: #665500;" +
                "-fx-border-radius: 8;"
            );
            leftPanel.getChildren().add(avlBanner);
        }

        // ── Traversal mode banner ─────────────────────────────────────────
        if (id >= 18 && id <= 20) {
            Label travBanner = new Label("🌐  Run your code first, then use the\n'Traversals' section in the left sidebar\nto perform the traversal — then submit.");
            travBanner.setWrapText(true);
            travBanner.setTextFill(Color.web("#80ffcc"));
            travBanner.setFont(Font.font("Arial", 12));
            travBanner.setStyle(
                "-fx-background-color: #002a1a;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 10 12;" +
                "-fx-border-color: #004d30;" +
                "-fx-border-radius: 8;"
            );
            leftPanel.getChildren().add(travBanner);
        }

        // ── Test Cases Checklist Panel ────────────────────────────────────
        VBox testCasesBox = new VBox(8);
        testCasesBox.setPadding(new Insets(12));
        testCasesBox.setStyle(
            "-fx-background-color: #0c123655;" +
            "-fx-border-color: " + Theme.BORDER + "44;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;"
        );

        Label testHeader = new Label("📋  Test Cases");
        testHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        testHeader.setTextFill(Color.web(Theme.TEXT_LIGHT));
        testCasesBox.getChildren().add(testHeader);

        for (String test : p.tests) {
            Label testLbl = new Label("   ○  " + test);
            testLbl.setFont(Font.font("Consolas", 12));
            testLbl.setTextFill(Color.web(Theme.TEXT_MUTED));
            testCasesBox.getChildren().add(testLbl);
        }

        // ── Hint Expandable Drawer ────────────────────────────────────────
        VBox hintContainer = new VBox(8);
        if (!p.hints.isEmpty()) {
            Button revealHintBtn = new Button("💡 Reveal Hint");
            revealHintBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            revealHintBtn.setStyle(
                "-fx-background-color: #8e44ad22;" +
                "-fx-text-fill: #ffd966;" +
                "-fx-border-color: #8e44ad66;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 6 12;" +
                "-fx-cursor: hand;"
            );

            Label hintTextLabel = new Label("💡 Hint: " + p.hints.get(0));
            hintTextLabel.setWrapText(true);
            hintTextLabel.setTextFill(Color.web("#ffd966"));
            hintTextLabel.setFont(Font.font("Arial", 12));
            hintTextLabel.setStyle(
                "-fx-background-color: #2a2000;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 8 10;" +
                "-fx-border-color: #665500;" +
                "-fx-border-radius: 8;"
            );
            hintTextLabel.setVisible(false);
            hintTextLabel.setManaged(false);

            revealHintBtn.setOnAction(ev -> {
                revealHintBtn.setVisible(false);
                revealHintBtn.setManaged(false);
                hintTextLabel.setVisible(true);
                hintTextLabel.setManaged(true);
                
                FadeTransition ft = new FadeTransition(Duration.millis(300), hintTextLabel);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);
                ft.play();
            });

            hintContainer.getChildren().addAll(revealHintBtn, hintTextLabel);
        }

        // ── Submit & Reset buttons ────────────────────────────────────────
        HBox submitBox = new HBox(8);
        Button submitBtn = styledBtn("▶  Submit & Check", Theme.SUCCESS);
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(submitBtn, Priority.ALWAYS);

        Button resetTemplateBtn = styledBtn("🔄 Reset", Theme.TEXT_MUTED);
        resetTemplateBtn.setOnAction(e -> {
            if (explorer != null) {
                explorer.setCode(p.startingCode);
                explorer.handleClear();
            }
            // Re-render raw test cases checklist
            testCasesBox.getChildren().clear();
            testCasesBox.getChildren().add(testHeader);
            for (String test : p.tests) {
                Label testLbl = new Label("   ○  " + test);
                testLbl.setFont(Font.font("Consolas", 12));
                testLbl.setTextFill(Color.web(Theme.TEXT_MUTED));
                testCasesBox.getChildren().add(testLbl);
            }
        });
        submitBox.getChildren().addAll(submitBtn, resetTemplateBtn);

        this.statusLabel = new Label();
        this.statusLabel.setWrapText(true);
        this.statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        this.statusLabel.setMaxWidth(Double.MAX_VALUE);

        final Label finalStatus = this.statusLabel;
        submitBtn.setOnAction(e -> {
            String code = explorer != null ? explorer.getCode().trim() : "";
            if (code.isEmpty()) {
                setStatus(finalStatus, "❌  Code editor is empty!", false);
                return;
            }

            boolean allPassed = true;
            testCasesBox.getChildren().clear();
            testCasesBox.getChildren().add(testHeader);

            for (String test : p.tests) {
                boolean passed = evaluateTestCase(id, test);
                Label testLbl = new Label();
                testLbl.setFont(Font.font("Consolas", 12));
                if (passed) {
                    testLbl.setText("   ✔  " + test + " (Passed)");
                    testLbl.setTextFill(Color.web(Theme.SUCCESS));
                } else {
                    testLbl.setText("   ❌  " + test + " (Failed)");
                    testLbl.setTextFill(Color.web(Theme.DANGER));
                    allPassed = false;
                }
                testCasesBox.getChildren().add(testLbl);
            }

            if (allPassed) {
                App.db.markProblemSolved(id);
                setStatus(this.statusLabel, "✅  Tests Passed! Problem solved.", true);
                int nextId = id + 1;
                if (nextId <= ProblemManager.getAll().size()) {
                    PauseTransition advance = new PauseTransition(Duration.millis(1800));
                    advance.setOnFinished(ev -> loadProblem(nextId));
                    advance.play();
                }
            } else {
                setStatus(finalStatus, "❌  Some tests failed. Run your code first, then submit.", false);
                shakeNode(testCasesBox);
            }
        });

        // ── Navigation buttons ────────────────────────────────────────────
        HBox navBtns = new HBox(8);
        Button prevBtn = styledBtn("◀ Prev", Theme.TEXT_MUTED);
        Button nextBtn = styledBtn("Next ▶", Theme.PRIMARY);
        prevBtn.setDisable(id <= 1);
        nextBtn.setDisable(id >= ProblemManager.getAll().size());
        prevBtn.setOnAction(e -> loadProblem(id - 1));
        nextBtn.setOnAction(e -> loadProblem(id + 1));
        navBtns.getChildren().addAll(prevBtn, nextBtn);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1a2a5a;");

        // ── Assemble panel ────────────────────────────────────────────────
        leftPanel.getChildren().addAll(selectorLabel, selector, pTitle, pDiff, solvedBadge, pDesc);
        leftPanel.getChildren().add(testCasesBox);
        if (!p.hints.isEmpty()) leftPanel.getChildren().add(hintContainer);
        leftPanel.getChildren().addAll(sep, submitBox, statusLabel, navBtns);

        if (explorer != null) {
            explorer.handleClear();
            if (p.startingCode != null && !p.startingCode.isEmpty()) {
                explorer.setCode(p.startingCode);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private void setStatus(Label lbl, String text, boolean pass) {
        lbl.setText(text);
        lbl.setStyle(pass
            ? "-fx-background-color: #0c3321; -fx-text-fill: #5fe3a1; -fx-border-color: " + Theme.SUCCESS + "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10 14; -fx-font-weight: bold;"
            : "-fx-background-color: #3d0a14; -fx-text-fill: #ff6b8b; -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10 14; -fx-font-weight: bold;"
        );
    }

    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(55), node);
        tt.setFromX(0); tt.setByX(14); tt.setCycleCount(5);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
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
            case 15 -> explorer.hasNode(20) && explorer.hasNode(30) && explorer.hasNode(10)
                        && explorer.getRootValue() == 20;
            case 16 -> explorer.hasNode(20) && explorer.hasNode(30) && explorer.hasNode(10)
                        && explorer.getRootValue() == 20 && explorer.getHeight() <= 2;
            case 17 -> explorer.getRootValue() == 50 && explorer.getHeight() <= 3;
            case 18 -> explorer.hasNode(40) && explorer.hasNode(20) && explorer.hasNode(60)
                        && explorer.hasNode(10) && explorer.hasNode(30);
            case 19 -> explorer.hasNode(50) && explorer.hasNode(30) && explorer.hasNode(70)
                        && explorer.hasNode(20) && explorer.hasNode(40);
            case 20 -> explorer.hasNode(45) && explorer.hasNode(15) && explorer.hasNode(75)
                        && explorer.hasNode(10) && explorer.hasNode(25);
            default -> explorer.getCode().trim().length() > 5;
        };
    }

    private boolean evaluateTestCase(int probId, String testName) {
        String log = explorer.getOutputLog().toLowerCase();
        String test = testName.toLowerCase().trim();
        
        if (test.contains("has node")) {
            try {
                String numStr = test.replace("has node", "").trim();
                int num = Integer.parseInt(numStr);
                return explorer.hasNode(num);
            } catch (Exception e) {
                return false;
            }
        }
        if (test.contains("missing node")) {
            try {
                String numStr = test.replace("missing node", "").trim();
                int num = Integer.parseInt(numStr);
                return !explorer.hasNode(num);
            } catch (Exception e) {
                return false;
            }
        }
        if (test.contains("searched 30")) {
            return log.contains("found node 30") || log.contains("✔");
        }
        if (test.contains("searched 8")) {
            return log.contains("found node 8") || log.contains("✔") || log.contains("search(8)");
        }
        if (test.contains("found 8")) {
            return log.contains("found node") || log.contains("✔");
        }
        if (test.contains("inserted multiple")) {
            return log.contains("inserted") || log.contains("inserted node");
        }
        if (test.contains("build successful")) {
            return log.contains("build successful") || log.contains("✅");
        }
        if (test.contains("balanced avl")) {
            if (probId == 15) {
                return explorer.hasNode(20) && explorer.hasNode(30) && explorer.hasNode(10) && explorer.getRootValue() == 20;
            }
            if (probId == 16) {
                return explorer.hasNode(20) && explorer.hasNode(30) && explorer.hasNode(10) && explorer.getRootValue() == 20 && explorer.getHeight() <= 2;
            }
            if (probId == 17) {
                return explorer.getRootValue() == 50 && explorer.getHeight() <= 3;
            }
        }
        if (test.contains("in-order traversal")) {
            return explorer.hasNode(40) && explorer.hasNode(20) && explorer.hasNode(60) && explorer.hasNode(10) && explorer.hasNode(30);
        }
        if (test.contains("pre-order traversal")) {
            return explorer.hasNode(50) && explorer.hasNode(30) && explorer.hasNode(70) && explorer.hasNode(20) && explorer.hasNode(40);
        }
        if (test.contains("post-order traversal")) {
            return explorer.hasNode(45) && explorer.hasNode(15) && explorer.hasNode(75) && explorer.hasNode(10) && explorer.hasNode(25);
        }
        
        return false;
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

    // ═══════════════════════════════════════════════════════════════════════
    //  ██  CONTENT PROTECTION SYSTEM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a fully mouse-transparent diagonal watermark pane stamped with the
     * current username and "PROTECTED" across the entire view at low opacity.
     * This deters sharing screenshots by embedding an identifier in every frame.
     */
    private Pane buildWatermarkPane() {
        Pane pane = new Pane();
        pane.setMouseTransparent(true);   // never intercepts mouse events
        pane.setPickOnBounds(false);
        pane.setOpacity(1.0);

        String username = App.db.getCurrentUser();
        String watermarkText = "⚑  " + (username != null ? username.toUpperCase() : "PROTECTED")
                               + "  ·  SEND NODE  ·  PROTECTED";

        // Stamp at a -35° diagonal, spaced across the whole canvas
        for (int row = -3; row < 14; row++) {
            for (int col = -1; col < 5; col++) {
                Text t = new Text(watermarkText);
                t.setFont(Font.font("Arial", FontWeight.BOLD, 13));
                t.setFill(Color.web("#4d80ff", 0.06)); // barely visible
                t.setLayoutX(col * 380 - 60);
                t.setLayoutY(row * 100);
                Rotate rotate = new Rotate(-35, 0, 0);
                t.getTransforms().add(rotate);
                pane.getChildren().add(t);
            }
        }
        return pane;
    }

    /**
     * Builds the reusable toast notification bar that slides in at the top of
     * the screen when a content protection rule is triggered.
     */
    private StackPane buildToastPane() {
        Label lbl = new Label();
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.WHITE);
        lbl.setPadding(new Insets(10, 20, 10, 20));
        lbl.setStyle(
            "-fx-background-color: #7c0000;" +
            "-fx-background-radius: 0 0 10 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 12, 0, 0, 4);"
        );

        StackPane toast = new StackPane(lbl);
        toast.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(lbl, Pos.TOP_CENTER);
        toast.setMouseTransparent(true);
        toast.setVisible(false);
        toast.setManaged(false);
        return toast;
    }

    /** Shows the toast with given message for ~2.4 seconds then hides it. */
    private void showProtectionToast(String message) {
        if (toastLabel == null || toastPane == null) return;
        toastLabel.setText(message);
        toastPane.setManaged(true);
        toastPane.setVisible(true);
        toastPane.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toastPane);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        fadeIn.setOnFinished(ev -> {
            PauseTransition hold = new PauseTransition(Duration.millis(2400));
            hold.setOnFinished(ev2 -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(350), toastPane);
                fadeOut.setFromValue(1); fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev3 -> {
                    toastPane.setVisible(false);
                    toastPane.setManaged(false);
                });
                fadeOut.play();
            });
            hold.play();
        });
        fadeIn.play();
    }

    /**
     * Attaches all content protection rules to the scene:
     *  • Blocks PrintScreen  → shows toast + increments violation counter
     *  • Blocks Ctrl+P (print dialog)
     *  • Blocks Ctrl+Shift+S / Win+Shift+S (Snipping Tool shortcuts)
     *  • Disables right-click context menu on the problem panel
     */
    private void attachContentProtections(StackPane rootStack, javafx.scene.Node leftPanel) {
        Platform.runLater(() -> {
            javafx.scene.Scene scene = rootStack.getScene();
            if (scene == null) return;

            // ── Key-level protections ─────────────────────────────────────
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                KeyCode code = event.getCode();

                // Block PrintScreen
                if (code == KeyCode.PRINTSCREEN) {
                    event.consume();
                    showProtectionToast("🚫  Screenshots are disabled in Challenge Mode.");
                    return;
                }

                // Block Ctrl+P  (Print dialog)
                if (event.isControlDown() && code == KeyCode.P) {
                    event.consume();
                    showProtectionToast("🚫  Printing is disabled in Challenge Mode.");
                    return;
                }

                // Block Win+Shift+S / Ctrl+Shift+S  (Snipping Tool)
                if (event.isShortcutDown() && event.isShiftDown()
                        && (code == KeyCode.S || code == KeyCode.PRINTSCREEN)) {
                    event.consume();
                    showProtectionToast("🚫  Screen capture shortcuts are disabled here.");
                    return;
                }

                // Block F12 (browser-style developer tools, less relevant for JavaFX
                // but keeps the feel of a protected environment)
                if (code == KeyCode.F12) {
                    event.consume();
                    showProtectionToast("🔒  Developer tools are not available in Challenge Mode.");
                }
            });

            // ── Right-click disable on left problem panel ─────────────────
            // Users cannot right-click → copy problem text to paste elsewhere.
            leftPanel.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() == MouseButton.SECONDARY) {
                    event.consume(); // swallow right-click, no context menu
                    showProtectionToast("🔒  Right-click is disabled in Challenge Mode.");
                }
            });

            // ── Middle-click disable (some users use it to open links) ────
            leftPanel.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() == MouseButton.MIDDLE) {
                    event.consume();
                }
            });
        });
    }
}

