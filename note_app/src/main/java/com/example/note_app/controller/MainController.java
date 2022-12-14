package com.example.note_app.controller;

import com.example.note_app.Main;
import com.example.note_app.dao.CategoryDaoImpl;
import com.example.note_app.dao.ContentDaoImpl;
import com.example.note_app.dao.UserDaoImpl;
import com.example.note_app.entity.Category;
import com.example.note_app.entity.Content;
import com.example.note_app.entity.User;
import com.example.note_app.entity.relationship.UserCategory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jfoenix.controls.JFXToggleButton;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javafx.scene.web.HTMLEditor;
import org.json.*;

public class MainController implements Initializable {

    /**
     * FXML Component
     **/
    @FXML
    private ListView<Category> listCategory;
    @FXML
    private Label labelStatus;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnUpdate;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnComment;
    @FXML
    private Button btnShare;
    @FXML
    private Button btnAddCategory;
    @FXML
    private Button btnAddNote;
    @FXML
    private Button btnSettings;
    @FXML
    private Button btnClose;
    @FXML
    private ListView<Content> listContent;
    @FXML
    private TextField txtTitle;
    @FXML
    private Label labelKeterangan;
    @FXML
    private HTMLEditor txtArea;
    @FXML
    private Label labelUser;
    @FXML
    private AnchorPane parent;
    @FXML
    private JFXToggleButton btnMode;


    /**
     * User hasil login
     */
    private User loggedUser;
    private UserDaoImpl userDao;
    private final Path pathSetting = Paths.get("data/setting.json");

    /**
     * Content Variables
     **/
    private ObservableList<Content> contents;
    private ContentDaoImpl contentDao;
    private Content selectedContent;


    /**
     * Category Variables
     */
    private ObservableList<Category> categories;
    private CategoryDaoImpl categoryDao;


    /**
     * Class method
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        showLogin();

        // User
        userDao = new UserDaoImpl();
        labelUser.setText("Hello, " + loggedUser.getUsername() + "!");

        // Category
        categoryDao = new CategoryDaoImpl();
        categories = FXCollections.observableArrayList();
        refreshCategory();
        initCategory();

        // Content
        contentDao = new ContentDaoImpl();
        contents = FXCollections.observableArrayList();
        refreshContent();
        initContent();

        // Setting mode
        try {
            JSONObject setting = new JSONObject(Files.readString(pathSetting));
            if (setting.getString("mode").equals("dark")) {
                isLightMode = true;
                btnMode.setSelected(true);
            } else {
                isLightMode = false;
            }
            onActionMode(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void reset() {
        txtTitle.clear();
        txtArea.setHtmlText("");
        btnSave.setDisable(false);
        btnUpdate.setDisable(true);
        btnDelete.setDisable(true);
        labelKeterangan.setText("");
    }


    /**
     * Content method
     **/
    private void initContent() {
        listContent.getSelectionModel().selectFirst();
        selectedContent = listContent.getSelectionModel().getSelectedItem();
        if (selectedContent != null) {
            labelKeterangan.setText("Created " + selectedContent.getCreatedAt() +" (" + selectedContent.getCreateTime() +")" + "\t\t\t Modified " + selectedContent.getUpdatedAt() +" (" + selectedContent.getUpdateTime() +")");
            txtTitle.setText(selectedContent.getContentTitle());
            txtArea.setHtmlText(selectedContent.getContentField());
        }

        listCategory.getSelectionModel().selectedItemProperty().addListener((observableValue, category, t1) -> {
            Category selectedCategory = listCategory.getSelectionModel().getSelectedItem();
            contents = FXCollections.observableArrayList(loggedUser.getContents());
            contents = contents.filtered(content -> content.getCategories().contains(selectedCategory));
            listContent.setItems(contents);
            listContent.getSelectionModel().selectFirst();
            btnAddNote.setDisable(false);
            if (selectedCategory == null) {
                btnAddNote.setDisable(true);
            }
        });

        listContent.getSelectionModel().selectedItemProperty().addListener((observableValue, content, t1) -> {
            selectedContent = listContent.getSelectionModel().getSelectedItem();
            if (selectedContent != null) {
                txtTitle.setDisable(false);
                txtArea.setDisable(false);
                btnSave.setDisable(false);
                btnDelete.setDisable(false);
                btnComment.setDisable(false);
                btnShare.setDisable(false);
                labelKeterangan.setText("Created " + selectedContent.getCreatedAt() +" (" + selectedContent.getCreateTime() +")" + "\t\t\t Modified " + selectedContent.getUpdatedAt() +" (" + selectedContent.getUpdateTime() +")");
                txtTitle.setText(selectedContent.getContentTitle());
                if (selectedContent.getContentField() == null) {
                    txtArea.setHtmlText("");
                } else {
                    txtArea.setHtmlText(selectedContent.getContentField());
                }
            } else {
                txtTitle.setText("");
                txtArea.setHtmlText("");
                labelKeterangan.setText("");
                txtTitle.setDisable(true);
                txtArea.setDisable(true);

                btnSave.setDisable(true);
                btnDelete.setDisable(true);
                btnComment.setDisable(true);
                btnShare.setDisable(true);
            }
        });
    }

