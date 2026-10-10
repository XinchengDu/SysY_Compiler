package lexer;

public class Token {
    private TokenType type;
    private final String content;
    private final int lineNum;

    public Token(TokenType type, String content, int lineNum) {
        this.type = type;
        this.content = content;
        this.lineNum = lineNum;
    }

    public TokenType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getNum() {
        return lineNum;
    }

    public void setType(TokenType type) {
        this.type = type;
    }
}