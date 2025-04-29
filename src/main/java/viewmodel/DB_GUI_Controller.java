package viewmodel;

import dao.DbConnectivityClass;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Person;
import service.MyLogger;
import service.PDFReportGenerator;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Timer;

public class DB_GUI_Controller implements Initializable {

    @FXML
    TextField first_name, last_name, department, email, imageURL;
    @FXML
    ComboBox<MajorOptions> majorComboBox;
    @FXML
    ImageView img_view;
    @FXML
    MenuBar menuBar;
    @FXML
    private TableView<Person> tv;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;
    @FXML
    private Button editButton, deleteButton, addButton;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField searchField;
    @FXML
    private MenuItem editItem;

    @FXML
    private MenuItem deleteItem;

    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();

    // Major options enum
    public enum MajorOptions {
        COMPUTER_SCIENCE("Computer Science"),
        CPIS("Computer Information Systems"),
        ENGLISH("English"),
        BUSINESS("Business"),
        MATHEMATICS("Mathematics"),
        PHYSICS("Physics");

        private final String displayName;

        MajorOptions(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
            tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
            tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
            tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));
            tv.setItems(data);

            // Populate major dropdown
            majorComboBox.getItems().addAll(MajorOptions.values());
            majorComboBox.getSelectionModel().selectFirst();

            // Disable edit and delete buttons initially
            editButton.setDisable(true);
            deleteButton.setDisable(true);

            // Listen for table selection changes
            tv.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                boolean hasSelection = newSelection != null;
                editButton.setDisable(!hasSelection);
                deleteButton.setDisable(!hasSelection);