    @FXML
    protected void onActionSaveContent(ActionEvent actionEvent) {
        selectedContent.setContentTitle(txtTitle.getText().trim());
        selectedContent.setContentField(txtArea.getHtmlText());
        int selectedIndex = listContent.getSelectionModel().getSelectedItem().getContentId();
        if (contentDao.updateData(selectedContent) == 1) {
            labelStatus.setText("Note Saved!");
            labelStatus.setVisible(true);
            refreshContent();
            contents.forEach(content -> {
                if (content.getContentId() == selectedIndex) {
                    listContent.getSelectionModel().select(content);
                }
            });
        }
        PauseTransition visiblePause = new PauseTransition(
                Duration.seconds(1)
        );
        visiblePause.setOnFinished(
                event -> labelStatus.setVisible(false)
        );
        visiblePause.play();
    }


    public void contentClicked(MouseEvent mouseEvent) {
//        selectedContent = listContent.getSelectionModel().getSelectedItem();
//        if (selectedContent != null) {
//            labelKeterangan.setText("Created in : "+selectedContent.getCreatedAt()+"\t Updated in : "+selectedContent.getUpdatedAt());
//            txtTitle.setText(selectedContent.getContentTitle());
//            txtArea.setText(selectedContent.getContentField());
//        }
    }

    @FXML
    protected void onActionDeleteContent(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Are you sure want to delete?");
        alert.showAndWait();
        if (alert.getResult() == ButtonType.OK) {
            if (contentDao.deleteData(selectedContent, loggedUser) == 1) {
                labelStatus.setText("Note Deleted!");
                labelStatus.setVisible(true);
                refreshContent();
                listContent.getSelectionModel().selectFirst();
            }
            PauseTransition visiblePause = new PauseTransition(
                    Duration.seconds(1)
            );
            visiblePause.setOnFinished(
                    event -> labelStatus.setVisible(false)
            );
            visiblePause.play();
        }
    }

    public void newNote() {
        Content newContent = new Content();

        Set<User> userContent = new HashSet<>();
        userContent.add(loggedUser);
        Set<Category> categoryContent = new HashSet<>();
        categoryContent.add(listCategory.getSelectionModel().getSelectedItem());

        newContent.setContentTitle("Untitled");
        newContent.setUsers(userContent);
        newContent.setCategories(categoryContent);

        contentDao.addData(newContent, loggedUser);

        refreshContent();

        listContent.getSelectionModel().select(newContent);
        txtArea.requestFocus();
    }

    private void refreshContent() {
        loggedUser = userDao.fetchUser(loggedUser.getUsername(), loggedUser.getPassword());
        contents = FXCollections.observableArrayList(loggedUser.getContents());
        contents = contents.filtered(content -> content.getCategories().contains(listCategory.getSelectionModel().getSelectedItem()));
        listContent.setItems(contents);
    }

    public Content getSelectedContent() {
        return selectedContent;
    }


