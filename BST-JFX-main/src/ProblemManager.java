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

    // ── Cached list — built once ─────────────────────────────────────────
    private static List<Problem> CACHE = null;

    public static List<Problem> getAll() {
        if (CACHE != null) return CACHE;
        CACHE = new ArrayList<>();

        CACHE.add(new Problem(1, "Insert a Node", "easy",
            "Write a command to insert the value 10 into the Binary Search Tree.",
            "BinarySearchTree tree = new BinarySearchTree();\n// Insert 10 into the tree\ntree.insert(10);\n",
            Arrays.asList("Has Node 10"),
            Arrays.asList("Use tree.insert(10)")
        ));
        CACHE.add(new Problem(2, "Search in a BST", "easy",
            "Insert 20 and 30, then search for 30.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(20);\ntree.insert(30);\n// Search for 30\ntree.search(30);\n",
            Arrays.asList("Has Node 20", "Has Node 30", "Searched 30"),
            Arrays.asList("Use tree.search(30)")
        ));
        CACHE.add(new Problem(3, "Delete a Node", "medium",
            "Insert 50, 30, and 70. Then delete 30.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(50);\ntree.insert(30);\ntree.insert(70);\n// Delete 30\ntree.delete(30);\n",
            Arrays.asList("Has Node 50", "Has Node 70", "Missing Node 30"),
            Arrays.asList("Use tree.delete(30)")
        ));
        CACHE.add(new Problem(4, "Insert Multiple Nodes", "easy",
            "Insert the values 15 and 25 into an empty tree.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(15);\ntree.insert(25);\n",
            Arrays.asList("Has Node 15", "Has Node 25"),
            Arrays.asList("Use tree.insert() for each value")
        ));
        CACHE.add(new Problem(5, "Delete the Root", "medium",
            "Insert 40 and 20. Then delete 40 (the root).",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(40);\ntree.insert(20);\n// Delete root 40\ntree.delete(40);\n",
            Arrays.asList("Missing Node 40", "Has Node 20"),
            Arrays.asList("tree.delete(40) removes the root; 20 becomes new root")
        ));
        CACHE.add(new Problem(6, "Search for a Present Value", "easy",
            "Insert 5, 3, 8. Search for 8 — it should be found.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(5);\ntree.insert(3);\ntree.insert(8);\ntree.search(8);\n",
            Arrays.asList("Searched 8", "Found 8"),
            Arrays.asList("Use tree.search(8) after inserting")
        ));
        CACHE.add(new Problem(7, "Insert 100", "easy",
            "Create a tree and insert the value 100.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(100);\n",
            Arrays.asList("Has Node 100"),
            Arrays.asList("Just tree.insert(100)")
        ));
        CACHE.add(new Problem(8, "Build a Balanced Subtree", "medium",
            "Insert 5, 3, and 7 to form a small balanced BST.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(5);\ntree.insert(3);\ntree.insert(7);\n",
            Arrays.asList("Has Node 5", "Has Node 3", "Has Node 7"),
            Arrays.asList("5 is root; 3 goes left, 7 goes right")
        ));
        CACHE.add(new Problem(9, "Insert Using an Array", "medium",
            "Declare an int array {10,20,30} and insert all into the tree using a for-each loop.",
            "BinarySearchTree tree = new BinarySearchTree();\nint[] nums = {10, 20, 30};\nfor (int x : nums) { tree.insert(x); }\n",
            Arrays.asList("Inserted multiple"),
            Arrays.asList("int[] nums = {10,20,30}; for (int x : nums) { tree.insert(x); }")
        ));
        CACHE.add(new Problem(10, "Insert 55 into Existing Tree", "easy",
            "Build a tree with 50 and 60, then also insert 55.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(50);\ntree.insert(60);\ntree.insert(55);\n",
            Arrays.asList("Has Node 55"),
            Arrays.asList("55 > 50, goes right of 50; 55 < 60, goes left of 60")
        ));
        CACHE.add(new Problem(11, "Delete a Leaf Node", "easy",
            "Insert 50 and 60. Delete 60 (a leaf node).",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(50);\ntree.insert(60);\ntree.delete(60);\n",
            Arrays.asList("Missing Node 60"),
            Arrays.asList("60 is a leaf — just remove it with tree.delete(60)")
        ));
        CACHE.add(new Problem(12, "Insert 1, 2, 3 in Order", "easy",
            "Insert 1, 2, 3 into the tree in this exact order.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(1);\ntree.insert(2);\ntree.insert(3);\n",
            Arrays.asList("Has Node 1", "Has Node 2", "Has Node 3"),
            Arrays.asList("Inserting sorted values creates a right-skewed tree")
        ));
        CACHE.add(new Problem(13, "Run a Full Program", "hard",
            "Build a tree of your choice with at least 5 nodes, search for 2 values, and delete 1 node. Click Run All to execute.",
            "BinarySearchTree tree = new BinarySearchTree();\nint[] vals = {40, 20, 60, 10, 30};\nfor (int x : vals) { tree.insert(x); }\ntree.search(30);\ntree.search(60);\ntree.delete(10);\n",
            Arrays.asList("Build successful"),
            Arrays.asList("Run your code first with ▶ Run All, then submit")
        ));
        CACHE.add(new Problem(14, "Insert the Maximum Value", "hard",
            "Insert the value 99 (the highest allowed) into the tree.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(99);\n",
            Arrays.asList("Has Node 99"),
            Arrays.asList("99 is always the rightmost node in any BST")
        ));

        return CACHE;
    }

    public static Problem get(int id) {
        for (Problem p : getAll()) {
            if (p.id == id) return p;
        }
        return null;
    }
}
