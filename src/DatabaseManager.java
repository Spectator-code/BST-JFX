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

    private Properties users    = new Properties();
    private Properties progress = new Properties();
    private String currentUser  = null;

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

        public StudentInfo(String username, int solvedCount, int xp, String rank, List<Integer> solvedList, int violations) {
            this.username = username;
            this.solvedCount = solvedCount;
            this.xp = xp;
            this.rank = rank;
            this.solvedList = solvedList;
            this.violations = violations;
        }

        // Getters for TableView property mapping
        public String getUsername() { return username; }
        public int getSolvedCount() { return solvedCount; }
        public int getXp() { return xp; }
        public String getRank() { return rank; }
        public int getViolations() { return violations; }
    }

    // ── Register (Overloaded for legacy compatibility) ─────────────────────
    public boolean register(String username, String password) {
        return register(username, password, "student");
    }

    public boolean register(String username, String password, String role) {
        if (users.containsKey(username)) return false;
        users.setProperty(username, hash(password) + ":" + role.toLowerCase());
        progress.setProperty(username, "");
        save();
        currentUser = username;
        currentUserRole = role.toLowerCase();
        return true;
    }

    // ── Login ─────────────────────────────────────────────────────────────
    public boolean login(String username, String password) {
        String stored = users.getProperty(username);
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
                users.setProperty(username, hash(password) + ":" + rolePart);
                save();
            }
            currentUser = username;
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
        String val = progress.getProperty(username, "");
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
                list.add(new StudentInfo(key, solvedCount, xp, rank, solvedList, violations));
            }
        }
        return list;
    }

    public void incrementViolationCount(String username) {
        if (username == null) return;
        int current = getViolationCount(username);
        progress.setProperty(username + "_violations", String.valueOf(current + 1));
        save();
    }

    public int getViolationCount(String username) {
        if (username == null) return 0;
        String val = progress.getProperty(username + "_violations", "0");
        try {
            return java.lang.Integer.parseInt(val);
        } catch (Exception e) {
            return 0;
        }
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
