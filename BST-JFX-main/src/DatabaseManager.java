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
            if (USERS_FILE.exists())    users.load(new FileInputStream(USERS_FILE));
            if (PROGRESS_FILE.exists()) progress.load(new FileInputStream(PROGRESS_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────
    private void save() {
        try {
            users.store(new FileOutputStream(USERS_FILE), "User Database");
            progress.store(new FileOutputStream(PROGRESS_FILE), "Progress Database");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Register ──────────────────────────────────────────────────────────
    public boolean register(String username, String password) {
        if (users.containsKey(username)) return false;
        users.setProperty(username, hash(password));
        progress.setProperty(username, "");
        save();
        currentUser = username;
        return true;
    }

    // ── Login ─────────────────────────────────────────────────────────────
    public boolean login(String username, String password) {
        String stored = users.getProperty(username);
        if (stored == null) return false;

        // Support both hashed (new) and plain-text (legacy) passwords
        boolean match = stored.equals(hash(password)) || stored.equals(password);
        if (match) {
            // Upgrade plain-text password to hashed on first login
            if (stored.equals(password)) {
                users.setProperty(username, hash(password));
                save();
            }
            currentUser = username;
        }
        return match;
    }

    // ── Logout ────────────────────────────────────────────────────────────
    public void logout() {
        currentUser = null;
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public String getCurrentUser() {
        return currentUser;
    }

    public List<Integer> getSolvedProblems() {
        if (currentUser == null) return new ArrayList<>();
        String val = progress.getProperty(currentUser, "");
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
