package lexer;

public class Token {
    private final TokenType type;
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
}