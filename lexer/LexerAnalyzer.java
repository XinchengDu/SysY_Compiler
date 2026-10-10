package lexer;

import java.util.ArrayList;
import java.util.List;

public class LexerAnalyzer {
    private final String source; //源程序字符串

    private int index = 0;

    private int lineNum = 1;

    private int length;

    private char current;

    private final List<Token> tokens = new ArrayList<>();

    public LexerAnalyzer(String source) {
        this.source = source;
    }

    public List<Token> analyze() {
        tokens.clear();
        index = 0;
        lineNum = 1;
        length = source.length();

        while (index < length) {
            current = source.charAt(index);

            if (current == '\n') {
                lineNum++;
                index++;
            }

            else if (current == ' ' || current == '\t' || current == '\r' || current == '\f') {
                index++;
            }

            else if (isIdentifierStart(current)) {
                scanIdentifier();
            }

            else if (isDigit(current)) {
                scanDigit();
            }

            else if (isOperator(current)) {
                scanOperater();
            }

            else if (isBracket(current)) {
                int start = index;
                index++;
                String text = source.substring(start, index);
                tokens.add(new Token(getBrackType(current), text, lineNum));
            }

            else if (current == ':') {
                int start = index;
                index++;
                String text = source.substring(start, index);
                tokens.add(new Token(TokenType.COLON, text, lineNum));
            }

            else if (current == ';') {
                int start = index;
                index++;
                String text = source.substring(start, index);
                tokens.add(new Token(TokenType.SEMICN, text, lineNum));
            }

            else if (current == ',') {
                int start = index;
                index++;
                String text = source.substring(start, index);
                tokens.add(new Token(TokenType.COMMA, text, lineNum));
            }

            else if (current == '\'') {
                scanCharCon();
            }

            else if (current == '\"') {
                scanStringCon();
            }

        }
        
        return tokens;
    }

    private TokenType getBrackType(char c) {
        switch (c) {
            case '(': return TokenType.LPARENT;
            case ')': return TokenType.RPARENT;
            case '[': return TokenType.LBRACK;
            case ']': return TokenType.RBRACK;
            case '{': return TokenType.LBRACE;
            default: return TokenType.RBRACE;
        }
    }

    private void scanIdentifier() {
        int start = index;
        while (index < length && isIdentifierPart((source.charAt(index)))) {
            index++;
        }

        String text = source.substring(start, index);
        TokenType type = getIdentifierType(text);

        tokens.add(new Token(type, text, lineNum));
    }

    private TokenType getIdentifierType(String text) {
        switch (text) {
            case "const": return TokenType.CONSTTK;
            case "int": return TokenType.INTTK;
            case "char": return TokenType.CHARTK;
            case "void": return TokenType.VOIDTK;
            case "main": return TokenType.MAINTK;
            case "if": return TokenType.IFTK;
            case "else": return TokenType.ELSETK;
            case "while": return TokenType.WHILETK;
            case "switch": return TokenType.SWITCHTK;
            case "case": return TokenType.CASETK;
            case "default": return TokenType.DEFAULTTK;
            case "break": return TokenType.BREAKTK;
            case "continue": return TokenType.CONTINUETK;
            case "return": return TokenType.RETURNTK;
            case "printf": return TokenType.PRINTFTK;
            case "static": return TokenType.STATICTK;

            default:
                return TokenType.IDENFR;
        }
    }

    private void scanDigit() {
        int start = index;
        while (index < length && isDigit((source.charAt(index)))) {
            index++;
        }

        String text = source.substring(start, index);
        tokens.add(new Token(TokenType.INTCON, text, lineNum));
    }

    private void scanOperater() {
        int start = index;
        index++;

        if (isOperator(source.charAt(index))) {
            index++;
        }

        String text = source.substring(start, index);
        TokenType type = getOperaterType(text);
        if (type != null) {
            tokens.add(new Token(type, text, lineNum));
            return;
        }

        else {
            index--;
        }

        text = source.substring(start, index);
        type = getOperaterType(text);
        tokens.add(new Token(type, text, lineNum));

    }

    private TokenType getOperaterType(String text) {
        switch (text) {
            case "!=": return TokenType.NEQ;
            case "<=": return TokenType.LEQ;
            case "==": return TokenType.EQL;
            case ">=": return TokenType.GEQ;
            case "<": return TokenType.LSS;
            case "=": return TokenType.ASSIGN;
            case ">": return TokenType.GRE;
            case "+": return TokenType.PLUS;
            case "-": return TokenType.MINU;
            case "*": return TokenType.MULT;
            case "/": return TokenType.DIV;
            case "||": return TokenType.OR;
            case "&&": return TokenType.AND;
            case "%": return TokenType.MOD;
            case "!": return TokenType.NOT;
            case "&": return TokenType.WRONGOPAND;
            case "|": return TokenType.WRONGOPOR;

            default:
                return null;
        }
    }

    private void scanCharCon() {
        int start = index;
        index++;
        while (index < length && source.charAt(index) != '\'') {
            if (source.charAt(index) == '\\') {
                index += 2;
            }
            //防止读入 “\'”
            else {
                index++;
            }
        }
        index++;

        String text = source.substring(start, index);
        tokens.add(new Token(TokenType.CHARCON, text, lineNum));
    }

    private void scanStringCon() {
        int start = index;
        index++;
        while (index < length && source.charAt(index) != '\"') {
            if (source.charAt(index) == '\\') {
                index += 2;
            }
            //防止读入 “\"”
            else {
                index++;
            }
        }
        index++;

        String text = source.substring(start, index);
        tokens.add(new Token(TokenType.STRCON, text, lineNum));
    }

    private boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isOperator(char c) {
        return "+-*/%!<>=&|".indexOf(c) >= 0;
    }

    private boolean isIdentifierStart(char c) {
        return isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    private boolean isBracket(char c) {
        return "()[]{}".indexOf(c) >= 0;
    }
}