    /**
     * Category method
     */
    private void initCategory() {
        listCategory.getSelectionModel().selectFirst();
        listCategory.setEditable(true);
        listCategory.setCellFactory(TextFieldListCell.forListView(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category.getCategoryName();
            }

            @Override
            public Category fromString(String s) {
                Category category1 = new Category();
                category1.setCategoryName(s);
                category1.setCategoryId(listCategory.getSelectionModel().getSelectedItem().getCategoryId());
                category1.setCategoryDescription(listCategory.getSelectionModel().getSelectedItem().getCategoryDescription());
                return category1;
            }
        }));

        ContextMenu deleteMenu = new ContextMenu();
        MenuItem delete = new MenuItem("Delete Category");
        deleteMenu.getItems().add(delete);
        delete.setOnAction(actionEvent -> {
                    UserCategory userCategory = categoryDao.fetchUserCategory(listCategory.getSelectionModel().getSelectedItem(), loggedUser);
                    categoryDao.deleteData(userCategory);
                    refreshCategory();
                }
        );

        listCategory.setContextMenu(deleteMenu);
    }

    public void editCommit(ListView.EditEvent<Category> editEvent) {
        Set<User> userCategory = new HashSet<>();
        userCategory.add(loggedUser);
        editEvent.getNewValue().setUsers(userCategory);

        categoryDao.updateData(editEvent.getNewValue());
        refreshCategory();
    }

    public void newCategory(ActionEvent actionEvent) {
        Category newCategory = new Category();
        Set<User> userCategory = new HashSet<>();
        userCategory.add(loggedUser);

        newCategory.setCategoryName("Untitled");
        newCategory.setUsers(userCategory);
        categoryDao.addData(newCategory);
        categories.add(newCategory);

        listCategory.getSelectionModel().select(newCategory);
        int i = listCategory.getSelectionModel().getSelectedIndex();
        listCategory.scrollTo(i);
        listCategory.edit(i);
    }

    private void refreshCategory() {
        loggedUser = userDao.fetchUser(loggedUser.getUsername(), loggedUser.getPassword());
        categories.clear();
        categories.addAll(loggedUser.getCategories());
        listCategory.setItems(categories);
    }


    /**
     * Login method
     */
    private void showLogin() {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("LoginView.fxml"));
        Scene scene = null;
        try {
            scene = new Scene(fxmlLoader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setTitle("Login");
        stage.setScene(scene);

        LoginController loginController = fxmlLoader.getController();
        loginController.setMainController(this);

        if (!loginController.checkLoginInfo()) {
            stage.showAndWait();
        }
        if (loggedUser == null) {
            Platform.exit();
            System.exit(0);
        }
    }

    public void setLoggedUser(User user) {
        loggedUser = user;
    }

    public User getLoggedUser() {
        return loggedUser;
    }

    public void logOut() throws IOException {
        Files.deleteIfExists(Paths.get("data/user.json"));
        labelUser.getScene().getWindow().hide();

        Platform.runLater(() -> {
            try {
                new Main().start( new Stage() );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Feedback method
     */
    public void showComments() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("FeedbackListView.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        stage.setTitle("Comments");
        stage.setScene(scene);

        FeedbackController feedbackController = fxmlLoader.getController();
        feedbackController.setMainController(this);
        feedbackController.setFeedbacks();
        feedbackController.load();

        stage.show();
    }


    /**
     * Collaborator method
     */
    public void showShare() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("CollaboratorView.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        stage.setTitle("Share");
        stage.setScene(scene);

        CollaboratorController collaboratorController = fxmlLoader.getController();
        collaboratorController.setMainController(this);
        collaboratorController.setUsers(selectedContent);

        stage.show();
    }

    /**
     * Light/Dark mode
     */

    private boolean isLightMode;

    public void onActionMode(ActionEvent actionEvent) {
        isLightMode = !isLightMode;
        if (isLightMode) {
            setLightMode();
        } else {
            setDarkMode();
        }
    }

    private void setLightMode() {
        parent.getStylesheets().remove(0);
        parent.getStylesheets().add(Main.class.getResource("style/light-main.css").toExternalForm());
        btnMode.setText("DAY");
        btnMode.setTextFill(Color.BLACK);
        String mode = "{\n" +
                "  \"mode\" : \"light\"\n" +
                "}";
        try {
            Files.write(pathSetting, mode.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setDarkMode() {
        parent.getStylesheets().remove(0);
        parent.getStylesheets().add(Main.class.getResource("style/dark-main.css").toExternalForm());
        btnMode.setText("NIGHT");
        btnMode.setTextFill(Color.WHITE);
        String mode = "{\n" +
                "  \"mode\" : \"dark\"\n" +
                "}";
        try {
            Files.write(pathSetting, mode.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * About method
    */
    public void onActionAbout(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("AboutUsView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setTitle("About Us");
        stage.setScene(scene);
        stage.show();
    }
}