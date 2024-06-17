package org.chatroom;

import java.io.Serializable;
import java.util.Date;

public class Message implements Comparable<Message>, Serializable {
    public String username;
    public Date date;
    public Object content;

    @Override
    public int compareTo(Message other) {
        return date.compareTo(other.date);
    }
}
