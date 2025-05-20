package TCPServer.lobby;

import java.util.List;
import TCPServer.models.Token;

public class TokenManager {
    List<Token> tokens = new java.util.ArrayList<>();
    public void addToken(Token token) {
        tokens.add(token);
    }
    public void removeToken(Token token) {
        tokens.remove(token);
    }
    public List<Token> getTokens() {
        return tokens;
    }
    public Token getToken(String uuid) {
        for (Token token : tokens) {
            if (token.getUuid().equals(uuid)) {
                return token;
            }
        }
        return null;
    }
}
