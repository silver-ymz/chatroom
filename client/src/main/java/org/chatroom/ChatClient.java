package org.chatroom;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

public class ChatClient extends Application {
    private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_IMAGE_WIDTH = 1000;
    private static final int MAX_IMAGE_HEIGHT = 1000;

    public static String username = "test";
    public static Message lastMessage = null;

    private Stage primaryStage;
    private VBox messageArea;
    private TextArea inputField;
    private SyncService syncService;

    public static void main(String[] args) {
        launch(args);
    }

    public static void alertError(String msg) {
        VBox errorBox = new VBox(5);
        errorBox.setPadding(new Insets(10));
        Label label = new Label("An error occurred:\n" + msg);
        label.setWrapText(true);
        errorBox.getChildren().add(label);
        Scene errorScene = new Scene(errorBox, 200, 100);
        Stage errorStage = new Stage();
        errorStage.setTitle("Error");
        errorStage.setScene(errorScene);
        errorStage.show();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Chat Client");

        // Display a window to ask username, then set it to ChatClient.username
        VBox usernameBox = new VBox(5);
        usernameBox.setPadding(new Insets(10));
        usernameBox.getChildren().add(new Label("Enter your username:"));
        TextArea usernameField = new TextArea();
        usernameField.setPromptText("Username");
        usernameField.setPrefSize(200, 20);
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                String username = usernameField.getText().trim();
                if (!username.isEmpty()) {
                    ChatClient.username = username;
                    primaryStage.getScene().setRoot(new VBox());
                    startChat();
                }
            }
        });
        usernameBox.getChildren().add(usernameField);
        Button usernameButton = new Button("Set Username");
        usernameButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                ChatClient.username = username;
                primaryStage.getScene().setRoot(new VBox());
                startChat();
            }
        });
        usernameBox.getChildren().add(usernameButton);
        primaryStage.setScene(new Scene(usernameBox, 300, 100));
        primaryStage.show();
    }

    void startChat() {
        primaryStage.setTitle("Chat Client - " + username);

        // Message display area
        messageArea = new VBox();
        ScrollPane scrollPane = new ScrollPane(messageArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Input field
        inputField = new TextArea();
        inputField.setPromptText("Enter your message...");
        inputField.setPrefRowCount(1);
        inputField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                sendText();
            }
        });

        // Text Send button
        Button textSendButton = new Button("Send");
        textSendButton.setMinSize(50, textSendButton.getMinHeight());
        textSendButton.setOnAction(e -> sendText());

        // Image Send button
        Button imageSendButton = new Button("+");
        imageSendButton.setOnAction(e -> sendImage());

        // Layout setup
        HBox inputBox = new HBox(5, inputField, imageSendButton, textSendButton);
        inputBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(scrollPane);
        root.setBottom(inputBox);

        primaryStage.setScene(new Scene(root, 400, 600));
        primaryStage.show();

        // Initialize SyncService
        try {
            syncService = new SyncService((msg) -> Platform.runLater(() -> displayMessage(msg)));
            syncService.start();
        } catch (Exception e) {
            alertError("Error initializing SyncService.\n" + e.getMessage());
        }
    }

    void sendText() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            Date date = new Date();

            // Send message to sync service
            Message message = new Message();
            message.username = username;
            message.date = date;
            message.content = text;
            syncService.sendMessage(message);

            // Display message in message area
            displayMessage(message);
        }
        inputField.clear();
    }

    // 1. ask user to set image path
    // 2. check image limit, < 1MB, < 1000px x 1000px
    // 3. send image to sync service
    // 4. display image in message area
    void sendImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file == null) {
            return;
        }

        // Check image size
        if (file.length() > MAX_IMAGE_SIZE) {
            // Create new window to display error message
            alertError("Image size exceeds 1MB limit");
            return;
        }

        // Check image dimensions
        Image image = new Image(file.toURI().toString());
        if (image.isError()) {
            alertError("Error loading image file");
            return;
        }
        if (image.getWidth() > MAX_IMAGE_WIDTH || image.getHeight() > MAX_IMAGE_HEIGHT) {
            alertError("Image dimensions exceed 1000x1000 limit");
            return;
        }

        // Send image to sync service
        byte[] imageBytes;
        try {
            imageBytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            alertError("Error reading image file.\n" + e.getMessage());
            return;
        }
        Date date = new Date();
        Message message = new Message();
        message.username = username;
        message.date = date;
        message.content = imageBytes;
        syncService.sendMessage(message);

        // Display image in message area
        displayMessage(message);
    }

    public void displayMessage(Message message) {
        if (message.content instanceof String) {
            displayText(message.username, message.date, (String) message.content);
        } else {
            displayImage(message.username, message.date, new Image(new ByteArrayInputStream((byte[]) message.content)));
        }
        lastMessage = message;
    }

    // Don't call it directly, use displayMessage instead
    private void displayText(String username, Date date, String text) {
        messageArea.getChildren().add(new MessageBox(username, date, text, username.equals(ChatClient.username), lastMessage));
    }

    // Don't call it directly, use displayMessage instead
    private void displayImage(String username, Date date, Image image) {
        MessageBox messageBox = new MessageBox(username, date, image, username.equals(ChatClient.username), lastMessage);
        messageArea.getChildren().add(messageBox);
    }
}