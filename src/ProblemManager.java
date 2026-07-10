import java.util.*;

public class ProblemManager {
    public static class Problem {
        public int id;
        public String title;
        public String difficulty;
        public String description;
        public String startingCode;
        public List<String> tests;
        public List<String> hints;

        public Problem(int id, String title, String diff, String desc, String code, List<String> tests, List<String> hints) {
            this.id = id; this.title = title; this.difficulty = diff; this.description = desc;
            this.startingCode = code; this.tests = tests; this.hints = hints;
        }
    }

    public static List<Problem> getAll() {
        List<Problem> list = new ArrayList<>();
        list.add(new Problem(1, "Insert a Node", "easy",
            "Write a command to insert the value `10` into the Binary Search Tree.",
            "// Insert 10 into the tree\ntree.insert(10);\n",
            Arrays.asList("Has Node 10"), Arrays.asList("Use tree.insert(val)")
        ));
        list.add(new Problem(2, "Search in a BST", "easy",
            "Insert 20 and 30, then write a command to search for `30`.",
            "tree.insert(20);\ntree.insert(30);\n// Search for 30\n",
            Arrays.asList("Has Node 20", "Has Node 30", "Searched 30"), Arrays.asList("Use tree.search(val)")
        ));
        list.add(new Problem(3, "Delete a Node", "medium",
            "Insert 50, 30, and 70. Then delete `30`.",
            "tree.insert(50);\ntree.insert(30);\ntree.insert(70);\n// Delete 30\n",
            Arrays.asList("Has Node 50", "Has Node 70", "Missing Node 30"), Arrays.asList("Use tree.delete(val)")
        ));
        // Add remaining mocked for brevity. The user can expand.
        for(int i = 4; i <= 14; i++) {
            list.add(new Problem(i, "Problem " + i, "hard", "Complete problem " + i, "// Write code here", Arrays.asList("Test passing"), Arrays.asList("Hint 1")));
        }
        return list;
    }

    public static Problem get(int id) {
        for(Problem p : getAll()) {
            if(p.id == id) return p;
        }
        return null;
    }
}
