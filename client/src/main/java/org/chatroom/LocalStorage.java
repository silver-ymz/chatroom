package org.chatroom;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalStorage {
    private final Connection connection;

    public LocalStorage() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:chat.db");
        createTable();
    }

    private void createTable() throws SQLException {
        String textsSql = """
                CREATE TABLE IF NOT EXISTS text_msg (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    text TEXT NOT NULL
                )
                """;
        String imagesSql = """
                CREATE TABLE IF NOT EXISTS image_msg (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    image BLOB NOT NULL
                );
                """;
        try (PreparedStatement stmt = connection.prepareStatement(textsSql)) {
            stmt.execute();
        }
        try (PreparedStatement stmt = connection.prepareStatement(imagesSql)) {
            stmt.execute();
        }
    }

    public void saveMessage(Message message) throws SQLException {
        if (message.content instanceof String) {
            saveText(message.username, message.date, (String) message.content);
        } else {
            saveImage(message.username, message.date, (byte[]) message.content);
        }
    }

    private void saveText(String user, java.util.Date date, String text) throws SQLException {
        String sql = "INSERT INTO text_msg (username, date, text) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user);
            stmt.setLong(2, date.getTime());
            stmt.setString(3, text);
            stmt.execute();
        }
    }

    private void saveImage(String user, java.util.Date date, byte[] image) throws SQLException {
        String sql = "INSERT INTO image_msg (username, date, image) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user);
            stmt.setLong(2, date.getTime());
            stmt.setBytes(3, image);
            stmt.execute();
        }
    }

    public List<Message> loadMessages() throws SQLException {
        ArrayList<Message> messages = new ArrayList<>();
        messages.addAll(loadTexts());
        messages.addAll(loadImages());
        return messages;
    }

    private ArrayList<Message> loadTexts() throws SQLException {
        ArrayList<Message> texts = new ArrayList<>();
        String sql = "SELECT username, date, text FROM text_msg";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message();
                    message.username = rs.getString("username");
                    message.date = new Date(rs.getLong("date"));
                    message.content = rs.getString("text");
                    texts.add(message);
                }
            }
        }
        return texts;
    }

    private ArrayList<Message> loadImages() throws SQLException {
        ArrayList<Message> images = new ArrayList<>();
        String sql = "SELECT username, date, image FROM image_msg";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message();
                    message.username = rs.getString("username");
                    message.date = new Date(rs.getLong("date"));
                    message.content = rs.getBytes("image");
                    images.add(message);
                }
            }
        }
        return images;
    }
}
