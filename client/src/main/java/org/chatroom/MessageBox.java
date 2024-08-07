package org.chatroom;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MessageBox extends AnchorPane {
    private final Boolean isCurrentUser;
    private final Boolean ignoreDate;
    private final Boolean ignoreUsername;
    private final VBox messageBox;
    private final Node content;
    private HBox usernameUi;
    private Label dateLabel;

    public MessageBox(String username, Date date, String text, Boolean isCurrentUser, Message lastMessage) {
        this.isCurrentUser = isCurrentUser;
        this.ignoreDate = lastMessage != null && lastMessage.date.getTime() / 60000 == date.getTime() / 60000;
        this.ignoreUsername = lastMessage != null && ignoreDate && lastMessage.username.equals(username);

        setDateAndUsername(username, date);

        HBox bubble = createMessageBubble(text);
        bubble.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        this.content = bubble;

        ArrayList<Node> children = new ArrayList<>();
        if (!ignoreDate) {
            children.add(dateLabel);
        }
        if (!ignoreUsername) {
            children.add(usernameUi);
        }
        children.add(content);
        messageBox = new VBox(10, children.toArray(new Node[0]));
        this.addToPane();
    }

    public MessageBox(String username, Date date, Image image, Boolean isCurrentUser, Message lastMessage) {
        this.isCurrentUser = isCurrentUser;
        this.ignoreDate = lastMessage != null && lastMessage.username.equals(username);
        this.ignoreUsername = lastMessage != null && lastMessage.date.equals(date);

        setDateAndUsername(username, date);

        ImageView imageView = (new ImageView(image));
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(true);
        HBox bubble = new HBox(imageView);
        bubble.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        this.content = bubble;

        ArrayList<Node> children = new ArrayList<>();
        if (!ignoreDate) {
            children.add(dateLabel);
        }
        if (!ignoreUsername) {
            children.add(usernameUi);
        }
        children.add(content);
        messageBox = new VBox(10, children.toArray(new Node[0]));
        this.addToPane();
    }

    public static String formatDate(Date date) {
        Date now = new Date();
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("MM-dd HH:mm");
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date);
        cal2.setTime(now);

        boolean isToday = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);

        if (isToday) {
            return timeFormatter.format(date);
        } else {
            return dateTimeFormatter.format(date);
        }
    }

    private void addToPane() {
        messageBox.setMaxWidth(Region.USE_PREF_SIZE);
        messageBox.setSpacing(5);
        messageBox.setPadding(new Insets(16, 16, 8, 8));

        this.getChildren().add(messageBox);

        AnchorPane.setLeftAnchor(messageBox, 0.0);
        AnchorPane.setRightAnchor(messageBox, 0.0);

        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
    }

    public void setDateAndUsername(String username, Date date) {
        if (!ignoreDate) {
            this.dateLabel = new Label(formatDate(date));
            dateLabel.setAlignment(Pos.CENTER);
            dateLabel.setMaxWidth(Double.MAX_VALUE);
        }

        if (!ignoreUsername) {
            Label usernameLabel = new Label(username);
            usernameLabel.setStyle("-fx-font-size: 8pt; -fx-font-weight: bold; -fx-padding-bottom: 0px;");
            this.usernameUi = new HBox(usernameLabel);
            usernameUi.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        }
    }

    private HBox createMessageBubble(String message) {
        StackPane bubble = new StackPane();

        Rectangle background = new Rectangle();
        background.setArcWidth(15);
        background.setArcHeight(15);
        background.setFill(Color.WHITE);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setPadding(new Insets(5, 10, 5, 10));

        bubble.getChildren().addAll(background, messageLabel);

        background.widthProperty().bind(bubble.widthProperty());
        background.heightProperty().bind(bubble.heightProperty());

        bubble.setMaxWidth(Region.USE_PREF_SIZE);
        HBox wrapper = new HBox(bubble);
        wrapper.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        if (ignoreUsername) {
            wrapper.setPadding(new Insets(-20, 0, 0, 0));
        }
        return wrapper;
    }
}
