package viewmodel;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import service.MyLogger;
import service.UserSession;

import java.util.prefs.Preferences;

public class SignUpController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<String> privilegesComboBox;

    @FXML
    private Label statusLabel;

    public void initialize() {
        // Initialize privileges dropdown
        privilegesComboBox.getItems().addAll("USER", "ADMIN");
        privilegesComboBox.getSelectionModel().selectFirst();

        // Add listeners for form validation
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());

        // Initial validation
        validateForm();
    }

    private void validateForm() {
        boolean isValid = true;
        StringBuilder message = new StringBuilder();

        // Validate username (at least 4 characters, alphanumeric)
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            isValid = false;
            message.append("Username is required\n");
        } else if (username.length() < 4) {
            isValid = false;
            message.append("Username must be at least 4 characters\n");
        } else if (!username.matches("^[a-zA-Z0-9]+$")) {
            isValid = false;
            message.append("Username must be alphanumeric\n");
        } else if (usernameExists(username)) {
            isValid = false;
            message.append("Username already exists\n");
        }

        // Validate password (at least 6 characters, one number, one uppercase)
        String password = passwordField.getText();
        if (password.isEmpty()) {
            isValid = false;
            message.append("Password is required\n");
        } else if (password.length() < 6) {
            isValid = false;
            message.append("Password must be at least 6 characters\n");
        } else if (!password.matches(".*[0-9].*")) {
            isValid = false;
            message.append("Password must contain at least one number\n");
        } else if (!password.matches(".*[A-Z].*")) {
            isValid = false;
            message.append("Password must contain at least one uppercase letter\n");
        }

        // Validate password confirmation
        if (!password.equals(confirmPasswordField.getText())) {
            isValid = false;
            message.append("Passwords do not match");
        }

        // Update status label
        if (!isValid) {
            statusLabel.setText(message.toString());
            statusLabel.setStyle("-fx-text-fill: red;");
        } else {
            statusLabel.setText("All fields are valid");
            statusLabel.setStyle("-fx-text-fill: green;");
        }
    }

    private boolean usernameExists(String username) {
        try {
            Preferences prefs = Preferences.userRoot();
            String allUsernames = prefs.get("ALL_USERNAMES", "");

            if (allUsernames.isEmpty()) {
                return false;
            }

            String[] usernames = allUsernames.split(",");
            for (String name : usernames) {
                if (name.equals(username)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            MyLogger.makeLog("Error checking username existence: " + e.getMessage());
            return false;
        }
    }

    @FXML
    public void createNewAccount(ActionEvent actionEvent) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String privilege = privilegesComboBox.getValue();

        // Validate inputs
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "All fields are required!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Passwords do not match!");
            return;
        }

        if (password.length() < 6 || !password.matches(".*[0-9].*") || !password.matches(".*[A-Z].*")) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Password must be at least 6 characters and contain at least one number and one uppercase letter!");
            return;
        }

        if (usernameExists(username)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Username already exists!");
            return;
        }

        // Save user in Preferences
        saveUser(username, password, privilege);

        // Create user session
        UserSession.getInstance(username, password, privilege);
        MyLogger.makeLog("New account created: " + username + " with privilege: " + privilege);

        showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully!");

        // Navigate to login
        goBack(actionEvent);
    }

    private void saveUser(String username, String password, String privileges) {
        try {
            Preferences prefs = Preferences.userRoot();

            // Update the list of all usernames
            String allUsernames = prefs.get("ALL_USERNAMES", "");
            if (allUsernames.isEmpty()) {
                allUsernames = username;
            } else {
                allUsernames += "," + username;
            }
            prefs.put("ALL_USERNAMES", allUsernames);

            // Save username-specific credentials
            prefs.put("PASSWORD_" + username, password);
            prefs.put("PRIVILEGES_" + username, privileges);

            // Also update the current session info
            prefs.put("USERNAME", username);
            prefs.put("PASSWORD", password);
            prefs.put("PRIVILEGES", privileges);

            MyLogger.makeLog("Saved user: " + username);
        } catch (Exception e) {
            MyLogger.makeLog("Error saving user: " + e.getMessage());
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
    public void goBack(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/academicTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to return to login screen: " + e.getMessage());
        }
    }
}