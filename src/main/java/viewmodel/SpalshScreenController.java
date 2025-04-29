package viewmodel;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SpalshScreenController {
    @FXML
    private Label welcomeText;

    @FXML
    private Label appTitle;

    @FXML
    private Label appSubtitle;

    @FXML
    private ImageView logoImage;

    @FXML
    private VBox contentBox;

    @FXML
    public void initialize() {
        // Apply logo effects
        logoImage.getStyleClass().add("app-logo");

        // Apply animations
        // 1. Fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), contentBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // 2. Scale animation for logo
        ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(2), logoImage);
        scaleTransition.setFromX(0.8);
        scaleTransition.setFromY(0.8);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.play();

        // 3. Slide in animation for text
        TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(1.5), appTitle);
        translateTransition.setFromY(50);
        translateTransition.setToY(0);
        translateTransition.play();

        TranslateTransition translateTransition2 = new TranslateTransition(Duration.seconds(1.5), appSubtitle);
        translateTransition2.setFromY(50);
        translateTransition2.setToY(0);
        translateTransition2.setDelay(Duration.seconds(0.3));
        translateTransition2.play();
    }
}