# Online Chat Room

> My Java Final Homework

This is a simple online chat room application that allows users to chat with each other in real-time. The application is built using Java.

## Features

- support text and image messages
- store chat history in both server and client
- auto sync chat history when client reconnects
- support multiple clients

## Used Tools

- Client: JavaFX, sqlite3, JDBC
- Server: PostgreSQL, JDBC
- Commons: Maven, Apache Commons Lang3

## Configuration

Server DB script:
```sql
CREATE TABLE IF NOT EXISTS text_msg (
    id SERIAL PRIMARY KEY,
    username TEXT NOT NULL,
    date BIGINT NOT NULL,
    text TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS image_msg (
    id SERIAL PRIMARY KEY,
    username TEXT NOT NULL,
    date BIGINT NOT NULL,
    image BYTEA NOT NULL
);
```

Environment variables:
- Client: `SERVER_HOST`, `SERVER_PORT`
- Server: `SERVER_PORT`, `DB_URL`
