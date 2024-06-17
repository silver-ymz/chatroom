package org.chatroom;

import org.apache.commons.lang3.SerializationUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class RemoteStorage {
    public static String SERVER_HOST = System.getenv("SERVER_HOST");
    public static int SERVER_PORT = Integer.parseInt(System.getenv("SERVER_PORT"));

    private final DataOutputStream out;
    private final DataInputStream in;
    private final Consumer<Message> msgHandler;
    private final Thread inboundThread;

    public RemoteStorage(Consumer<Message> msgHandler) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            ChatClient.alertError("Failed to connect to server: " + e.getMessage());
            throw new RuntimeException(e);
        }
        this.msgHandler = msgHandler;
        inboundThread = new Thread(this::inboundLoop);
        inboundThread.setDaemon(true);
    }

    public void sendMessage(Message message) {
        try {
            byte[] data = SerializationUtils.serialize(message);
            out.writeInt(data.length);
            out.write(data);
        } catch (IOException e) {
            ChatClient.alertError("Failed to send data: " + e.getMessage());
        }
    }

    public List<Message> loadMessages(Date lastSyncTime) {
        List<Message> messages;
        try {
            out.writeLong(lastSyncTime.getTime());
            out.flush();
            int length = in.readInt();
            byte[] data = new byte[length];
            in.readFully(data);
            messages = SerializationUtils.deserialize(data);
        } catch (IOException e) {
            ChatClient.alertError("Failed to receive data: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return messages;
    }

    public void start(String username) {
        try {
            byte[] data = SerializationUtils.serialize(username);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
            int ready = in.read();
            if (ready == 0) {
                ChatClient.alertError("Username is already taken");
                return;
            }
            inboundThread.start();
        } catch (IOException e) {
            ChatClient.alertError("Failed to start client: " + e.getMessage());
        }
    }

    private void inboundLoop() {
        try {
            while (true) {
                int length = in.readInt();
                byte[] data = new byte[length];
                in.readFully(data);
                Message message = SerializationUtils.deserialize(data);
                msgHandler.accept(message);
            }
        } catch (IOException e) {
            ChatClient.alertError("Failed to receive data: " + e.getMessage());
        }
    }
}
