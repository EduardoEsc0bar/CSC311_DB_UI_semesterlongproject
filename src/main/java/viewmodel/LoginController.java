package viewmodel;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.MyLogger;
import service.UserSession;

import java.util.prefs.Preferences;

public class LoginController {
    @FXML
    private GridPane rootpane;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    public void initialize() {
        // Set background image
        rootpane.setBackground(new Background(
                createImage("https://edencoding.com/wp-content/uploads/2021/03/layer_06_1920x1080.png"),
                null,
                null,
                null,
                null,
                null
        ));

        // Apply fade-in animation
        rootpane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), rootpane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Try to load saved credentials
        loadSavedCredentials();
    }

    private void loadSavedCredentials() {
        try {
            Preferences prefs = Preferences.userRoot();
            String savedUsername = prefs.get("USERNAME", "");

            if (!savedUsername.isEmpty()) {
                usernameField.setText(savedUsername);
                MyLogger.makeLog("Loaded saved credentials for: " + savedUsername);
            }
        } catch (Exception e) {
            MyLogger.makeLog("Error loading saved credentials: " + e.getMessage());
        }
    }

    private static BackgroundImage createImage(String url) {
        return new BackgroundImage(
                new Image(url),
                BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT,
                new BackgroundPosition(Side.LEFT, 0, true, Side.BOTTOM, 0, true),
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, false, true));
    }

    @FXML
    public void login(ActionEvent actionEvent) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Username and password are required!");
            return;
        }

        // Verify credentials against saved preferences
        boolean isAuthenticated = verifyCredentials(username, password);

        if (isAuthenticated) {
            // Set the current user session
            UserSession.getInstance(username, password);

            // Navigate to main screen
            navigateToMainScreen(actionEvent);
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Invalid username or password!");
        }
    }

    private boolean verifyCredentials(String username, String password) {
        try {
            Preferences prefs = Preferences.userRoot();

            // Get all saved usernames to check
            String allUsernames = prefs.get("ALL_USERNAMES", "");

            if (allUsernames.isEmpty()) {
                // No users registered yet, allow login if this is the first user
                if (username.equals("admin") && password.equals("admin")) {
                    // Create admin account if it doesn't exist
                    saveCredentials("admin", "admin", "ADMIN");
                    return true;
                }
                return false;
            }

            // Check if this username exists
            String[] usernames = allUsernames.split(",");
            boolean usernameExists = false;

            for (String name : usernames) {
                if (name.equals(username)) {
                    usernameExists = true;
                    break;
                }
            }

            if (!usernameExists) {
                MyLogger.makeLog("Username not found: " + username);
                return false;
            }

            // Get the saved password for this username
            String savedPassword = prefs.get("PASSWORD_" + username, "");

            // Compare passwords
            boolean passwordMatches = savedPassword.equals(password);
            if (passwordMatches) {
                MyLogger.makeLog("Login successful for user: " + username);
                return true;
            } else {
                MyLogger.makeLog("Password mismatch for user: " + username);
                return false;
            }
        } catch (Exception e) {
            MyLogger.makeLog("Error verifying credentials: " + e.getMessage());
            return false;
        }
    }

    private void saveCredentials(String username, String password, String privileges) {
        try {
            Preferences prefs = Preferences.userRoot();

            // Save current username and password
            prefs.put("USERNAME", username);
            prefs.put("PASSWORD", password);
            prefs.put("PRIVILEGES", privileges);

            // Update the list of all usernames
            String allUsernames = prefs.get("ALL_USERNAMES", "");
            if (allUsernames.isEmpty()) {
                allUsernames = username;
            } else if (!allUsernames.contains(username)) {
                allUsernames += "," + username;
            }
            prefs.put("ALL_USERNAMES", allUsernames);

            // Save username-specific credentials
            prefs.put("PASSWORD_" + username, password);
            prefs.put("PRIVILEGES_" + username, privileges);

            MyLogger.makeLog("Saved credentials for user: " + username);
        } catch (Exception e) {
            MyLogger.makeLog("Error saving credentials: " + e.getMessage());
        }
    }

    private void navigateToMainScreen(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/db_interface_gui.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/academicTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            MyLogger.makeLog("Error navigating to main screen: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to load main interface: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void signUp(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/signUp.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/academicTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            MyLogger.makeLog("Error navigating to signup page: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to load signup page: " + e.getMessage());
        }
    }
}