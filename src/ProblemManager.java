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
        CACHE.add(new Problem(15, "AVL Single Right Rotation", "medium",
            "Initialize an AVL tree and insert 30, 20, 10 in that order. This should trigger a Left-Left Case. Verify the root of the tree becomes 20.",
            "AVLTree tree = new AVLTree();\ntree.insert(30);\ntree.insert(20);\ntree.insert(10);\n",
            Arrays.asList("Balanced AVL"),
            Arrays.asList("Use AVLTree instead of BinarySearchTree. 20 becomes the new root.")
        ));
        CACHE.add(new Problem(16, "AVL Left-Right Rotation", "hard",
            "Initialize an AVL tree and insert 30, 10, 20. This triggers a Left-Right Case (rotating left at 10, then right at 30). Verify the root becomes 20.",
            "AVLTree tree = new AVLTree();\ntree.insert(30);\ntree.insert(10);\ntree.insert(20);\n",
            Arrays.asList("Balanced AVL"),
            Arrays.asList("A double rotation occurs where 10 is rotated left and 30 is rotated right.")
        ));
        CACHE.add(new Problem(17, "Build balanced AVL Tree", "hard",
            "Insert elements 50, 25, 75, 12, 37, 60, 85 into an AVL tree. Verify the root remains 50 and the height is balanced.",
            "AVLTree tree = new AVLTree();\nint[] vals = {50, 25, 75, 12, 37, 60, 85};\nfor (int x : vals) { tree.insert(x); }\n",
            Arrays.asList("Balanced AVL"),
            Arrays.asList("Inserting these elements keeps the root at 50 with balanced height <= 3.")
        ));
        CACHE.add(new Problem(18, "In-Order Traversal", "easy",
            "Insert 40, 20, 60, 10, 30 into a BST. Then perform an in-order traversal using the sidebar — verify the result is 10, 20, 30, 40, 60 (sorted ascending).",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(40);\ntree.insert(20);\ntree.insert(60);\ntree.insert(10);\ntree.insert(30);\n// Click 'In-Order' in the Traversals section\n",
            Arrays.asList("In-Order traversal"),
            Arrays.asList("In-order visits Left → Root → Right, producing sorted output.")
        ));
        CACHE.add(new Problem(19, "Pre-Order Traversal", "medium",
            "Build a tree with values 50, 30, 70, 20, 40, 60, 80. Perform pre-order traversal and verify the root (50) appears first in the output.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(50);\ntree.insert(30);\ntree.insert(70);\ntree.insert(20);\ntree.insert(40);\ntree.insert(60);\ntree.insert(80);\n// Click 'Pre-Order' in the Traversals section\n",
            Arrays.asList("Pre-Order traversal"),
            Arrays.asList("Pre-order visits Root → Left → Right. The root value is always first.")
        ));
        CACHE.add(new Problem(20, "Post-Order Traversal", "hard",
            "Build a tree with 45, 15, 75, 10, 25. Perform post-order traversal — the root (45) should appear LAST in the result sequence.",
            "BinarySearchTree tree = new BinarySearchTree();\ntree.insert(45);\ntree.insert(15);\ntree.insert(75);\ntree.insert(10);\ntree.insert(25);\n// Click 'Post-Order' in the Traversals section\n",
            Arrays.asList("Post-Order traversal"),
            Arrays.asList("Post-order visits Left → Right → Root. The root always appears last.")
        ));
        CACHE.add(new Problem(21, "2-3 Tree Insertion", "easy",
            "Initialize a 2-3 Tree and insert the values 40, 20, 60. This builds a simple, balanced 2-3 Tree.",
            "BTree tree = new BTree();\ntree.insert(40);\ntree.insert(20);\ntree.insert(60);\n",
            Arrays.asList("Has Node 40", "Has Node 20", "Has Node 60"),
            Arrays.asList("Use BTree tree = new BTree(); and tree.insert(val) for each value.")
        ));
        CACHE.add(new Problem(22, "2-3 Tree Search", "medium",
            "Insert 35 and 75 into a 2-3 Tree, then search for 75.",
            "BTree tree = new BTree();\ntree.insert(35);\ntree.insert(75);\ntree.search(75);\n",
            Arrays.asList("Has Node 35", "Has Node 75", "Searched 75"),
            Arrays.asList("Use tree.search(75) after inserting the values.")
        ));
        CACHE.add(new Problem(23, "2-3 Tree Split Propagation", "hard",
            "Insert 10, 20, and 30 into a 2-3 Tree. Adding 30 triggers a node overflow and split, pushing 20 up to become the new root. Verify the root becomes 20.",
            "BTree tree = new BTree();\ntree.insert(10);\ntree.insert(20);\ntree.insert(30);\n",
            Arrays.asList("Balanced BTree Split"),
            Arrays.asList("When 30 is inserted, the node containing [10, 20, 30] splits, pushing 20 up.")
        ));

        return CACHE;
    }

    /** Returns count of problems per difficulty string (case-insensitive). */
    public static java.util.Map<String, Integer> countByDifficulty() {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        counts.put("easy", 0);
        counts.put("medium", 0);
        counts.put("hard", 0);
        for (Problem p : getAll()) {
            String key = p.difficulty.toLowerCase();
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return counts;
    }

    public static Problem get(int id) {
        for (Problem p : getAll()) {
            if (p.id == id) return p;
        }
        return null;
    }
}
