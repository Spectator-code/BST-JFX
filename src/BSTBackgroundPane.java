import javafx.animation.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A beautiful, highly aesthetic animated background pane for the Send Node platform.
 * It renders faint, glowing Binary Search Tree structures with drifting nodes,
 * pulsing links, and glowing traversal particles flowing down the edges.
 * Uses low opacities to serve as a non-distracting premium background.
 */
public class BSTBackgroundPane extends Pane {

    private final List<DriftingTree> trees = new ArrayList<>();
    private final Random rng = new Random();
    private AnimationTimer driftTimer;

    public BSTBackgroundPane() {
        // Deep background matching Theme.BG
        setStyle("-fx-background-color: " + Theme.BG + ";");
        setMouseTransparent(true); // Ensure it doesn't intercept clicks/inputs

        // Listen for size changes to regenerate/adjust trees to fill the viewport
        widthProperty().addListener((obs, oldVal, newVal) -> handleResize());
        heightProperty().addListener((obs, oldVal, newVal) -> handleResize());
    }

    private void handleResize() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Clear existing trees
        getChildren().clear();
        trees.clear();
        if (driftTimer != null) driftTimer.stop();

        // Spawn 3-4 background trees at different positions
        int numTrees = w > 1200 ? 4 : 3;
        for (int i = 0; i < numTrees; i++) {
            double startX = (w / numTrees) * i + (w / numTrees) * 0.3;
            double startY = rng.nextDouble() * (h * 0.5) + (h * 0.15);
            DriftingTree tree = new DriftingTree(startX, startY);
            trees.add(tree);
            // Add all visual elements of the tree to the pane
            getChildren().addAll(tree.getNodesGroup());
        }

