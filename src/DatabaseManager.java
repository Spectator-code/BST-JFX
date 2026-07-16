import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DatabaseManager {

    // ── Store files next to the JAR / working directory robustly ──────────
    private static final File BASE_DIR   = resolveBaseDir();
    private static final File USERS_FILE    = new File(BASE_DIR, "users.properties");
    private static final File PROGRESS_FILE = new File(BASE_DIR, "progress.properties");
    private static final File ANSWERS_FILE  = new File(BASE_DIR, "answers.properties");
    private static final File CLASSES_FILE  = new File(BASE_DIR, "classes.properties");
    private static final File TASKS_FILE    = new File(BASE_DIR, "tasks.properties");
    private static final File GRADES_FILE   = new File(BASE_DIR, "grades.properties");

    private Properties users    = new Properties();
    private Properties progress = new Properties();
    private Properties answers  = new Properties();
    private Properties classes  = new Properties();
    private Properties tasks    = new Properties();
    private Properties grades   = new Properties();
    private String currentUser  = null;

    public static class CustomTask {
        public int id;
        public String classCode;
        public String title;
        public String description;
        public CustomTask(int id, String classCode, String title, String description) {
            this.id = id; this.classCode = classCode; this.title = title; this.description = description;
        }
    }

    public DatabaseManager() {
        load();
    }

    // ── Resolve a reliable base directory ─────────────────────────────────
    private static File resolveBaseDir() {
        try {
            // Try to resolve relative to the location of the running class/jar
            File jarDir = new File(DatabaseManager.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            File candidate = jarDir.isDirectory() ? jarDir : jarDir.getParentFile();
            if (candidate != null && candidate.canWrite()) return candidate;
        } catch (Exception ignored) {}
        // Fallback: current working directory
        return new File(System.getProperty("user.dir", "."));
    }

    // ── Load ──────────────────────────────────────────────────────────────
    private void load() {
        try {
            if (USERS_FILE.exists()) {
                try (FileInputStream fis = new FileInputStream(USERS_FILE)) {
                    users.load(fis);
                }
            }
            if (PROGRESS_FILE.exists()) {
                try (FileInputStream fis = new FileInputStream(PROGRESS_FILE)) {
                    progress.load(fis);
                }
            }
            if (ANSWERS_FILE.exists()) {
                try (FileInputStream fis = new FileInputStream(ANSWERS_FILE)) {
                    answers.load(fis);
                }
            }
            if (CLASSES_FILE.exists()) {
                try (FileInputStream fis = new FileInputStream(CLASSES_FILE)) {
                    classes.load(fis);
                }
            }
            if (TASKS_FILE.exists()) {
                try (FileInputStream fis = new FileInputStream(TASKS_FILE)) {
                    tasks.load(fis);
                }
            }
            if (GRADES_FILE.exists()) {
                try (FileInputStream fis = new FileInputStream(GRADES_FILE)) {
                    grades.load(fis);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────
    private void save() {
        try {
            try (FileOutputStream fos = new FileOutputStream(USERS_FILE)) {
                users.store(fos, "User Database");
            }
            try (FileOutputStream fos = new FileOutputStream(PROGRESS_FILE)) {
                progress.store(fos, "Progress Database");
            }
            try (FileOutputStream fos = new FileOutputStream(ANSWERS_FILE)) {
                answers.store(fos, "Answers Database");
            }
            try (FileOutputStream fos = new FileOutputStream(CLASSES_FILE)) {
                classes.store(fos, "Classes Database");
            }
            try (FileOutputStream fos = new FileOutputStream(TASKS_FILE)) {
                tasks.store(fos, "Tasks Database");
            }
            try (FileOutputStream fos = new FileOutputStream(GRADES_FILE)) {
                grades.store(fos, "Grades Database");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String currentUserRole = "student";

    public static class StudentInfo {
        public String username;
        public int solvedCount;
        public int xp;
        public String rank;
        public List<Integer> solvedList;
        public int violations;
        public String classCode;

        public StudentInfo(String username, int solvedCount, int xp, String rank, List<Integer> solvedList, int violations, String classCode) {
            this.username = username;
            this.solvedCount = solvedCount;
            this.xp = xp;
            this.rank = rank;
            this.solvedList = solvedList;
            this.violations = violations;
            this.classCode = classCode;
        }

        // Getters for TableView property mapping
        public String getUsername() { return username; }
        public int getSolvedCount() { return solvedCount; }
        public int getXp() { return xp; }
        public String getRank() { return rank; }
        public int getViolations() { return violations; }
        public String getClassCode() { return classCode; }
    }

    // ── Register (Overloaded for legacy compatibility) ─────────────────────
    public boolean register(String username, String password) {
        return register(username, password, "student");
    }

    public boolean register(String username, String password, String role) {
        if (username == null) return false;
        String lowerUser = username.toLowerCase();
        if (users.containsKey(lowerUser)) return false;
        users.setProperty(lowerUser, hash(password) + ":" + role.toLowerCase());
        progress.setProperty(lowerUser, "");
        save();
        currentUser = lowerUser;
        currentUserRole = role.toLowerCase();
        return true;
    }

    // ── Login ─────────────────────────────────────────────────────────────
    public boolean login(String username, String password) {
        if (username == null) return false;
        String lowerUser = username.toLowerCase();
        String stored = users.getProperty(lowerUser);
        if (stored == null) return false;

        String passPart = stored;
        String rolePart = "student";
        if (stored.contains(":")) {
            int colonIndex = stored.indexOf(":");
            passPart = stored.substring(0, colonIndex);
            rolePart = stored.substring(colonIndex + 1);
        }

        // Support both hashed (new) and plain-text (legacy) passwords
        boolean match = passPart.equals(hash(password)) || passPart.equals(password);
        if (match) {
            // Upgrade plain-text password to hashed on first login
            if (passPart.equals(password)) {
                users.setProperty(lowerUser, hash(password) + ":" + rolePart);
                save();
            }
            currentUser = lowerUser;
            currentUserRole = rolePart.toLowerCase();
        }
        return match;
    }

    // ── Logout ────────────────────────────────────────────────────────────
    public void logout() {
        currentUser = null;
        currentUserRole = "student";
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public String getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }

    public List<Integer> getSolvedProblems() {
        return getSolvedProblemsForUser(currentUser);
    }

    public List<Integer> getSolvedProblemsForUser(String username) {
        if (username == null) return new ArrayList<>();
        String lowerUser = username.toLowerCase();
        String val = progress.getProperty(lowerUser, "");
        if (val.isEmpty()) return new ArrayList<>();
        List<Integer> solved = new ArrayList<>();
        for (String s : val.split(",")) {
            try { solved.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
        }
        return solved;
    }

    public void markProblemSolved(int id) {
        if (currentUser == null) return;
        List<Integer> solved = getSolvedProblems();
        if (!solved.contains(id)) {
            solved.add(id);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < solved.size(); i++) {
                sb.append(solved.get(i));
                if (i < solved.size() - 1) sb.append(",");
            }
            progress.setProperty(currentUser, sb.toString());
            save();
        }
    }

    // ── Reset progress for current user ──────────────────────────────────
    public void resetProgress() {
        if (currentUser == null) return;
        progress.setProperty(currentUser, "");
        progress.setProperty(currentUser + "_violations", "0");
        progress.setProperty(currentUser + "_lockout", "0");
        save();
    }

    // ── Return first unsolved problem id, or -1 if all done ──────────────
    public int getFirstUnsolvedProblemId() {
        List<Integer> solved = getSolvedProblems();
        for (ProblemManager.Problem p : ProblemManager.getAll()) {
            if (!solved.contains(p.id)) return p.id;
        }
        return -1;
    }

    // ── Rank title utility ────────────────────────────────────────────────
    public static String getRankTitle(int solved) {
        if (solved >= 18) return "🏆 Tree Grandmaster";
        if (solved >= 14) return "AVL Scholar";
        if (solved >= 10) return "BST Expert";
        if (solved >= 7)  return "BST Practitioner";
        if (solved >= 4)  return "BST Adept";
        if (solved >= 2)  return "BST Trainee";
        return "BST Novice";
    }

    // ── Retrieve all registered student info ──────────────────────────────
    public List<StudentInfo> getAllStudentsInfo() {
        List<StudentInfo> list = new ArrayList<>();
        for (String key : users.stringPropertyNames()) {
            String stored = users.getProperty(key);
            if (stored == null) continue;
            String rolePart = "student";
            if (stored.contains(":")) {
                rolePart = stored.substring(stored.indexOf(":") + 1).toLowerCase();
            }
            if (rolePart.equals("student")) {
                List<Integer> solvedList = getSolvedProblemsForUser(key);
                int solvedCount = solvedList.size();
                int xp = solvedCount * 100;
                String rank = getRankTitle(solvedCount);
                int violations = getViolationCount(key);
                String classCode = getClassCode(key);
                list.add(new StudentInfo(key, solvedCount, xp, rank, solvedList, violations, classCode));
            }
        }
        return list;
    }

    public void incrementViolationCount(String username) {
        if (username == null) return;
        String lowerUser = username.toLowerCase();
        int current = getViolationCount(lowerUser);
        progress.setProperty(lowerUser + "_violations", String.valueOf(current + 1));
        save();
    }

    public void setViolationCount(String username, int count) {
        if (username == null) return;
        String lowerUser = username.toLowerCase();
        progress.setProperty(lowerUser + "_violations", String.valueOf(count));
        save();
    }

    public int getViolationCount(String username) {
        if (username == null) return 0;
        String lowerUser = username.toLowerCase();
        String val = progress.getProperty(lowerUser + "_violations", "0");
        try {
            return java.lang.Integer.parseInt(val);
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Answers Storage ───────────────────────────────────────────────────
    public void saveAnswer(int problemId, String code) {
        if (currentUser == null || code == null) return;
        String b64 = Base64.getEncoder().encodeToString(code.getBytes(StandardCharsets.UTF_8));
        answers.setProperty(currentUser + "_" + problemId, b64);
        save();
    }

    public String getAnswer(String username, int problemId) {
        if (username == null) return null;
        String b64 = answers.getProperty(username.toLowerCase() + "_" + problemId);
        if (b64 == null) return "No answer saved.";
        try {
            return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return b64; // fallback if it wasn't encoded properly in the past
        }
    }

    // ── Classroom System ───────────────────────────────────────────────────
    public void setClassCode(String classCode) {
        if (currentUser == null) return;
        progress.setProperty(currentUser + "_class", classCode != null ? classCode.trim() : "");
        save();
    }

    public String getClassCode(String username) {
        if (username == null) return "";
        return progress.getProperty(username.toLowerCase() + "_class", "");
    }

    public List<String> getTeacherClasses(String username) {
        if (username == null) return new ArrayList<>();
        String val = classes.getProperty(username.toLowerCase() + "_classes", "");
        if (val.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(val.split(",")));
    }

    public void addTeacherClass(String username, String classCode) {
        if (username == null || classCode == null || classCode.trim().isEmpty()) return;
        List<String> list = getTeacherClasses(username);
        if (!list.contains(classCode.trim())) {
            list.add(classCode.trim());
            classes.setProperty(username.toLowerCase() + "_classes", String.join(",", list));
            // Default max warnings to 3 when creating a new class
            if (getClassMaxWarnings(classCode) == 3 && classes.getProperty(classCode.trim() + "_maxWarnings") == null) {
                setClassMaxWarnings(classCode, 3);
            }
            save();
        }
    }

    public void setClassMaxWarnings(String classCode, int max) {
        if (classCode == null) return;
        classes.setProperty(classCode.trim() + "_maxWarnings", String.valueOf(max));
        save();
    }

    public int getClassMaxWarnings(String classCode) {
        if (classCode == null || classCode.trim().isEmpty()) return 3;
        String val = classes.getProperty(classCode.trim() + "_maxWarnings", "3");
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return 3;
        }
    }

    // ── Custom Tasks ───────────────────────────────────────────────────────
    public int createCustomTask(String classCode, String title, String description) {
        if (classCode == null) return -1;
        
        // Find next ID >= 1000
        int nextId = 1000;
        String maxIdStr = tasks.getProperty("MAX_TASK_ID");
        if (maxIdStr != null) {
            try {
                nextId = Integer.parseInt(maxIdStr) + 1;
            } catch (Exception ignored) {}
        }
        
        tasks.setProperty("MAX_TASK_ID", String.valueOf(nextId));
        tasks.setProperty("task_" + nextId + "_class", classCode);
        tasks.setProperty("task_" + nextId + "_title", Base64.getEncoder().encodeToString(title.getBytes(StandardCharsets.UTF_8)));
        tasks.setProperty("task_" + nextId + "_desc", Base64.getEncoder().encodeToString(description.getBytes(StandardCharsets.UTF_8)));
        
        // Add to class task list
        String existingTasks = classes.getProperty(classCode + "_tasks", "");
        if (existingTasks.isEmpty()) {
            classes.setProperty(classCode + "_tasks", String.valueOf(nextId));
        } else {
            classes.setProperty(classCode + "_tasks", existingTasks + "," + nextId);
        }
        save();
        return nextId;
    }

    public List<CustomTask> getCustomTasksForClass(String classCode) {
        List<CustomTask> list = new ArrayList<>();
        if (classCode == null || classCode.isEmpty()) return list;
        
        String existingTasks = classes.getProperty(classCode + "_tasks", "");
        if (existingTasks.isEmpty()) return list;
        
        for (String idStr : existingTasks.split(",")) {
            try {
                int id = Integer.parseInt(idStr);
                CustomTask t = getCustomTask(id);
                if (t != null) list.add(t);
            } catch (Exception ignored) {}
        }
        return list;
    }

    public CustomTask getCustomTask(int taskId) {
        String cls = tasks.getProperty("task_" + taskId + "_class");
        if (cls == null) return null;
        
        String titleB64 = tasks.getProperty("task_" + taskId + "_title", "");
        String descB64 = tasks.getProperty("task_" + taskId + "_desc", "");
        
        String title = "";
        String desc = "";
        try {
            title = new String(Base64.getDecoder().decode(titleB64), StandardCharsets.UTF_8);
            desc = new String(Base64.getDecoder().decode(descB64), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        
        return new CustomTask(taskId, cls, title, desc);
    }

    // ── Grading System ─────────────────────────────────────────────────────
    public void setGrade(String username, int taskId, int score) {
        if (username == null) return;
        grades.setProperty(username.toLowerCase() + "_" + taskId + "_grade", String.valueOf(score));
        save();
    }

    public Integer getGrade(String username, int taskId) {
        if (username == null) return null;
        String val = grades.getProperty(username.toLowerCase() + "_" + taskId + "_grade");
        if (val == null) return null;
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return null;
        }
    }

    public int getTotalScore(String username, String classCode) {
        if (username == null || classCode == null) return 0;
        int total = 0;
        List<CustomTask> customTasks = getCustomTasksForClass(classCode);
        for (CustomTask t : customTasks) {
            Integer g = getGrade(username, t.id);
            if (g != null) {
                total += g;
            }
        }
        return total;
    }

    public void resetProgressForUser(String username) {
        if (username == null) return;
        String lowerUser = username.toLowerCase();
        progress.setProperty(lowerUser, "");
        progress.setProperty(lowerUser + "_violations", "0");
        progress.setProperty(lowerUser + "_lockout", "0");
        save();
    }

    public void clearViolationsForUser(String username) {
        if (username == null) return;
        String lowerUser = username.toLowerCase();
        progress.setProperty(lowerUser + "_violations", "0");
        progress.setProperty(lowerUser + "_lockout", "0");
        save();
    }

    public void setLockoutTimestamp(String username, long timestamp) {
        if (username == null) return;
        String lowerUser = username.toLowerCase();
        progress.setProperty(lowerUser + "_lockout", String.valueOf(timestamp));
        save();
    }

    public long getLockoutTimestamp(String username) {
        if (username == null) return 0;
        String lowerUser = username.toLowerCase();
        String val = progress.getProperty(lowerUser + "_lockout", "0");
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean deleteUser(String username) {
        if (username == null) return false;
        String lowerUser = username.toLowerCase();
        if (!users.containsKey(lowerUser)) return false;
        users.remove(lowerUser);
        progress.remove(lowerUser);
        progress.remove(lowerUser + "_violations");
        progress.remove(lowerUser + "_lockout");
        save();
        return true;
    }

    // ── SHA-256 hashing ───────────────────────────────────────────────────
    private static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java; this should never happen
            return input;
        }
    }
}
