package parser;
import lexer.*;
import static lexer.TokenType.*;

import java.util.ArrayList;
import java.util.List;

public class PaserContext {
    public static List<Token> tokens = new ArrayList<>();
    public static int index = 0;
    public static int length;
    public static List<String> output = new ArrayList<>();

    public static List<Token> getTokens() {
        return tokens;
    }

    public static Token getCurToken() {
        return tokens.get(index);
    }

    public static Token getFollowToken(int num) {
        if (index + num < length) {
            return tokens.get(index + num);
        }
        return new Token(INDEXOUT, null, -1);
    }

    public static int getIndex() {
        return index;
    }
}
