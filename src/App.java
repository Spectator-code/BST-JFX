import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

/**
 * Main application entry point for the Send Node BST Visual Learning Platform.
 * Sets up primary stage configurations, centralizes scene navigation animations,
 * and hosts safe asset-loading helpers.
 */
public class App extends Application {
    private static Stage primaryStage;
    public static DatabaseManager db = new DatabaseManager();

    /** Exposes the primary stage so views can attach window-level listeners. */
    public static Stage getPrimaryStage() { return primaryStage; }


    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("Send Node - BST Learning Platform");
        
        // Start by directing the user to the Loading splash screen.
        changeScene(new LoadingView().getView());
        
        // Define minimum and initial dimensions for the responsive window layout
        stage.setMinWidth(900);
        stage.setMinHeight(580);
        stage.setWidth(1000);
        stage.setHeight(700);
        stage.show();
    }

    /**
     * Navigates to a new view using a custom visual cross-fade animation.
     * 
     * @param root The parent root layout of the incoming scene view.
     */
    public static void changeScene(javafx.scene.Parent root) {
        Scene currentScene = primaryStage.getScene();
        if (currentScene == null) {
            // First time load: perform simple fade-in.
            primaryStage.setScene(new Scene(root));
            root.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        } else {
            // Smooth cross-fade transition: fade out current root, swap scene, then fade in new root.
            javafx.scene.Parent currentRoot = currentScene.getRoot();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), currentRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                primaryStage.setScene(new Scene(root));
                root.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }

    /**
     * Custom image loader utility designed to bypass JavaFX native platform restrictions.
     * 
     * Motivation: Some lightweight or minimal JavaFX SDK distributions do not ship with the
     * required native decoding library ('javafx_iio.dll' on Windows) necessary to decode standard
     * image formats (PNG/JPEG), resulting in UnsatisfiedLinkErrors.
     * 
     * Solution: We read the file using standard Java JDK AWT ImageIO decoders (which rely on the
     * stable JVM standard libraries), and reconstruct it pixel-by-pixel into a JavaFX WritableImage.
     * 
     * Enhancement: We also perform a real-time chroma-key filter inside this loader to discard
     * solid black/charcoal backgrounds (R, G, B < 28) and replace them with alpha transparency,
     * allowing the graphics to sit cleanly over gradient backgrounds.
     * 
     * @param path The relative path to the image asset.
     * @return WritableImage containing the transparent logo, or null if loading fails.
     */
    public static javafx.scene.image.WritableImage loadJavaFXImage(String path) {
        return loadJavaFXImage(path, true);
    }

    public static javafx.scene.image.WritableImage loadJavaFXImage(String path, boolean chromaKey) {
        try {
            java.io.File file = new java.io.File(path);
            if (!file.exists()) return null;
            
            // Read using standard JVM platform decoder
            java.awt.image.BufferedImage bimg = javax.imageio.ImageIO.read(file);
            if (bimg == null) return null;
            
            int w = bimg.getWidth();
            int h = bimg.getHeight();
            javafx.scene.image.WritableImage wimg = new javafx.scene.image.WritableImage(w, h);
            javafx.scene.image.PixelWriter pw = wimg.getPixelWriter();
            
            // Map pixels while filtering out black background blocks (chroma-key transparent filter)
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = bimg.getRGB(x, y);
                    if (chromaKey) {
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;
                        
                        // Chroma-key: if the pixel is close to black, make it completely transparent (alpha = 0)
                        if (r < 28 && g < 28 && b < 28) {
                            pw.setArgb(x, y, 0x00000000);
                        } else {
                            pw.setArgb(x, y, argb);
                        }
                    } else {
                        pw.setArgb(x, y, argb);
                    }
                }
            }
            return wimg;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