        // Start animation timer for general drifting / rotation effects
        driftTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                for (DriftingTree tree : trees) {
                    tree.update(w, h);
                }
            }
        };
        driftTimer.start();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DRIFTING TREE STRUCTURAL LAYER
    // ═══════════════════════════════════════════════════════════════════════

    private class DriftingTree {
        private final List<BNode> nodes = new ArrayList<>();
        private final List<BEdge> edges = new ArrayList<>();
        private final Pane group = new Pane();
        
        // Drifting speeds
        private double dx;
        private double dy;
        private double angle = 0;
        private final double rotationSpeed;

        public DriftingTree(double centerX, double centerY) {
            group.setMouseTransparent(true);
            group.setPickOnBounds(false);

            dx = (rng.nextDouble() - 0.5) * 0.18;
            dy = (rng.nextDouble() - 0.5) * 0.18;
            rotationSpeed = (rng.nextDouble() - 0.5) * 0.05;

            // Generate a random 3-level tree structure (root + children + grandchildren)
            // Node values: typical BST structure
            BNode root = new BNode(50, centerX, centerY, 15);
            BNode left = new BNode(25, centerX - 80, centerY + 65, 12);
            BNode right = new BNode(75, centerX + 80, centerY + 65, 12);
            
            BNode leftLeft = new BNode(12, centerX - 120, centerY + 120, 10);
            BNode leftRight = new BNode(37, centerX - 40, centerY + 120, 10);
            BNode rightLeft = new BNode(62, centerX + 40, centerY + 120, 10);
            BNode rightRight = new BNode(88, centerX + 120, centerY + 120, 10);

            nodes.add(root); nodes.add(left); nodes.add(right);
            nodes.add(leftLeft); nodes.add(leftRight);
            nodes.add(rightLeft); nodes.add(rightRight);

            // Connect edges
            edges.add(new BEdge(root, left));
            edges.add(new BEdge(root, right));
            edges.add(new BEdge(left, leftLeft));
            edges.add(new BEdge(left, leftRight));
            edges.add(new BEdge(right, rightLeft));
            edges.add(new BEdge(right, rightRight));

            // Render edges first so they sit behind circles
            for (BEdge edge : edges) {
                group.getChildren().add(edge.line);
            }
            // Render nodes
            for (BNode node : nodes) {
                group.getChildren().addAll(node.circle, node.text);
            }

            // Set overall group transparency to stay faint
            group.setOpacity(0.12);

            // Start traversal particle loop
            startTraversalParticles();

            // Start simulated node insertion animation loop
            startInsertionSimulation();
        }

        public Pane getNodesGroup() {
            return group;
        }

        public void update(double boundW, double boundH) {
            // Drift coordinate shifts
            double currentX = group.getTranslateX();
            double currentY = group.getTranslateY();

            // Boundary wrap around
            if (Math.abs(currentX) > boundW * 0.5) dx = -dx;
            if (Math.abs(currentY) > boundH * 0.5) dy = -dy;

            group.setTranslateX(currentX + dx);
            group.setTranslateY(currentY + dy);

            // Subtle rotation
            angle += rotationSpeed;
            group.setRotate(angle);
        }

        /**
         * Simulates searches/traversals by spawning glowing light particles
         * that traverse from parent to child node along the tree edges.
         */
        private void startTraversalParticles() {
            // Create a looping timeline that triggers search traversals every few seconds
            Timeline particleTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> spawnPathTraversal()),
                new KeyFrame(Duration.seconds(3.5 + rng.nextDouble() * 2.0))
            );
            particleTimeline.setCycleCount(Animation.INDEFINITE);
            particleTimeline.play();
        }

        private void spawnPathTraversal() {
            if (nodes.isEmpty()) return;

            // Choose a target leaf to search for: 12, 37, 62, or 88
            int[] leaves = {12, 37, 62, 88};
            int targetVal = leaves[rng.nextInt(leaves.length)];

            List<BNode> path = new ArrayList<>();
            BNode curr = nodes.get(0); // Root is at index 0
            path.add(curr);

            // Traverse to target leaf
            while (curr != null) {
                BNode next = null;
                if (targetVal < curr.value) {
                    // search left children
                    for (BNode n : nodes) {
                        if (n.value < curr.value && Math.abs(n.x - (curr.x - 80)) < 40 && Math.abs(n.y - (curr.y + 65)) < 15) {
                            next = n; break;
                        }
                    }
                } else if (targetVal > curr.value) {
                    // search right children
                    for (BNode n : nodes) {
                        if (n.value > curr.value && Math.abs(n.x - (curr.x + 80)) < 40 && Math.abs(n.y - (curr.y + 65)) < 15) {
                            next = n; break;
                        }
                    }
                }
                if (next != null) {
                    path.add(next);
                }
                curr = next;
            }

            // Animate a glowing particle traversing down this path sequentially
            animateParticleOnPath(path, 0);
        }

        private void animateParticleOnPath(List<BNode> path, int index) {
            if (index >= path.size() - 1) return;

            BNode start = path.get(index);
            BNode end = path.get(index + 1);

            // Create particle
            Circle p = new Circle(start.x, start.y, 3.5, Color.web(Theme.TEAL, 0.75));
            group.getChildren().add(p);

            // Pulsing outline
            p.setStroke(Color.web(Theme.TEXT_LIGHT, 0.8));
            p.setStrokeWidth(1.0);

            // Pulse the starting node of the hop
            pulseNode(start);

            // Animation moving along the branch
            Timeline t = new Timeline();
            t.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, 
                    new KeyValue(p.centerXProperty(), start.x),
                    new KeyValue(p.centerYProperty(), start.y),
                    new KeyValue(p.opacityProperty(), 0.75)
                ),
                new KeyFrame(Duration.millis(800),
                    new KeyValue(p.centerXProperty(), end.x),
                    new KeyValue(p.centerYProperty(), end.y),
                    new KeyValue(p.opacityProperty(), 0.2)
                )
            );
            t.setOnFinished(e -> {
                group.getChildren().remove(p);
                // Pulse the destination node on arrival if it's the last hop
                if (index == path.size() - 2) {
                    pulseNode(end);
                }
                // Trigger next hop in path
                animateParticleOnPath(path, index + 1);
            });
            t.play();
        }

        /** Pulse scale and color of a background node to simulate visitor interaction. */
        private void pulseNode(BNode node) {
            ScaleTransition scale = new ScaleTransition(Duration.millis(250), node.circle);
            scale.setFromX(1.0); scale.setFromY(1.0);
            scale.setToX(1.35); scale.setToY(1.35);
            scale.setAutoReverse(true);
            scale.setCycleCount(2);

            Color originalStroke = (Color) node.circle.getStroke();
            Color originalFill = (Color) node.circle.getFill();

            node.circle.setStroke(Color.web(Theme.TEAL, 0.8));
            node.circle.setFill(Color.web(Theme.TEAL, 0.35));

            scale.setOnFinished(e -> {
                node.circle.setStroke(originalStroke);
                node.circle.setFill(originalFill);
            });
            scale.play();
        }

        /** Timeline that triggers a simulated BST insertion every few seconds. */
        private void startInsertionSimulation() {
            Timeline insertionTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> spawnInsertion()),
                new KeyFrame(Duration.seconds(12.0 + rng.nextDouble() * 5.0))
            );
            insertionTimeline.setCycleCount(Animation.INDEFINITE);
            insertionTimeline.play();
        }

        /** Spawns a floating node and traces its comparison path down the tree layout. */
        private void spawnInsertion() {
            if (nodes.isEmpty()) return;

            // Pick a value to insert
            int val = rng.nextInt(90) + 5; // value between 5 and 95
            
            // Ensure no duplicate value in background nodes
            boolean exists = true;
            while (exists) {
                exists = false;
                for (BNode n : nodes) {
                    if (n.value == val) {
                        val = rng.nextInt(90) + 5;
                        exists = true;
                        break;
                    }
                }
            }

            // Trace path of comparison nodes based on BST insertion logic
            List<BNode> path = new ArrayList<>();
            BNode curr = nodes.get(0); // root
            
            double nextX = curr.x;
            double nextY = curr.y;
            
            double offsetX = 80;
            double offsetY = 65;

            while (curr != null) {
                path.add(curr);
                BNode next = null;
                if (val < curr.value) {
                    double targetX = curr.x - offsetX;
                    double targetY = curr.y + offsetY;
                    for (BNode n : nodes) {
                        if (Math.abs(n.x - targetX) < 15 && Math.abs(n.y - targetY) < 15) {
                            next = n;
                            break;
                        }
                    }
                    if (next == null) {
                        nextX = targetX;
                        nextY = targetY;
                    }
                } else {
                    double targetX = curr.x + offsetX;
                    double targetY = curr.y + offsetY;
                    for (BNode n : nodes) {
                        if (Math.abs(n.x - targetX) < 15 && Math.abs(n.y - targetY) < 15) {
                            next = n;
                            break;
                        }
                    }
                    if (next == null) {
                        nextX = targetX;
                        nextY = targetY;
                    }
                }
                curr = next;
                offsetX = Math.max(20, offsetX * 0.5);
                offsetY = 55;
            }

            animateInsertion(path, val, nextX, nextY);
        }

        /** Animates a node traveling down the comparison path and flashing green on insertion. */
        private void animateInsertion(List<BNode> path, int insertVal, double targetX, double targetY) {
            BNode rootNode = path.get(0);
            double startX = rootNode.x;
            double startY = rootNode.y - 50;

            Circle c = new Circle(startX, startY, 8);
            c.setFill(Color.web(Theme.TEAL, 0.15));
            c.setStroke(Color.web(Theme.TEAL, 0.5));
            c.setStrokeWidth(1.2);
            c.setOpacity(0.0);

            Text t = new Text(String.valueOf(insertVal));
            t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 7));
            t.setFill(Color.web(Theme.TEXT_LIGHT, 0.7));
            t.setX(startX - 3.5);
            t.setY(startY + 2.5);
            t.setOpacity(0.0);

            group.getChildren().addAll(c, t);

            Timeline timeline = new Timeline();
            
            // Fade in above root
            timeline.getKeyFrames().add(new KeyFrame(Duration.ZERO, 
                new KeyValue(c.opacityProperty(), 0.0),
                new KeyValue(t.opacityProperty(), 0.0)
            ));
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(400),
                new KeyValue(c.opacityProperty(), 0.85),
                new KeyValue(t.opacityProperty(), 0.85)
            ));

            double currentDelayMs = 400;

            // Travel and compare down the branches
            for (int i = 0; i < path.size(); i++) {
                BNode node = path.get(i);
                
                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(currentDelayMs + 600),
                    new KeyValue(c.centerXProperty(), node.x),
                    new KeyValue(c.centerYProperty(), node.y),
                    new KeyValue(t.xProperty(), node.x - (String.valueOf(insertVal).length() > 1 ? 5.5 : 3.0)),
                    new KeyValue(t.yProperty(), node.y + 2.5)
                ));
                currentDelayMs += 600;

                final BNode nodeToPulse = node;
                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(currentDelayMs), e -> {
                    pulseNode(nodeToPulse);
                }));
                currentDelayMs += 400;
            }

            // Move to final empty slot
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(currentDelayMs + 600),
                new KeyValue(c.centerXProperty(), targetX),
                new KeyValue(c.centerYProperty(), targetY),
                new KeyValue(t.xProperty(), targetX - (String.valueOf(insertVal).length() > 1 ? 5.5 : 3.0)),
                new KeyValue(t.yProperty(), targetY + 2.5)
            ));
            currentDelayMs += 600;

            // Flash green at target leaf
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(currentDelayMs), e -> {
                ScaleTransition scale = new ScaleTransition(Duration.millis(250), c);
                scale.setFromX(1.0); scale.setFromY(1.0);
                scale.setToX(1.3); scale.setToY(1.3);
                scale.setAutoReverse(true);
                scale.setCycleCount(2);

                c.setStroke(Color.web(Theme.SUCCESS, 0.8));
                c.setFill(Color.web(Theme.SUCCESS, 0.25));
                scale.play();
            }));
            currentDelayMs += 1000;

            // Fade out
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(currentDelayMs + 800),
                new KeyValue(c.opacityProperty(), 0.0),
                new KeyValue(t.opacityProperty(), 0.0)
            ));
            
            timeline.setOnFinished(e -> {
                group.getChildren().removeAll(c, t);
            });

            timeline.play();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NODE / EDGE REPRESENTATION CLASSES
    // ═══════════════════════════════════════════════════════════════════════

    private class BNode {
        int value;
        double x, y;
        Circle circle;
        Text text;

        public BNode(int value, double x, double y, double radius) {
            this.value = value;
            this.x = x;
            this.y = y;

            circle = new Circle(x, y, radius);
            circle.setFill(Color.web(Theme.PRIMARY, 0.15));
            circle.setStroke(Color.web(Theme.ACCENT, 0.4));
            circle.setStrokeWidth(1.5);

            text = new Text(String.valueOf(value));
            text.setFont(Font.font("Segoe UI", FontWeight.BOLD, radius * 0.8));
            text.setFill(Color.web(Theme.TEXT_LIGHT, 0.6));
            text.setX(x - (String.valueOf(value).length() > 1 ? radius * 0.6 : radius * 0.3));
            text.setY(y + (radius * 0.3));
        }
    }

    private class BEdge {
        Line line;

        public BEdge(BNode from, BNode to) {
            line = new Line(from.x, from.y, to.x, to.y);
            line.setStroke(Color.web(Theme.BORDER, 0.35));
            line.setStrokeWidth(1.8);
        }
    }
}
