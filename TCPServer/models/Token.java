package TCPServer.models;

public class Token {
    private String uuid;
    private String username;
    private String room = null;

    public Token(String uuid, String username, String room) {
        this.uuid = uuid;
        this.username = username;
        this.room = room;
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

    public void setRoom(String room) {
        this.room = room;
    }
    @Override
    public String toString() {
        return "Token{" +
                "uuid='" + uuid + '\'' +
                ", username='" + username + '\'' +
                ", room='" + room + '\'' +
                '}';
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Token token = (Token) obj;

        if (!uuid.equals(token.uuid)) return false;
        if (!username.equals(token.username)) return false;
        return room != null ? room.equals(token.room) : token.room == null;
    }
}
