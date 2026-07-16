import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.chart.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Premium dashboard view for teachers.
 * Displays total statistics, a searchable list of registered students,
 * and a side detail panel showing exactly which of the problems each student solved.
 */
public class TeacherDashboardView {

    private VBox studentCardsContainer;
    private List<DatabaseManager.StudentInfo> allStudents = new ArrayList<>();
    private DatabaseManager.StudentInfo selectedStudent = null;

    // Detail panel elements
    private ScrollPane detailsPlaceholder;
    private VBox detailsContent;
    private Label detailStudentName;
    private Label detailStudentRank;
    private Label detailStudentXP;
    private Label detailStudentViolations;
    private Label detailStudentTotalScore;
    private GridPane solvedGrid;
    private HBox statsRow;
    private Label solvedTitle;

    public Parent getView() {
        StackPane rootStack = new StackPane();
        BSTBackgroundPane bgPane = new BSTBackgroundPane();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: transparent;");

        // ── Top Nav ───────────────────────────────────────────────────────
        HBox nav = new HBox(14);
        nav.setStyle("-fx-background-color: " + Theme.NAV_BG + "; -fx-padding: 14 20;");
        nav.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("  Send Node  ·  Teacher Portal");
        brand.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        brand.setTextFill(Color.web(Theme.ACCENT));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String user = App.db.getCurrentUser();
        Label userLabel = new Label("👤  " + (user != null ? user : "Instructor"));
        userLabel.setTextFill(Color.web(Theme.TEXT_LIGHT));
        userLabel.setFont(Font.font("Arial", 14));
        userLabel.setStyle("-fx-padding: 0 18 0 0;");

        List<String> teacherClasses = App.db.getTeacherClasses(user);
        
        ComboBox<String> classDropdown = new ComboBox<>();
        classDropdown.getItems().addAll(teacherClasses);
        if (!teacherClasses.isEmpty()) {
            classDropdown.getSelectionModel().selectFirst();
        }
        classDropdown.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Arial';" +
            "-fx-font-weight: bold;" +
            "-fx-border-color: " + Theme.PRIMARY + ";" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;"
        );
        
        Button newClassBtn = styledBtn("➕", Theme.ACCENT);
        newClassBtn.setStyle(newClassBtn.getStyle() + " -fx-padding: 4 8;");
        newClassBtn.setOnAction(e -> showNewClassDialog(user, classDropdown));

        Button settingsBtn = styledBtn("⚙", Theme.WARN);
        settingsBtn.setStyle(settingsBtn.getStyle() + " -fx-padding: 4 8;");
        settingsBtn.setOnAction(e -> showSettingsDialog(classDropdown.getValue()));

        Button createTaskBtn = styledBtn("📝", Theme.SUCCESS);
        createTaskBtn.setStyle(createTaskBtn.getStyle() + " -fx-padding: 4 8;");
        createTaskBtn.setOnAction(e -> showCreateTaskDialog(classDropdown.getValue()));

        HBox classControls = new HBox(6, new Label("Class: "), classDropdown, newClassBtn, settingsBtn, createTaskBtn);
        classControls.setAlignment(Pos.CENTER_LEFT);
        classControls.lookupAll(".label").forEach(l -> ((Label)l).setTextFill(Color.web(Theme.TEXT_LIGHT)));
        classControls.setStyle("-fx-padding: 0 20 0 0;");

        Button inviteBtn = styledBtn("✉  Invite Student", Theme.SUCCESS);
        inviteBtn.setOnAction(e -> showInviteStudentDialog());

        Button logoutBtn = styledBtn("Logout", Theme.DANGER);
        logoutBtn.setOnAction(e -> confirmLogout());

        nav.getChildren().addAll(brand, spacer, classControls, userLabel, inviteBtn, logoutBtn);
        root.setTop(nav);

        // ── Load Student Data ─────────────────────────────────────────────
        classDropdown.valueProperty().addListener((obs, oldVal, newVal) -> {
            loadStudentsForClass(newVal);
        });
        loadStudentsForClass(classDropdown.getValue());

        // ── Split Pane Layout ─────────────────────────────────────────────
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent;");

        // Left Area: Search + Stats + Student Cards list
        VBox leftPanel = new VBox(16);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setStyle("-fx-background-color: " + Theme.SURFACE + "cc;");
        leftPanel.setMinWidth(420);

        // A. Statistics row
        statsRow = buildStatsRow();

