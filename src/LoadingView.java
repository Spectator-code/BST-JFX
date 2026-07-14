import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class LoadingView {

    public Parent getView() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0b122f;");

        VBox content = new VBox(24);
        content.setAlignment(Pos.CENTER);

        // Load the image R.png without chroma keying
        WritableImage img = App.loadJavaFXImage("assets/R.png", false);
        if (img != null) {
            ImageView view = new ImageView(img);
            view.setFitWidth(320);
            view.setPreserveRatio(true);
            
            // Faint fade-in and subtle scale-up animation for the logo/image
            view.setOpacity(0.0);
            view.setScaleX(0.9);
            view.setScaleY(0.9);

            FadeTransition imgFade = new FadeTransition(Duration.millis(800), view);
            imgFade.setFromValue(0.0);
            imgFade.setToValue(1.0);

            ScaleTransition imgScale = new ScaleTransition(Duration.millis(1200), view);
            imgScale.setFromX(0.9); imgScale.setFromY(0.9);
            imgScale.setToX(1.0); imgScale.setToY(1.0);
            imgScale.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition pt = new ParallelTransition(imgFade, imgScale);
            pt.play();

            content.getChildren().add(view);
        }

        // Add a premium progress bar
        ProgressBar bar = new ProgressBar(0.0);
        bar.setPrefWidth(260);
        bar.setPrefHeight(4);
        bar.setStyle(
            "-fx-accent: " + Theme.ACCENT + ";" +
            "-fx-control-inner-background: #080d28;" +
            "-fx-background-color: #1a2240;" +
            "-fx-background-radius: 4;" +
            "-fx-padding: 0;"
        );

        Label statusLbl = new Label("Initializing learning modules...");
        statusLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        statusLbl.setTextFill(Color.web(Theme.TEXT_MUTED));

        content.getChildren().addAll(bar, statusLbl);
        root.getChildren().add(content);

        // Animate the progress bar and transition to LoginView
        Timeline progressTimeline = new Timeline();
        progressTimeline.getKeyFrames().addAll(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(bar.progressProperty(), 0.0),
                new KeyValue(statusLbl.textProperty(), "Initializing learning modules...")
            ),
            new KeyFrame(Duration.millis(800), 
                new KeyValue(bar.progressProperty(), 0.35),
                new KeyValue(statusLbl.textProperty(), "Loading BST structures...")
            ),
            new KeyFrame(Duration.millis(1600), 
                new KeyValue(bar.progressProperty(), 0.75),
                new KeyValue(statusLbl.textProperty(), "Optimizing AVL balance engines...")
            ),
            new KeyFrame(Duration.millis(2500), 
                new KeyValue(bar.progressProperty(), 1.0),
                new KeyValue(statusLbl.textProperty(), "Ready!")
            )
        );

        progressTimeline.setOnFinished(e -> {
            // Fade out the content of the loading screen before swapping scene
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                App.changeScene(new LoginView().getView());
            });
            fadeOut.play();
        });

        progressTimeline.play();

        return root;
    }
}
