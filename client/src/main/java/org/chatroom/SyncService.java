package org.chatroom;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class SyncService {
    private final Consumer<Message> msgHandler;
    private LocalStorage localStorage;
    private RemoteStorage remoteStorage;

    public SyncService(Consumer<Message> msgHandler) {
        this.msgHandler = msgHandler;
        try {
            this.localStorage = new LocalStorage();
        } catch (SQLException e) {
            ChatClient.alertError("Failed to initialize local storage: " + e.getMessage());
        }
        try {
            this.remoteStorage = new RemoteStorage((msg) -> {
                try {
                    localStorage.saveMessage(msg);
                    msgHandler.accept(msg);
                } catch (SQLException e) {
                    ChatClient.alertError("Failed to save message: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            ChatClient.alertError("Failed to initialize remote storage: " + e.getMessage());
        }
    }

    public void start() {
        TreeSet<Message> messages;
        try {
            messages = new TreeSet<>(localStorage.loadMessages());
            Date lastSyncTime = messages.isEmpty() ? new Date(0) : messages.last().date;
            List<Message> remoteMessages = remoteStorage.loadMessages(lastSyncTime);
            for (Message m : remoteMessages) {
                if (messages.add(m)) {
                    localStorage.saveMessage(m);
                }
            }
        } catch (SQLException e) {
            ChatClient.alertError("Failed to load data: " + e.getMessage());
            return;
        }
        for (Message m : messages) {
            msgHandler.accept(m);
        }
        remoteStorage.start(ChatClient.username);
    }

    public void sendMessage(Message message) {
        try {
            localStorage.saveMessage(message);
            remoteStorage.sendMessage(message);
        } catch (SQLException e) {
            ChatClient.alertError("Failed to send message: " + e.getMessage());
        }
    }
}
