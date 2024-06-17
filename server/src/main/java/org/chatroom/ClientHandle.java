package org.chatroom;

import org.apache.commons.lang3.SerializationUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.Date;

public class ClientHandle implements Runnable {
    private final Socket client;
    private final DataOutputStream messageOut;

    public ClientHandle(Socket client, DataOutputStream messageOut) {
        this.client = client;
        this.messageOut = messageOut;
    }

    @Override
    public void run() {
        String username;
        DataInputStream in;
        try {
            in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            Date lastSyncTime = new Date(in.readLong());
            byte[] messages = SerializationUtils.serialize((Serializable) ChatServer.loadMessages(lastSyncTime));
            out.writeInt(messages.length);
            out.write(messages);
            out.flush();

            int length = in.readInt();
            byte[] data = new byte[length];
            in.readFully(data);
            username = (String) SerializationUtils.deserialize(data);
            if (!ChatServer.registerClient(username, out)) {
                out.write(0);
                out.flush();
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to handle client: " + e.getMessage());
            return;
        }

        try {
            while (true) {
                int length = in.readInt();
                byte[] data = new byte[length];
                in.readFully(data);
                synchronized (messageOut) {
                    messageOut.writeInt(data.length);
                    messageOut.write(data);
                }
            }
        } catch (IOException e) {
            if (!e.getMessage().equals("Connection reset")) {
                System.err.println("Failed to read message: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Failed handle client: " + e.getMessage());
        } finally {
            ChatServer.unregisterClient(username);
        }
    }
}
