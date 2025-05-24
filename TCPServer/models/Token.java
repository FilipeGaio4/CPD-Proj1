package TCPServer.models;

import java.time.LocalDateTime;

public class Token {
    private String uuid;
    private String username;
    private String room = null;
    private LocalDateTime date;

    public Token(String uuid, String username, String room, LocalDateTime date) {
        this.uuid = uuid;
        this.username = username;
        this.room = room;
        this.date = LocalDateTime.now().plusMinutes(20);
    }

    public String getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getRoom() {
        return room;
    }

    public LocalDateTime getdate() {return date;}

    public void setRoom(String room) {
        this.room = room;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    @Override
    public String toString() {
        return "Token{" +
                "uuid='" + uuid + '\'' +
                ", username='" + username + '\'' +
                ", room='" + room + '\'' +
                ", dateLimit=" + date +
                '}';
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Token token = (Token) obj;

        if (!uuid.equals(token.uuid)) return false;
        if (!username.equals(token.username)) return false;
        if (!date.equals(token.date)) return false;
        return room != null ? room.equals(token.room) : token.room == null;
    }
}