        // B. Search bar
        TextField searchField = new TextField();
        searchField.setPromptText("🔍  Search students by username...");
        searchField.setStyle(
            "-fx-background-color: #0c1236;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #4d5c8a;" +
            "-fx-border-color: #1e2c56;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 14;" +
            "-fx-font-size: 13px;"
        );
        searchField.focusedProperty().addListener((obs, was, now) -> {
            if (now) {
                searchField.setStyle(
                    "-fx-background-color: " + Theme.SURFACE + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-prompt-text-fill: " + Theme.TEXT_MUTED + ";" +
                    "-fx-border-color: " + Theme.ACCENT + ";" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 9 13;" +
                    "-fx-font-size: 13px;"
                );
            } else {
                searchField.setStyle(
                    "-fx-background-color: " + Theme.NAV_BG + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-prompt-text-fill: " + Theme.TEXT_MUTED + ";" +
                    "-fx-border-color: " + Theme.BORDER + ";" +
                    "-fx-border-width: 1.5;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 10 14;" +
                    "-fx-font-size: 13px;"
                );
            }
        });
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterStudents(newVal));

        // C. Scrollable list of Student Cards
        studentCardsContainer = new VBox(10);
        studentCardsContainer.setStyle("-fx-background-color: transparent;");

        ScrollPane cardsScroll = new ScrollPane(studentCardsContainer);
        cardsScroll.setFitToWidth(true);
        cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        cardsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(cardsScroll, Priority.ALWAYS);

        leftPanel.getChildren().addAll(statsRow, searchField, cardsScroll);

        // Populate cards
        renderStudentCards(allStudents);

        // Right Area: Student detail sidebar
        StackPane rightPanel = new StackPane();
        rightPanel.setStyle("-fx-background-color: #080d28cc;");
        rightPanel.setPadding(new Insets(24));

        // State 1: Placeholder when no student is clicked (Visual Analytics Dashboard)
        detailsPlaceholder = new ScrollPane();
        detailsPlaceholder.setFitToWidth(true);
        detailsPlaceholder.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailsPlaceholder.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        detailsPlaceholder.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox analyticsContainer = buildClassAnalyticsPanel();
        detailsPlaceholder.setContent(analyticsContainer);

        // State 2: Actual details layout
        detailsContent = new VBox(20);
        detailsContent.setVisible(false);
        detailsContent.setManaged(false);

        HBox detailsHeader = new HBox(12);
        detailsHeader.setAlignment(Pos.CENTER_LEFT);

        detailStudentName = new Label("Student Name");
        detailStudentName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        detailStudentName.setTextFill(Color.WHITE);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button backToAnalyticsBtn = styledBtn("📊 Class Overview", Theme.ACCENT);
        backToAnalyticsBtn.setStyle(backToAnalyticsBtn.getStyle() + " -fx-padding: 6 12; -fx-font-size: 11px;");
        backToAnalyticsBtn.setOnAction(e -> {
            selectedStudent = null;
            renderStudentCards(allStudents);
            showClassAnalytics();
        });

        detailsHeader.getChildren().addAll(detailStudentName, headerSpacer, backToAnalyticsBtn);

        HBox detailStats = new HBox(12);
        detailStats.setAlignment(Pos.CENTER_LEFT);
        detailStudentRank = new Label("Rank");
        detailStudentRank.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        detailStudentRank.setTextFill(Color.web(Theme.ACCENT));
        detailStudentRank.setStyle("-fx-background-color: " + Theme.PRIMARY + "1c; -fx-border-color: " + Theme.ACCENT + "50; -fx-border-radius: 8; -fx-padding: 4 12;");

        detailStudentXP = new Label("0 XP");
        detailStudentXP.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        detailStudentXP.setTextFill(Color.web(Theme.SUCCESS));
        detailStudentXP.setStyle("-fx-background-color: #27ae601c; -fx-border-color: #27ae6050; -fx-border-radius: 8; -fx-padding: 4 12;");

        detailStudentViolations = new Label("0 Violations");
        detailStudentViolations.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        detailStudentViolations.setTextFill(Color.web(Theme.SUCCESS));
        detailStudentViolations.setStyle("-fx-background-color: #27ae601c; -fx-border-color: #27ae6050; -fx-border-radius: 8; -fx-padding: 4 12;");

        detailStudentTotalScore = new Label("Total Score: 0");
        detailStudentTotalScore.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        detailStudentTotalScore.setTextFill(Color.web(Theme.PRIMARY2));
        detailStudentTotalScore.setStyle("-fx-background-color: #0d47a11c; -fx-border-color: #0d47a150; -fx-border-radius: 8; -fx-padding: 4 12;");

        detailStats.getChildren().addAll(detailStudentRank, detailStudentXP, detailStudentViolations, detailStudentTotalScore);

        solvedTitle = new Label("Challenge Tracker (Solved problems are highlighted in green)");
        solvedTitle.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        solvedTitle.setTextFill(Color.web(Theme.TEXT_MUTED));

        solvedGrid = new GridPane();
        solvedGrid.setHgap(10);
        solvedGrid.setVgap(10);
        solvedGrid.setPadding(new Insets(10, 0, 10, 0));

        detailsContent.getChildren().addAll(detailsHeader, detailStats, new Separator(), solvedTitle, solvedGrid);

        rightPanel.getChildren().addAll(detailsPlaceholder, detailsContent);

        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.44);

        root.setCenter(splitPane);
        rootStack.getChildren().addAll(bgPane, root);
        return rootStack;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UI RENDERERS & DATA UPDATERS
    // ═══════════════════════════════════════════════════════════════════════

    private HBox buildStatsRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);

        int totalCount = allStudents.size();
        double avgXP = allStudents.isEmpty() ? 0 : allStudents.stream().mapToInt(s -> s.xp).average().orElse(0);
        
        VBox card1 = createMiniStatCard("TOTAL STUDENTS", String.valueOf(totalCount), Theme.ACCENT);
        VBox card2 = createMiniStatCard("AVERAGE XP", ((int)avgXP) + " XP", Theme.SUCCESS);

        row.getChildren().addAll(card1, card2);
        return row;
    }

    private VBox createMiniStatCard(String labelText, String valText, String colorHex) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle(
            "-fx-background-color: #0f184766;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #1e2c56bb;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;"
        );

        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        lbl.setTextFill(Color.web(Theme.TEXT_MUTED));

        Label val = new Label(valText);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        val.setTextFill(Color.web(colorHex));

        card.getChildren().addAll(lbl, val);
        return card;
    }

    private void renderStudentCards(List<DatabaseManager.StudentInfo> students) {
        studentCardsContainer.getChildren().clear();
        if (students.isEmpty()) {
            Label noStudents = new Label("No registered students found.");
            noStudents.setTextFill(Color.web(Theme.TEXT_MUTED));
            noStudents.setFont(Font.font("Arial", 13));
            noStudents.setPadding(new Insets(14));
            studentCardsContainer.getChildren().add(noStudents);
            return;
        }

        for (DatabaseManager.StudentInfo student : students) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(14, 16, 14, 16));
            card.setCursor(javafx.scene.Cursor.HAND);
            
            // Set styles based on selection
            boolean isSelected = selectedStudent != null && selectedStudent.username.equals(student.username);
            updateCardStyle(card, isSelected);

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            Label nameLbl = new Label("👤  " + student.username);
            nameLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            nameLbl.setTextFill(Color.WHITE);

            Label violationBadge = null;
            if (student.violations > 0) {
                violationBadge = new Label("⚠️ " + student.violations);
                violationBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
                violationBadge.setTextFill(Color.web(Theme.DANGER));
                violationBadge.setStyle("-fx-background-color: #e74c3c1a; -fx-border-color: #e74c3c44; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 1 4;");
                Tooltip.install(violationBadge, new Tooltip("Student logged " + student.violations + " focus violations during challenge mode."));
            }

            Region hSpacer = new Region();
            HBox.setHgrow(hSpacer, Priority.ALWAYS);

            Label progressLbl = new Label(student.solvedCount + " / " + ProblemManager.getAll().size() + " solved");
            progressLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            progressLbl.setTextFill(Color.web(Theme.ACCENT));

            header.getChildren().add(nameLbl);
            if (violationBadge != null) {
                header.getChildren().add(violationBadge);
            }
            header.getChildren().addAll(hSpacer, progressLbl);

            HBox footer = new HBox(8);
            footer.setAlignment(Pos.CENTER_LEFT);
            
            Label rankLbl = new Label(student.rank);
            rankLbl.setFont(Font.font("Arial", 11));
            rankLbl.setTextFill(Color.web(Theme.TEXT_MUTED));

            Region fSpacer = new Region();
            HBox.setHgrow(fSpacer, Priority.ALWAYS);

            Label xpLbl = new Label(student.xp + " XP");
            xpLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            xpLbl.setTextFill(Color.web(Theme.SUCCESS));

            footer.getChildren().addAll(rankLbl, fSpacer, xpLbl);

            card.getChildren().addAll(header, footer);

            // Card click behavior
            card.setOnMouseClicked(e -> {
                if (selectedStudent != null && selectedStudent.username.equals(student.username)) {
                    selectedStudent = null; // deselect
                    renderStudentCards(students);
                    showClassAnalytics();
                } else {
                    selectedStudent = student;
                    renderStudentCards(students);
                    showStudentDetails(student);
                }
            });

            // Card hover highlights
            card.setOnMouseEntered(e -> {
                if (selectedStudent == null || !selectedStudent.username.equals(student.username)) {
                    card.setStyle(
                        "-fx-background-color: " + Theme.SURFACE + "aa;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + Theme.ACCENT + "aa;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;"
                    );
                }
            });
            card.setOnMouseExited(e -> {
                boolean active = selectedStudent != null && selectedStudent.username.equals(student.username);
                updateCardStyle(card, active);
            });

            studentCardsContainer.getChildren().add(card);
        }
    }

    private void updateCardStyle(VBox card, boolean isSelected) {
        if (isSelected) {
            card.setStyle(
                "-fx-background-color: " + Theme.PRIMARY + "1c;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: " + Theme.ACCENT + ";" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 10;"
            );
        } else {
            card.setStyle(
                "-fx-background-color: " + Theme.SURFACE + "44;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: " + Theme.BORDER + "aa;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 10;"
            );
        }
    }

    private void filterStudents(String query) {
        if (query == null || query.trim().isEmpty()) {
            renderStudentCards(allStudents);
            return;
        }
        String q = query.toLowerCase().trim();
        List<DatabaseManager.StudentInfo> filtered = allStudents.stream()
            .filter(s -> s.username.toLowerCase().contains(q))
            .collect(Collectors.toList());
        renderStudentCards(filtered);
    }

    private void showStudentDetails(DatabaseManager.StudentInfo student) {
        detailsPlaceholder.setVisible(false);
        detailsPlaceholder.setManaged(false);

        detailsContent.setVisible(true);
        detailsContent.setManaged(true);

        detailStudentName.setText(student.username);
        detailStudentRank.setText("Rank: " + student.rank);
        detailStudentXP.setText("XP: " + student.xp);

        if (student.violations > 0) {
            detailStudentViolations.setTextFill(Color.web(Theme.DANGER));
            detailStudentViolations.setStyle("-fx-background-color: #e74c3c1c; -fx-border-color: #e74c3c50; -fx-border-radius: 8; -fx-padding: 4 12;");
            detailStudentViolations.setText("🚫 Focus Violations: " + student.violations);
        } else {
            detailStudentViolations.setTextFill(Color.web(Theme.SUCCESS));
            detailStudentViolations.setStyle("-fx-background-color: #27ae601c; -fx-border-color: #27ae6050; -fx-border-radius: 8; -fx-padding: 4 12;");
            detailStudentViolations.setText("🛡️ Focus Integrity: OK");
        }

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        
        Button resetStudentBtn = styledBtn("🔄 Reset Progress", Theme.WARN);
        resetStudentBtn.setStyle(resetStudentBtn.getStyle() + " -fx-padding: 6 12; -fx-font-size: 11px;");
        resetStudentBtn.setOnAction(e -> confirmResetStudent(student.username));
        
        Button deleteStudentBtn = styledBtn("🗑️ Delete Student", Theme.DANGER);
        deleteStudentBtn.setStyle(deleteStudentBtn.getStyle() + " -fx-padding: 6 12; -fx-font-size: 11px;");
        deleteStudentBtn.setOnAction(e -> confirmDeleteStudent(student.username));
        
        actionRow.getChildren().addAll(resetStudentBtn, deleteStudentBtn);

        // Remove previous dynamic children to prevent duplicates/stale nodes
        if (detailsContent.getChildren().size() > 2) {
            detailsContent.getChildren().remove(2, detailsContent.getChildren().size());
        }

        // Build the checkmark grid of all 20 challenges
        solvedGrid.getChildren().clear();
        int cols = 4;
        List<ProblemManager.Problem> allProbs = ProblemManager.getAll();

        for (int i = 0; i < allProbs.size(); i++) {
            ProblemManager.Problem p = allProbs.get(i);
            boolean solved = student.solvedList.contains(p.id);

            Label badge = new Label(p.id + (solved ? "  ✅" : "  ❌"));
            badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            badge.setWrapText(false);
            badge.setAlignment(Pos.CENTER);
            badge.setPrefWidth(90);

            Tooltip tooltip = new Tooltip(p.id + ". " + p.title + " (" + p.difficulty.toUpperCase() + ")");
            badge.setTooltip(tooltip);

            if (solved) {
                badge.setTextFill(Color.web(Theme.SUCCESS));
                badge.setStyle(
                    "-fx-background-color: " + Theme.SUCCESS + "14;" +
                    "-fx-border-color: " + Theme.SUCCESS + "aa;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;" +
                    "-fx-padding: 8 10;" +
                    "-fx-cursor: hand;"
                );
                badge.setOnMouseClicked(e -> showAnswerCode(student.username, p.id, p.title));
            } else {
                badge.setTextFill(Color.web(Theme.TEXT_LIGHT + "88"));
                badge.setStyle(
                    "-fx-background-color: " + Theme.NAV_BG + "22;" +
                    "-fx-border-color: " + Theme.BORDER + "aa;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;" +
                    "-fx-padding: 8 10;"
                );
            }

            int row = i / cols;
            int col = i % cols;
            solvedGrid.add(badge, col, row);
        }

        Label customTitle = new Label("Class Assignments");
        customTitle.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        customTitle.setTextFill(Color.web(Theme.TEXT_MUTED));

        GridPane customGrid = new GridPane();
        customGrid.setHgap(10);
        customGrid.setVgap(10);
        customGrid.setPadding(new Insets(10, 0, 10, 0));

        List<DatabaseManager.CustomTask> tasks = App.db.getCustomTasksForClass(student.getClassCode());
        for (int i = 0; i < tasks.size(); i++) {
            DatabaseManager.CustomTask task = tasks.get(i);
            boolean submitted = App.db.getAnswer(student.username, task.id) != null;
            Integer grade = App.db.getGrade(student.username, task.id);
            boolean rejected = grade != null && grade == -1;

            Label badge = new Label("T" + task.id + (grade != null && !rejected ? " ✅" : (rejected ? " 🔁" : (submitted ? " 📝" : " ❌"))));
            badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            badge.setWrapText(false);
            badge.setAlignment(Pos.CENTER);
            badge.setPrefWidth(90);
            badge.setTooltip(new Tooltip(task.title));

            if (grade != null && !rejected) {
                badge.setTextFill(Color.web(Theme.SUCCESS));
                badge.setStyle("-fx-background-color: " + Theme.SUCCESS + "14; -fx-border-color: " + Theme.SUCCESS + "aa; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 10; -fx-cursor: hand;");
            } else if (rejected) {
                badge.setTextFill(Color.web(Theme.DANGER));
                badge.setStyle("-fx-background-color: " + Theme.DANGER + "14; -fx-border-color: " + Theme.DANGER + "aa; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 10; -fx-cursor: hand;");
            } else if (submitted) {
                badge.setTextFill(Color.web(Theme.WARN));
                badge.setStyle("-fx-background-color: " + Theme.WARN + "14; -fx-border-color: " + Theme.WARN + "aa; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 10; -fx-cursor: hand;");
            } else {
                badge.setTextFill(Color.web(Theme.TEXT_LIGHT + "88"));
                badge.setStyle("-fx-background-color: " + Theme.NAV_BG + "22; -fx-border-color: " + Theme.BORDER + "aa; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 10;");
            }

            if (submitted || rejected) {
                badge.setOnMouseClicked(e -> showGradingDialog(student.username, task));
            }

            customGrid.add(badge, i % cols, i / cols);
        }

        detailsContent.getChildren().addAll(actionRow, new Separator(), solvedTitle, solvedGrid, new Separator(), customTitle, customGrid);
        
        // Update total score
        detailStudentTotalScore.setText("Total Score: " + App.db.getTotalScore(student.username, student.getClassCode()));
    }

    private void showAnswerCode(String username, int problemId, String problemTitle) {
        String code = App.db.getAnswer(username, problemId);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Student Answer");
        alert.setHeaderText("Problem " + problemId + ": " + problemTitle + "\nStudent: " + username);
        
        TextArea textArea = new TextArea(code);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px; -fx-control-inner-background: #080d28; -fx-text-fill: #a0d1ff;");
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setContent(expContent);
        
        DialogPane dp = alert.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.PRIMARY + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        javafx.scene.Node headerPanel = dp.lookup(".header-panel");
        if (headerPanel != null) headerPanel.setStyle("-fx-background-color: " + Theme.NAV_BG + ";");

        alert.showAndWait();
    }

    private void confirmLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Logout of Teacher Portal?");
        alert.setContentText("You will return to the welcome screen.");
        
        DialogPane dp = alert.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.PRIMARY + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        javafx.scene.Node headerPanel = dp.lookup(".header-panel");
        if (headerPanel != null) headerPanel.setStyle("-fx-background-color: " + Theme.NAV_BG + ";");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                App.db.logout();
                App.changeScene(new LoginView().getView());
            }
        });
    }

    private void loadStudentsForClass(String classCode) {
        allStudents = App.db.getAllStudentsInfo().stream()
            .filter(s -> classCode != null && classCode.equals(s.getClassCode()))
            .collect(Collectors.toList());
        if (studentCardsContainer != null) {
            renderStudentCards(allStudents);
            updateStatsRow();
        }
    }

    private void updateStatsRow() {
        if (statsRow != null) {
            // We need to re-render the stats row with the new allStudents
            statsRow.getChildren().clear();
            HBox newRow = buildStatsRow();
            statsRow.getChildren().addAll(newRow.getChildren());
        }
    }

    private void showNewClassDialog(String username, ComboBox<String> classDropdown) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create New Class");
        dialog.setHeaderText("Enter a unique code for your new class:");
        dialog.setContentText("Class Code:");

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.PRIMARY + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        javafx.scene.Node headerPanel = dp.lookup(".header-panel");
        if (headerPanel != null) headerPanel.setStyle("-fx-background-color: " + Theme.NAV_BG + ";");

        dialog.showAndWait().ifPresent(code -> {
            if (!code.trim().isEmpty()) {
                App.db.addTeacherClass(username, code.trim());
                classDropdown.getItems().clear();
                classDropdown.getItems().addAll(App.db.getTeacherClasses(username));
                classDropdown.getSelectionModel().select(code.trim());
            }
        });
    }

    private void showSettingsDialog(String classCode) {
        if (classCode == null || classCode.isEmpty()) return;
        
        TextInputDialog dialog = new TextInputDialog(String.valueOf(App.db.getClassMaxWarnings(classCode)));
        dialog.setTitle("Class Settings");
        dialog.setHeaderText("Set maximum allowed focus violations (Alt-Tabs) for " + classCode);
        dialog.setContentText("Max Violations:");

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.PRIMARY + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        javafx.scene.Node headerPanel = dp.lookup(".header-panel");
        if (headerPanel != null) headerPanel.setStyle("-fx-background-color: " + Theme.NAV_BG + ";");

        dialog.showAndWait().ifPresent(val -> {
            try {
                int limit = Integer.parseInt(val.trim());
                if (limit >= 0) {
                    App.db.setClassMaxWarnings(classCode, limit);
                }
            } catch (Exception ignored) {}
        });
    }

    private void showCreateTaskDialog(String classCode) {
        if (classCode == null || classCode.isEmpty()) return;
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Custom Task");
        dialog.setHeaderText("Create a new assignment for class: " + classCode);
        
        DialogPane dp = dialog.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.PRIMARY + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        javafx.scene.Node headerPanel = dp.lookup(".header-panel");
        if (headerPanel != null) headerPanel.setStyle("-fx-background-color: " + Theme.NAV_BG + ";");
        
        dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        TextField titleField = new TextField();
        titleField.setPromptText("Task Title (e.g. Implement AVL Tree)");
        titleField.setStyle("-fx-background-color: #1e2c56; -fx-text-fill: white; -fx-padding: 8;");
        
        TextArea descField = new TextArea();
        descField.setPromptText("Task Description / Instructions");
        descField.setWrapText(true);
        descField.setStyle("-fx-control-inner-background: #1e2c56; -fx-text-fill: white;");
        
        VBox content = new VBox(10, new Label("Title:"), titleField, new Label("Description:"), descField);
        dp.setContent(content);
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK && !titleField.getText().trim().isEmpty()) {
                App.db.createCustomTask(classCode, titleField.getText().trim(), descField.getText().trim());
                if (selectedStudent != null) {
                    showStudentDetails(selectedStudent);
                }
            }
        });
    }

    private void showGradingDialog(String username, DatabaseManager.CustomTask task) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Grade Assignment");
        dialog.setHeaderText("Grading: " + username + " - " + task.title);

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.PRIMARY + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        javafx.scene.Node headerPanel = dp.lookup(".header-panel");
        if (headerPanel != null) headerPanel.setStyle("-fx-background-color: " + Theme.NAV_BG + ";");

        ButtonType rejectBtnType = new ButtonType("Reject", ButtonBar.ButtonData.OTHER);
        dp.getButtonTypes().addAll(ButtonType.OK, rejectBtnType, ButtonType.CANCEL);

        TextArea codeArea = new TextArea();
        codeArea.setEditable(false);
        codeArea.setWrapText(true);
        codeArea.setPrefHeight(200);
        codeArea.setStyle("-fx-control-inner-background: #111a3a; -fx-text-fill: #a9b7c6; -fx-font-family: 'Consolas';");
        codeArea.setText(App.db.getAnswer(username, task.id));

        Integer existingGrade = App.db.getGrade(username, task.id);
        TextField gradeField = new TextField();
        gradeField.setPromptText("Enter Score (0-100)");
        gradeField.setStyle("-fx-background-color: #1e2c56; -fx-text-fill: white; -fx-padding: 8;");
        if (existingGrade != null && existingGrade != -1) {
            gradeField.setText(String.valueOf(existingGrade));
        }

        VBox content = new VBox(10, new Label("Student's Submitted Code:"), codeArea, new Label("Grade (Leave blank to reject):"), gradeField);
        dp.setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK && !gradeField.getText().trim().isEmpty()) {
                try {
                    int score = Integer.parseInt(gradeField.getText().trim());
                    App.db.setGrade(username, task.id, score);
                    if (selectedStudent != null) {
                        showStudentDetails(selectedStudent);
                    }
                } catch (Exception ignored) {}
            } else if (result == rejectBtnType) {
                App.db.setGrade(username, task.id, -1);
                if (selectedStudent != null) {
                    showStudentDetails(selectedStudent);
                }
            }
        });
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

    private void showInviteStudentDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Invite Student");
        dialog.setHeaderText("Create a new student account");

        DialogPane dp = dialog.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.PRIMARY + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 30, 10, 20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setStyle("-fx-background-color: " + Theme.NAV_BG + "; -fx-text-fill: white; -fx-prompt-text-fill: " + Theme.TEXT_MUTED + "; -fx-border-color: " + Theme.BORDER + "; -fx-border-radius: 4; -fx-background-radius: 4;");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle("-fx-background-color: " + Theme.NAV_BG + "; -fx-text-fill: white; -fx-prompt-text-fill: " + Theme.TEXT_MUTED + "; -fx-border-color: " + Theme.BORDER + "; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label userLbl = new Label("Username:");
        userLbl.setStyle("-fx-text-fill: white;");
        Label passLbl = new Label("Password:");
        passLbl.setStyle("-fx-text-fill: white;");

        grid.add(userLbl, 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(passLbl, 0, 1);
        grid.add(passwordField, 1, 1);

        dp.setContent(grid);

        ButtonType inviteButtonType = new ButtonType("Create Student", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(inviteButtonType, ButtonType.CANCEL);

        javafx.scene.Node inviteButton = dp.lookupButton(inviteButtonType);
        inviteButton.setStyle(
            "-fx-background-color: " + Theme.SUCCESS + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        );

        javafx.scene.Node cancelButton = dp.lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle(
            "-fx-background-color: " + Theme.DANGER + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == inviteButtonType) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    showErrorAlert("Validation Error", "Username and Password cannot be empty.");
                    return;
                }

                boolean success = App.db.register(username, password, "student");
                if (success) {
                    showSuccessAlert("Success", "Student account '" + username + "' created successfully.");
                    refreshStudentList();
                } else {
                    showErrorAlert("Registration Failed", "Username '" + username + "' is already taken.");
                }
            }
        });
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        DialogPane dp = alert.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.DANGER + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        DialogPane dp = alert.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.SUCCESS + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        alert.showAndWait();
    }

    private VBox buildEmptyStatePlaceholder() {
        VBox placeholder = new VBox(16);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(60, 40, 60, 40));
        placeholder.setStyle(
            "-fx-background-color: #0f184744;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #1e2c5688;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 12;"
        );

        Label icon = new Label("🎓");
        icon.setFont(Font.font("Segoe UI", 48));
        
        Label title = new Label("No Students Enrolled");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);

        Label desc = new Label("Enroll students to start tracking their progress, XP, and focus metrics.");
        desc.setFont(Font.font("Arial", 13));
        desc.setTextFill(Color.web(Theme.TEXT_MUTED));
        desc.setTextAlignment(TextAlignment.CENTER);
        desc.setWrapText(true);
        desc.setMaxWidth(320);

        Label hint = new Label("Click \"Invite Student\" in the navigation bar to create student accounts.");
        hint.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        hint.setTextFill(Color.web(Theme.ACCENT));
        hint.setTextAlignment(TextAlignment.CENTER);
        hint.setWrapText(true);
        hint.setMaxWidth(320);

        placeholder.getChildren().addAll(icon, title, desc, hint);
        return placeholder;
    }

    private VBox buildClassAnalyticsPanel() {
        VBox contentBox = new VBox(24);
        contentBox.setPadding(new Insets(10, 10, 10, 10));
        contentBox.setStyle("-fx-background-color: transparent;");

        Label dashboardTitle = new Label("📈 Class Visual Analytics");
        dashboardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        dashboardTitle.setTextFill(Color.WHITE);

        Label dashboardSubtitle = new Label("Class-wide metrics and concept completion rates. Click a student card to select, click again to return here.");
        dashboardSubtitle.setFont(Font.font("Arial", 12));
        dashboardSubtitle.setTextFill(Color.web(Theme.TEXT_MUTED));

        VBox titleBox = new VBox(4, dashboardTitle, dashboardSubtitle);
        contentBox.getChildren().add(titleBox);

        if (allStudents.isEmpty()) {
            VBox emptyState = buildEmptyStatePlaceholder();
            contentBox.getChildren().add(emptyState);
        } else {
            VBox chartsBox = buildClassAnalyticsCharts();
            contentBox.getChildren().add(chartsBox);
        }

        return contentBox;
    }

    private void refreshStudentList() {
        allStudents = App.db.getAllStudentsInfo();
        filterStudents(""); // Re-render the student list cards
        
        // Refresh total stats values
        statsRow.getChildren().clear();
        int totalCount = allStudents.size();
        double avgXP = allStudents.isEmpty() ? 0 : allStudents.stream().mapToInt(s -> s.xp).average().orElse(0);
        VBox card1 = createMiniStatCard("TOTAL STUDENTS", String.valueOf(totalCount), Theme.ACCENT);
        VBox card2 = createMiniStatCard("AVERAGE XP", ((int)avgXP) + " XP", Theme.SUCCESS);
        statsRow.getChildren().addAll(card1, card2);

        // Refresh visual analytics charts if active
        if (detailsPlaceholder.isVisible()) {
            VBox analyticsContainer = buildClassAnalyticsPanel();
            detailsPlaceholder.setContent(analyticsContainer);
        }

        // Sync right-hand detailed view of the currently selected student
        if (selectedStudent != null) {
            String selUser = selectedStudent.username;
            selectedStudent = allStudents.stream()
                .filter(s -> s.username.equals(selUser))
                .findFirst()
                .orElse(null);
            if (selectedStudent != null) {
                showStudentDetails(selectedStudent);
            } else {
                showClassAnalytics();
            }
        }
    }

    private VBox buildClassAnalyticsCharts() {
        VBox box = new VBox(24);
        box.setStyle("-fx-background-color: transparent;");

        HBox row1 = new HBox(16);
        row1.setAlignment(Pos.CENTER);

        // XP Leaderboard
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Student");
        xAxis.setTickLabelFill(Color.web(Theme.TEXT_MUTED));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("XP Earned");
        yAxis.setTickLabelFill(Color.web(Theme.TEXT_MUTED));

        BarChart<String, Number> xpChart = new BarChart<>(xAxis, yAxis);
        xpChart.setTitle("XP Leaderboard");
        xpChart.setLegendVisible(false);
        xpChart.setPrefSize(320, 240);
        xpChart.setMinSize(320, 240);
        xpChart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> xpSeries = new XYChart.Series<>();
        List<DatabaseManager.StudentInfo> sortedStudents = allStudents.stream()
            .sorted((s1, s2) -> Integer.compare(s2.xp, s1.xp))
            .limit(5)
            .collect(Collectors.toList());

        for (DatabaseManager.StudentInfo student : sortedStudents) {
            xpSeries.getData().add(new XYChart.Data<>(student.username, student.xp));
        }
        xpChart.getData().add(xpSeries);

        // Difficulty Breakdown Pie Chart
        PieChart pieChart = new PieChart();
        pieChart.setTitle("Difficulty Distribution");
        pieChart.setPrefSize(300, 240);
        pieChart.setMinSize(300, 240);
        pieChart.setLegendVisible(true);
        pieChart.setStyle("-fx-background-color: transparent;");

        int easyCount = 0;
        int mediumCount = 0;
        int hardCount = 0;

        for (DatabaseManager.StudentInfo s : allStudents) {
            for (int probId : s.solvedList) {
                ProblemManager.Problem p = ProblemManager.get(probId);
                if (p != null) {
                    if (p.difficulty.equalsIgnoreCase("easy")) easyCount++;
                    else if (p.difficulty.equalsIgnoreCase("medium")) mediumCount++;
                    else hardCount++;
                }
            }
        }

        if (easyCount > 0 || mediumCount > 0 || hardCount > 0) {
            pieChart.getData().addAll(
                new PieChart.Data("Easy", easyCount),
                new PieChart.Data("Medium", mediumCount),
                new PieChart.Data("Hard", hardCount)
            );
        } else {
            pieChart.getData().add(new PieChart.Data("No Solved Problems Yet", 1));
        }

        row1.getChildren().addAll(xpChart, pieChart);

        HBox row2 = new HBox(16);
        row2.setAlignment(Pos.CENTER);

        // Concept Solving Frequency Rate
        CategoryAxis xAxisProb = new CategoryAxis();
        xAxisProb.setLabel("Problem ID");
        xAxisProb.setTickLabelFill(Color.web(Theme.TEXT_MUTED));

        NumberAxis yAxisProb = new NumberAxis();
        yAxisProb.setLabel("Students Completed");
        yAxisProb.setTickLabelFill(Color.web(Theme.TEXT_MUTED));
        yAxisProb.setMinorTickVisible(false);
        yAxisProb.setTickUnit(1);

        BarChart<String, Number> probChart = new BarChart<>(xAxisProb, yAxisProb);
        probChart.setTitle("Concept Solving Frequency");
        probChart.setLegendVisible(false);
        probChart.setPrefSize(320, 240);
        probChart.setMinSize(320, 240);
        probChart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> probSeries = new XYChart.Series<>();
        java.util.Map<Integer, Integer> solveCounts = new java.util.HashMap<>();
        List<ProblemManager.Problem> allProbs = ProblemManager.getAll();
        for (ProblemManager.Problem p : allProbs) {
            solveCounts.put(p.id, 0);
        }

        for (DatabaseManager.StudentInfo s : allStudents) {
            for (int probId : s.solvedList) {
                solveCounts.put(probId, solveCounts.getOrDefault(probId, 0) + 1);
            }
        }

        for (ProblemManager.Problem p : allProbs) {
            int count = solveCounts.getOrDefault(p.id, 0);
            probSeries.getData().add(new XYChart.Data<>(String.valueOf(p.id), count));
        }
        probChart.getData().add(probSeries);

        // Focus Violations Tracker Bar Chart
        CategoryAxis xAxisViolations = new CategoryAxis();
        xAxisViolations.setLabel("Student");
        xAxisViolations.setTickLabelFill(Color.web(Theme.TEXT_MUTED));

        NumberAxis yAxisViolations = new NumberAxis();
        yAxisViolations.setLabel("Violations");
        yAxisViolations.setTickLabelFill(Color.web(Theme.TEXT_MUTED));
        yAxisViolations.setMinorTickVisible(false);
        yAxisViolations.setTickUnit(1);

        BarChart<String, Number> violationsChart = new BarChart<>(xAxisViolations, yAxisViolations);
        violationsChart.setTitle("Focus Violations Tracker");
        violationsChart.setLegendVisible(false);
        violationsChart.setPrefSize(300, 240);
        violationsChart.setMinSize(300, 240);
        violationsChart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> violationsSeries = new XYChart.Series<>();
        int totalViolations = 0;
        for (DatabaseManager.StudentInfo student : allStudents) {
            violationsSeries.getData().add(new XYChart.Data<>(student.username, student.violations));
            totalViolations += student.violations;
        }
        violationsChart.getData().add(violationsSeries);

        // StackPane container for Violations Chart and "All Quiet" overlay label
        StackPane violationsChartStack = new StackPane();
        violationsChartStack.setPrefSize(300, 240);
        violationsChartStack.getChildren().add(violationsChart);

        if (totalViolations == 0) {
            Label noViolationsLbl = new Label("✨ All Quiet: No Violations logged!");
            noViolationsLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            noViolationsLbl.setTextFill(Color.web(Theme.SUCCESS));
            noViolationsLbl.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.85);" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: " + Theme.SUCCESS + ";" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 8;" +
                "-fx-padding: 8 16;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 8, 0, 0, 1);"
            );
            violationsChartStack.getChildren().add(noViolationsLbl);
            StackPane.setAlignment(noViolationsLbl, Pos.CENTER);
        }

        row2.getChildren().addAll(probChart, violationsChartStack);

        box.getChildren().addAll(row1, row2);

        applyChartThemeStyles(xpChart, probChart, violationsChart, pieChart);

        return box;
    }

    private void applyChartThemeStyles(XYChart<?, ?> chart1, XYChart<?, ?> chart2, XYChart<?, ?> chart3, PieChart pie) {
        chart1.lookupAll(".chart-plot-background").forEach(n -> n.setStyle("-fx-background-color: transparent;"));
        chart2.lookupAll(".chart-plot-background").forEach(n -> n.setStyle("-fx-background-color: transparent;"));
        chart3.lookupAll(".chart-plot-background").forEach(n -> n.setStyle("-fx-background-color: transparent;"));

        chart1.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"));
        chart2.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"));
        chart3.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"));
        pie.lookupAll(".chart-title").forEach(n -> n.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"));

        chart1.lookupAll(".axis").forEach(n -> n.setStyle("-fx-tick-label-fill: " + Theme.TEXT_MUTED + "; -fx-text-fill: " + Theme.TEXT_MUTED + ";"));
        chart2.lookupAll(".axis").forEach(n -> n.setStyle("-fx-tick-label-fill: " + Theme.TEXT_MUTED + "; -fx-text-fill: " + Theme.TEXT_MUTED + ";"));
        chart3.lookupAll(".axis").forEach(n -> n.setStyle("-fx-tick-label-fill: " + Theme.TEXT_MUTED + "; -fx-text-fill: " + Theme.TEXT_MUTED + ";"));
        
        chart1.lookupAll(".chart-vertical-grid-lines").forEach(n -> n.setStyle("-fx-stroke: transparent;"));
        chart1.lookupAll(".chart-horizontal-grid-lines").forEach(n -> n.setStyle("-fx-stroke: " + Theme.BORDER + "44;"));
        chart2.lookupAll(".chart-vertical-grid-lines").forEach(n -> n.setStyle("-fx-stroke: transparent;"));
        chart2.lookupAll(".chart-horizontal-grid-lines").forEach(n -> n.setStyle("-fx-stroke: " + Theme.BORDER + "44;"));
        chart3.lookupAll(".chart-vertical-grid-lines").forEach(n -> n.setStyle("-fx-stroke: transparent;"));
        chart3.lookupAll(".chart-horizontal-grid-lines").forEach(n -> n.setStyle("-fx-stroke: " + Theme.BORDER + "44;"));

        javafx.application.Platform.runLater(() -> {
            chart1.lookupAll(".default-color0.chart-bar").forEach(n -> n.setStyle(
                "-fx-bar-fill: " + Theme.ACCENT + ";" +
                "-fx-background-radius: 4 4 0 0;"
            ));
            chart2.lookupAll(".default-color0.chart-bar").forEach(n -> n.setStyle(
                "-fx-bar-fill: " + Theme.PRIMARY + ";" +
                "-fx-background-radius: 2 2 0 0;"
            ));
            chart3.lookupAll(".default-color0.chart-bar").forEach(n -> n.setStyle(
                "-fx-bar-fill: " + Theme.DANGER + ";" +
                "-fx-background-radius: 4 4 0 0;"
            ));

            int index = 0;
            String[] sliceColors = {Theme.SUCCESS, Theme.WARN, Theme.DANGER};
            for (PieChart.Data d : pie.getData()) {
                if (d.getNode() != null) {
                    String color = sliceColors[index % sliceColors.length];
                    if (d.getName().contains("No Solved")) {
                        color = Theme.TEXT_MUTED;
                    }
                    d.getNode().setStyle("-fx-pie-color: " + color + "; -fx-border-color: " + Theme.SURFACE + "; -fx-border-width: 1.5;");
                }
                index++;
            }
        });
    }

    private void showClassAnalytics() {
        detailsContent.setVisible(false);
        detailsContent.setManaged(false);

        detailsPlaceholder.setVisible(true);
        detailsPlaceholder.setManaged(true);
        
        // Rebuild/repopulate class visual charts
        refreshStudentList();
    }

    private void confirmResetStudent(String username) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Student Progress");
        alert.setHeaderText("Reset progress for " + username + "?");
        alert.setContentText("This will clear all solved problems and focus violations for this student. This cannot be undone.");
        
        DialogPane dp = alert.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.WARN + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        javafx.scene.Node headerPanel = dp.lookup(".header-panel");
        if (headerPanel != null) headerPanel.setStyle("-fx-background-color: " + Theme.NAV_BG + ";");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                App.db.resetProgressForUser(username);
                showSuccessAlert("Success", "Progress reset for student '" + username + "'.");
                refreshStudentList();
            }
        });
    }

    private void confirmDeleteStudent(String username) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Student");
        alert.setHeaderText("Delete account '" + username + "'?");
        alert.setContentText("This will permanently delete the student account and all progress. This cannot be undone.");
        
        DialogPane dp = alert.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + Theme.SURFACE + ";" +
            "-fx-border-color: " + Theme.DANGER + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;"
        );
        dp.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
        javafx.scene.Node headerPanel = dp.lookup(".header-panel");
        if (headerPanel != null) headerPanel.setStyle("-fx-background-color: " + Theme.NAV_BG + ";");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                boolean success = App.db.deleteUser(username);
                if (success) {
                    showSuccessAlert("Success", "Student account '" + username + "' deleted.");
                    selectedStudent = null; // deselect since they are deleted
                    showClassAnalytics();
                } else {
                    showErrorAlert("Error", "Could not delete student account.");
                }
            }
        });
    }
}
