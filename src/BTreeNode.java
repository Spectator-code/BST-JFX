import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single node in the 2-3 Tree (B-Tree of order 3).
 * A node can contain 1 or 2 keys (or 3 keys temporarily during a split).
 * It holds both keys and child BTreeNode pointers, alongside JavaFX visual objects.
 */
public final class BTreeNode {

    public List<Integer> keys = new ArrayList<>();
    public List<BTreeNode> children = new ArrayList<>();
    public BTreeNode parent;

    // Layout target coordinates
    public double targetX;
    public double targetY;

    // Current coordinates (animated)
    public DoubleProperty currentX = new SimpleDoubleProperty(2000);
    public DoubleProperty currentY = new SimpleDoubleProperty(60);

    // Visual elements
    public Group group;
    public Rectangle bgRect;
    public List<Text> keyLabels = new ArrayList<>();
    public List<Line> dividers = new ArrayList<>();
    public Group edgeToParent; // Null for root node

    public BTreeNode(int firstValue) {
        keys.add(firstValue);

        group = new Group();
        bgRect = new Rectangle();
        bgRect.setStrokeWidth(2);
        bgRect.setArcWidth(10);
        bgRect.setArcHeight(10);
        group.getChildren().add(bgRect);

        // Bind layout position centered on currentX and currentY
        group.layoutXProperty().bind(currentX.subtract(bgRect.widthProperty().divide(2)));
        group.layoutYProperty().bind(currentY.subtract(bgRect.heightProperty().divide(2)));

        redrawNode("#0ea5e9");
    }

    /** Moves the node instantly to (x, y). */
    public void placeAt(double x, double y) {
        this.targetX = x;
        this.targetY = y;
        this.currentX.set(x);
        this.currentY.set(y);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    /** Re-renders visual dividers and keys inside the glassmorphic rectangle. */
    public void redrawNode(String accentColorHex) {
        // Retain only background rectangle
        group.getChildren().retainAll(bgRect);
        keyLabels.clear();
        dividers.clear();

        int numKeys = keys.size();
        double cellWidth = 50;
        double height = 40;
        double width = numKeys * cellWidth;

        bgRect.setWidth(width);
        bgRect.setHeight(height);
        bgRect.setStroke(Color.web(accentColorHex));

        // Premium Gradient Fill (Dark Navy to Deep Indigo)
        javafx.scene.paint.LinearGradient gradient = new javafx.scene.paint.LinearGradient(
            0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.web("#080b25", 0.90)),
            new javafx.scene.paint.Stop(1, Color.web("#111a47", 0.90))
        );
        bgRect.setFill(gradient);

        // Premium DropShadow + Neon Glow Effect
        javafx.scene.effect.DropShadow dropShadow = new javafx.scene.effect.DropShadow();
        dropShadow.setRadius(12);
        dropShadow.setOffsetX(0);
        dropShadow.setOffsetY(4);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.6));

        javafx.scene.effect.DropShadow neonGlow = new javafx.scene.effect.DropShadow();
        neonGlow.setRadius(8);
        neonGlow.setColor(Color.web(accentColorHex, 0.45));
        dropShadow.setInput(neonGlow);

        bgRect.setEffect(dropShadow);

        for (int i = 0; i < numKeys; i++) {
            Text label = new Text(String.valueOf(keys.get(i)));
            label.setFill(Color.WHITE);
            label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            double cellCenterX = i * cellWidth + cellWidth / 2;
            double tWidth = 5 + (String.valueOf(keys.get(i)).length() - 1) * 4;
            label.setX(cellCenterX - tWidth);
            label.setY(height / 2 + 5);

            keyLabels.add(label);
            group.getChildren().add(label);
        }

        // Draw vertical division lines for nodes with multiple keys
        for (int i = 1; i < numKeys; i++) {
            Line div = new Line(i * cellWidth, 0, i * cellWidth, height);
            div.setStroke(Color.web(accentColorHex, 0.45));
            div.setStrokeWidth(1.5);
            dividers.add(div);
            group.getChildren().add(div);
        }
    }
}