                //update MenuItem state
                if (editItem != null) editItem.setDisable(!hasSelection);
                if (deleteItem != null) deleteItem.setDisable(!hasSelection);
            });

            // Connect menu items to button actions
            if (editItem != null) {
                editItem.setOnAction(event -> editRecord());
                editItem.setDisable(true);
            }

            if (deleteItem != null) {
                deleteItem.setOnAction(event -> deleteRecord());
                deleteItem.setDisable(true);
            }

            // Setup form validation
            setupFormValidation();

            // Setup search functionality
            setupSearch();

            // Setup auto backup
            setupAutoBackup();

            // Initialize status label
            statusLabel.setText("Ready");

        } catch (Exception e) {
            showStatusMessage("Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupFormValidation() {
        // Add listeners to form fields for validation
        first_name.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        last_name.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        email.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        department.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        majorComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validateForm());

        // Initially validate the form
        validateForm();
    }

    private boolean validateForm() {
        boolean isFirstNameValid = validateName(first_name.getText());
        boolean isLastNameValid = validateName(last_name.getText());
        boolean isEmailValid = validateEmail(email.getText());
        boolean isDepartmentValid = validateDepartment(department.getText());
        boolean isMajorValid = majorComboBox.getValue() != null;

        // Set visual indication for validation
        setValidationStyle(first_name, isFirstNameValid);
        setValidationStyle(last_name, isLastNameValid);
        setValidationStyle(email, isEmailValid);
        setValidationStyle(department, isDepartmentValid);

        // Enable add button only if all required fields are valid
        addButton.setDisable(!(isFirstNameValid && isLastNameValid && isEmailValid));

        // Return overall validation result
        return isFirstNameValid && isLastNameValid && isEmailValid && isMajorValid;
    }

    private void setValidationStyle(TextField field, boolean isValid) {
        if (isValid) {
            field.setStyle("-fx-border-color: green;");
        } else {
            field.setStyle("-fx-border-color: red;");
        }
    }

    // Validation methods with regex patterns
    private boolean validateName(String name) {
        // Name validation: letters, spaces, hyphens, min of 2 characters
        String regex = "^[A-Za-z\\s-]{2,}$";
        return name != null && !name.isEmpty() && name.matches(regex);
    }

    private boolean validateEmail(String email) {
        // email validation pattern
        String regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email != null && !email.isEmpty() && email.matches(regex);
    }

    private boolean validateDepartment(String department) {
        // Department validation: letters, numbers, spaces, hyphens
        String regex = "^[A-Za-z0-9\\s-]{2,}$";
        return department == null || department.isEmpty() || department.matches(regex);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                tv.setItems(data);
            } else {
                ObservableList<Person> filteredList = FXCollections.observableArrayList();
                String searchText = newValue.toLowerCase();

                for (Person person : data) {
                    if (person.getFirstName().toLowerCase().contains(searchText) ||
                            person.getLastName().toLowerCase().contains(searchText) ||
                            person.getEmail().toLowerCase().contains(searchText) ||
                            (person.getDepartment() != null && person.getDepartment().toLowerCase().contains(searchText)) ||
                            person.getMajor().toLowerCase().contains(searchText)) {
                        filteredList.add(person);
                    }
                }

                tv.setItems(filteredList);
            }
        });
    }

    private void setupAutoBackup() {
        // Setup scheduled backup
        Timer timer = new Timer(true);

        // Schedule backup every 30 minutes
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    try {
                        File backupDir = new File("backups");
                        if (!backupDir.exists()) {
                            backupDir.mkdir();
                        }

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                        String fileName = "backup_" + dateFormat.format(new Date()) + ".csv";
                        File backupFile = new File(backupDir, fileName);

                        BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile));

                        // Write header
                        writer.write("FirstName,LastName,Department,Major,Email,ImageURL");
                        writer.newLine();

                        // Write data
                        for (Person p : data) {
                            writer.write(String.format("%s,%s,%s,%s,%s,%s",
                                    p.getFirstName(),
                                    p.getLastName(),
                                    p.getDepartment(),
                                    p.getMajor(),
                                    p.getEmail(),
                                    p.getImageURL()));
                            writer.newLine();
                        }

                        writer.close();
                        showStatusMessage("Auto backup created: " + fileName);
                    } catch (Exception e) {
                        System.err.println("Auto backup failed: " + e.getMessage());
                    }
                });
            }
        }, 30 * 60 * 1000, 30 * 60 * 1000); // 30 minutes in milliseconds
    }

    @FXML
    protected void addNewRecord() {
        try {
            Person p = new Person(first_name.getText(), last_name.getText(), department.getText(),
                    majorComboBox.getValue().toString(), email.getText(), imageURL.getText());
            cnUtil.insertUser(p);
            cnUtil.retrieveId(p);
            p.setId(cnUtil.retrieveId(p));
            data.add(p);
            clearForm();
            showStatusMessage("Record added successfully!");
        } catch (Exception e) {
            showStatusMessage("Error adding record: " + e.getMessage());
        }
    }

    @FXML
    protected void clearForm() {
        first_name.setText("");
        last_name.setText("");
        department.setText("");
        email.setText("");
        imageURL.setText("");
        majorComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    protected void logOut(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/academicTheme.css").toExternalForm());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void closeApplication() {
        System.exit(0);
    }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/about.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            scene.getStylesheets().add(getClass().getResource("/css/academicTheme.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void editRecord() {
        try {
            // Makes sure we have a selected item
            Person p = tv.getSelectionModel().getSelectedItem();
            if (p == null) {
                showStatusMessage("Please select a record to edit");
                return;
            }

            // Validates form fields
            if (!validateForm()) {
                showStatusMessage("Please correct the form errors before saving");
                return;
            }

            // Get the ID of the selected record
            int id = p.getId();

            // Create updated person object
            Person p2 = new Person(
                    id,
                    first_name.getText(),
                    last_name.getText(),
                    department.getText(),
                    majorComboBox.getValue().toString(),
                    email.getText(),
                    imageURL.getText()
            );

            // Update in database
            cnUtil.editUser(id, p2);

            // Update in UI list
            int index = data.indexOf(p);
            data.remove(p);
            data.add(index, p2);
            tv.getSelectionModel().select(index);

            showStatusMessage("Record updated successfully!");
        } catch (Exception e) {
            showStatusMessage("Error updating record: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void deleteRecord() {
        try {
            // Make sure we have a selected item
            Person p = tv.getSelectionModel().getSelectedItem();
            if (p == null) {
                showStatusMessage("Please select a record to delete");
                return;
            }

            // Confirm deletion
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Delete");
            alert.setHeaderText("Delete Record");
            alert.setContentText("Are you sure you want to delete " + p.getFirstName() + " " + p.getLastName() + "?");

            if (alert.showAndWait().get() == ButtonType.OK) {
                // Delete from database
                cnUtil.deleteRecord(p);

                // Remove from UI list
                int index = data.indexOf(p);
                data.remove(index);

                // Clear form
                clearForm();

                // Select next item if available
                if (!data.isEmpty()) {
                    if (index < data.size()) {
                        tv.getSelectionModel().select(index);
                    } else {
                        tv.getSelectionModel().select(data.size() - 1);
                    }
                }

                showStatusMessage("Record deleted successfully!");
            }
        } catch (Exception e) {
            showStatusMessage("Error deleting record: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void showImage() {
        File file = (new FileChooser()).showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            img_view.setImage(new Image(file.toURI().toString()));
            imageURL.setText(file.toURI().toString());
        }
    }

    @FXML
    protected void addRecord() {
        showSomeone();
    }

    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p != null) {
            first_name.setText(p.getFirstName());
            last_name.setText(p.getLastName());
            department.setText(p.getDepartment());

            // Set the major ComboBox to the correct value
            for (MajorOptions option : majorComboBox.getItems()) {
                if (option.toString().equals(p.getMajor())) {
                    majorComboBox.setValue(option);
                    break;
                }
            }

            email.setText(p.getEmail());
            imageURL.setText(p.getImageURL());

            // Show image if URL is provided
            if (p.getImageURL() != null && !p.getImageURL().isEmpty()) {
                try {
                    img_view.setImage(new Image(p.getImageURL()));
                } catch (Exception e) {
                    img_view.setImage(null);
                }
            } else {
                img_view.setImage(null);
            }
        }
    }

    public void lightTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            Stage stage = (Stage) scene.getWindow();
            stage.getScene().getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            System.out.println("light " + scene.getStylesheets());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void darkTheme(ActionEvent actionEvent) {
        try {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/darkTheme.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void academicTheme(ActionEvent actionEvent) {
        try {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/academicTheme.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSomeone() {
        Dialog<Results> dialog = new Dialog<>();
        dialog.setTitle("New User");
        dialog.setHeaderText("Please specify…");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField textField1 = new TextField("Name");
        TextField textField2 = new TextField("Last Name");
        TextField textField3 = new TextField("Email ");
        ObservableList<Major> options =
                FXCollections.observableArrayList(Major.values());
        ComboBox<Major> comboBox = new ComboBox<>(options);
        comboBox.getSelectionModel().selectFirst();
        dialogPane.setContent(new VBox(8, textField1, textField2,textField3, comboBox));
        Platform.runLater(textField1::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new Results(textField1.getText(),
                        textField2.getText(), comboBox.getValue());
            }
            return null;
        });
        Optional<Results> optionalResult = dialog.showAndWait();
        optionalResult.ifPresent((Results results) -> {
            MyLogger.makeLog(
                    results.fname + " " + results.lname + " " + results.major);
        });
    }

    private static enum Major {Business, CSC, CPIS}

    private static class Results {
        String fname;
        String lname;
        Major major;

        public Results(String name, String date, Major venue) {
            this.fname = name;
            this.lname = date;
            this.major = venue;
        }
    }

    // Method to display status messages to the user
    private void showStatusMessage(String message) {
        statusLabel.setText(message);

        // Clear after 5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> statusLabel.setText("Ready"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Methods for CSV import/export
    @FXML
    protected void importCSV(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(menuBar.getScene().getWindow());

        if (selectedFile != null) {
            try {
                List<Person> importedPersons = new ArrayList<>();
                BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue; // Skip header
                    }

                    String[] parts = line.split(",");
                    if (parts.length >= 6) {
                        Person p = new Person(
                                parts[0].trim(), // firstName
                                parts[1].trim(), // lastName
                                parts[2].trim(), // department
                                parts[3].trim(), // major
                                parts[4].trim(), // email
                                parts.length > 5 ? parts[5].trim() : "" // imageURL
                        );
                        cnUtil.insertUser(p);
                        p.setId(cnUtil.retrieveId(p));
                        importedPersons.add(p);
                    }
                }
                reader.close();

                data.addAll(importedPersons);
                showStatusMessage("Imported " + importedPersons.size() + " records successfully!");
            } catch (Exception e) {
                showStatusMessage("Error importing CSV: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    protected void exportCSV(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showSaveDialog(menuBar.getScene().getWindow());

        if (selectedFile != null) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile));

                // Write header
                writer.write("FirstName,LastName,Department,Major,Email,ImageURL");
                writer.newLine();

                // Write data
                for (Person p : data) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s",
                            p.getFirstName(),
                            p.getLastName(),
                            p.getDepartment(),
                            p.getMajor(),
                            p.getEmail(),
                            p.getImageURL()));
                    writer.newLine();
                }

                writer.close();
                showStatusMessage("Exported " + data.size() + " records successfully!");
            } catch (Exception e) {
                showStatusMessage("Error exporting CSV: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Statistics Dashboard
    @FXML
    protected void showStatistics() {
        try {
            // Count by major
            Map<String, Integer> majorCounts = new HashMap<>();

            for (Person person : data) {
                String major = person.getMajor();
                majorCounts.put(major, majorCounts.getOrDefault(major, 0) + 1);
            }

            // Count by department
            Map<String, Integer> departmentCounts = new HashMap<>();

            for (Person person : data) {
                String dept = person.getDepartment();
                if (dept != null && !dept.isEmpty()) {
                    departmentCounts.put(dept, departmentCounts.getOrDefault(dept, 0) + 1);
                }
            }

            // Create a statistics window
            Stage statsStage = new Stage();
            statsStage.setTitle("Academic Hub Statistics");

            VBox root = new VBox(10);
            root.setPadding(new Insets(15));

            Label statsLabel = new Label("Database Statistics");
            statsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            Label totalLabel = new Label("Total Records: " + data.size());
            totalLabel.setStyle("-fx-font-size: 14px;");

            // Major statistics
            Label majorLabel = new Label("Distribution by Major:");
            majorLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            VBox majorBox = new VBox(5);
            for (Map.Entry<String, Integer> entry : majorCounts.entrySet()) {
                ProgressBar bar = new ProgressBar((double)entry.getValue() / data.size());
                bar.setPrefWidth(300);

                HBox row = new HBox(10);
                row.getChildren().addAll(
                        new Label(entry.getKey() + ":"),
                        bar,
                        new Label(entry.getValue() + " (" + String.format("%.1f", 100.0 * entry.getValue() / data.size()) + "%)")
                );

                majorBox.getChildren().add(row);
            }

            // Department statistics
            Label deptLabel = new Label("Distribution by Department:");
            deptLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            VBox deptBox = new VBox(5);
            for (Map.Entry<String, Integer> entry : departmentCounts.entrySet()) {
                ProgressBar bar = new ProgressBar((double)entry.getValue() / data.size());
                bar.setPrefWidth(300);

                HBox row = new HBox(10);
                row.getChildren().addAll(
                        new Label(entry.getKey() + ":"),
                        bar,
                        new Label(entry.getValue() + " (" + String.format("%.1f", 100.0 * entry.getValue() / data.size()) + "%)")
                );

                deptBox.getChildren().add(row);
            }

            Button closeButton = new Button("Close");
            closeButton.setOnAction(e -> statsStage.close());

            Button generateReportButton = new Button("Generate PDF Report");
            generateReportButton.setOnAction(e -> generatePDFReport(majorCounts, departmentCounts));

            HBox buttonBox = new HBox(10);
            buttonBox.getChildren().addAll(generateReportButton, closeButton);

            root.getChildren().addAll(
                    statsLabel,
                    totalLabel,
                    new Separator(),
                    majorLabel,
                    majorBox,
                    new Separator(),
                    deptLabel,
                    deptBox,
                    buttonBox
            );

            Scene scene = new Scene(root, 500, 500);
            statsStage.setScene(scene);
            statsStage.show();

        } catch (Exception e) {
            showStatusMessage("Error showing statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // PDF Report Generation (Extra Credit)
    private void generatePDFReport(Map<String, Integer> majorCounts, Map<String, Integer> departmentCounts) {
        try {
            File report = PDFReportGenerator.generateMajorReport(majorCounts, departmentCounts, data.size());
            if (report != null) {
                PDFReportGenerator.openReport(report);
                showStatusMessage("PDF Report generated successfully!");
            } else {
                showStatusMessage("Failed to generate PDF report.");
            }
        } catch (Exception e) {
            showStatusMessage("Error generating PDF report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}