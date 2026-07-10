import java.io.*;
import java.util.*;

public class DatabaseManager {
    private static final String USERS_FILE = "users.properties";
    private static final String PROGRESS_FILE = "progress.properties";

    private Properties users = new Properties();
    private Properties progress = new Properties();

    private String currentUser = null;

    public DatabaseManager() {
        load();
    }

    private void load() {
        try {
            File uf = new File(USERS_FILE);
            if(uf.exists()) users.load(new FileInputStream(uf));
            File pf = new File(PROGRESS_FILE);
            if(pf.exists()) progress.load(new FileInputStream(pf));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            users.store(new FileOutputStream(USERS_FILE), "User Database");
            progress.store(new FileOutputStream(PROGRESS_FILE), "Progress Database");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public boolean register(String username, String password) {
        if(users.containsKey(username)) return false; // Already exists
        users.setProperty(username, password);
        progress.setProperty(username, "");
        save();
        currentUser = username;
        return true;
    }

    public boolean login(String username, String password) {
        if(users.containsKey(username) && users.getProperty(username).equals(password)) {
            currentUser = username;
            return true;
        }
        return false;
    }

    public void logout() {
        currentUser = null;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public List<Integer> getSolvedProblems() {
        if(currentUser == null) return new ArrayList<>();
        String val = progress.getProperty(currentUser, "");
        if(val.isEmpty()) return new ArrayList<>();
        List<Integer> solved = new ArrayList<>();
        for(String s : val.split(",")) {
            try { solved.add(Integer.parseInt(s)); } catch(Exception e) {}
        }
        return solved;
    }

    public void markProblemSolved(int id) {
        if(currentUser == null) return;
        List<Integer> solved = getSolvedProblems();
        if(!solved.contains(id)) {
            solved.add(id);
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < solved.size(); i++) {
                sb.append(solved.get(i));
                if(i < solved.size() - 1) sb.append(",");
            }
            progress.setProperty(currentUser, sb.toString());
            save();
        }
    }
}
