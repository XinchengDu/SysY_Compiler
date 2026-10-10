package parser;
import lexer.*;
import static parser.PaserContext.*;

import java.util.List;

public class Parser {
    public final List<Token> tokens;
    public AstNode root;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        PaserContext.tokens = tokens;
        PaserContext.length = tokens.size();
    }

    public List<String> analyze() {
        ParseRules rules = new ParseRules();
        root = rules.parseCompUnit();
        return output;
    }

    public AstNode getRoot() {
        return root;
    }
}
