import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

/**
 * Represents a single node in the Binary Search Tree.
 * Holds both the data (value, left, right) and the visual
 * JavaFX elements (circle, label, edge line to parent).
 */
public class TreeNode {

    public int value;
    public TreeNode left;
    public TreeNode right;
    public TreeNode parent;

    // Visual elements
    public Circle circle;
    public Text label;
    public Line edgeToParent; // null for root

    // Target layout position (used for auto re-layout animation)
    public double targetX;
    public double targetY;

    public TreeNode(int value) {
        this.value = value;

        circle = new Circle(24);
        circle.setFill(Color.web("#2e7dd6"));
        circle.setStroke(Color.web("#0d3c66"));
        circle.setStrokeWidth(2);

        label = new Text(String.valueOf(value));
        label.setFill(Color.WHITE);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
    }

    /** Moves the circle + label instantly to (x, y) with no animation. */
    public void placeAt(double x, double y) {
        this.targetX = x;
        this.targetY = y;
        circle.setCenterX(x);
        circle.setCenterY(y);
        label.setX(x - textWidthEstimate());
        label.setY(y + 5);
    }

    private double textWidthEstimate() {
        // rough centering based on digit count
        return 5 + (String.valueOf(value).length() - 1) * 4;
    }
}
