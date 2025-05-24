package TCPServer.lobby;

import java.util.List;
import TCPServer.models.Token;
import java.util.concurrent.locks.ReentrantLock;

public class TokenManager {
    List<Token> tokens = new java.util.ArrayList<>();
    private final ReentrantLock tokensLock = new ReentrantLock();

    public void addToken(Token token) {
        tokensLock.lock();
        try {
            tokens.add(token);
        } finally {
            tokensLock.unlock();
        }
    }

    public void removeToken(Token token) {
        tokensLock.lock();
        try {
            tokens.remove(token);
        } finally {
            tokensLock.unlock();
        }
    }

    public void updateTokenRoom(String token, String room) {
        for (Token t : tokens) {
            if (t.getUuid().equals(token)) {
                tokensLock.lock();
                try {
                    t.setRoom(room);
                } finally {
                    tokensLock.unlock();
                }
                return;
            }
        }
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
