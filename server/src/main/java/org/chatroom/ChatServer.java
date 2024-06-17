package org.chatroom;

import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    public static int PORT = Integer.parseInt(System.getenv("SERVER_PORT"));
    public static String DB_URL = System.getenv("DB_URL");

    private static Connection connection;
    private static DataInputStream messageIn;
    private static ExecutorService executor;
    private static final Map<String, DataOutputStream> clients = new HashMap<>();

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        ServerSocket socket;
        DataOutputStream messageOut;

        try {
            executor = Executors.newFixedThreadPool(4);
            socket = new ServerSocket(PORT);
            PipedInputStream pipedIn = new PipedInputStream();
            PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
            messageIn = new DataInputStream(pipedIn);
            messageOut = new DataOutputStream(pipedOut);
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
            return;
        }
        try {
            connection = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Server started on port " + PORT);

        executor.execute(() -> {
            try {
                while (true) {
                    int length = messageIn.readInt();
                    byte[] data = new byte[length];
                    messageIn.readFully(data);
                    Message message = SerializationUtils.deserialize(data);
                    String username = message.username;
                    if (message.content instanceof String) {
                        saveText(message.username, message.date, (String) message.content);
                    } else {
                        saveImage(message.username, message.date, (byte[]) message.content);
                    }
                    for (Map.Entry<String, DataOutputStream> entry : clients.entrySet()) {
                        if (entry.getKey().equals(username)) {
                            continue;
                        }
                        byte[] messageData = SerializationUtils.serialize((Serializable) message);
                        entry.getValue().writeInt(messageData.length);
                        entry.getValue().write(messageData);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to handle messageIn: " + e.getMessage());
            }
        });

        try {
            while (true) {
                Socket client = socket.accept();
                executor.execute(new ClientHandle(client, messageOut));
            }
        } catch (IOException e) {
            System.err.println("Failed to accept client: " + e.getMessage());
        }
    }

    public static synchronized boolean registerClient(String username, DataOutputStream out) {
        if (clients.containsKey(username)) {
            return false;
        }
        try {
            out.write(1);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to register client: " + e.getMessage());
            return false;
        }
        clients.put(username, out);
        return true;
    }

    public static synchronized void unregisterClient(String username) {
        clients.remove(username);
    }

    public static List<Object> loadMessages(Date date) {
        String loadTextSql = "SELECT username, date, text FROM text_msg WHERE date > ?";
        String loadImageSql = "SELECT username, date, image FROM image_msg WHERE date > ?";
        try (PreparedStatement textStmt = connection.prepareStatement(loadTextSql);
             PreparedStatement imageStmt = connection.prepareStatement(loadImageSql)) {
            textStmt.setLong(1, date.getTime());
            imageStmt.setLong(1, date.getTime());
            List<Object> messages = new ArrayList<>();
            try (ResultSet rs = textStmt.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message();
                    message.username = rs.getString(1);
                    message.date = new Date(rs.getLong(2));
                    message.content = rs.getString(3);
                    messages.add(message);
                }
            }
            try (ResultSet rs = imageStmt.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message();
                    message.username = rs.getString(1);
                    message.date = new Date(rs.getLong(2));
                    message.content = rs.getBytes(3);
                    messages.add(message);
                }
            }
            return messages;
        } catch (SQLException e) {
            System.err.println("Failed to load messages: " + e.getMessage());
            return List.of();
        }
    }

    private static void saveText(String user, java.util.Date date, String text) {
        String sql = "INSERT INTO text_msg (username, date, text) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user);
            stmt.setLong(2, date.getTime());
            stmt.setString(3, text);
            stmt.execute();
        } catch (SQLException e) {
            System.err.println("Failed to save text: " + e.getMessage());
        }
    }

    private static void saveImage(String user, java.util.Date date, byte[] image) {
        String sql = "INSERT INTO image_msg (username, date, image) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user);
            stmt.setLong(2, date.getTime());
            stmt.setBytes(3, image);
            stmt.execute();
        } catch (SQLException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }
}
