import lexer.LexerAnalyzer;
import lexer.Token;
import lexer.TokenType;

import parser.*;

import static lexer.TokenType.AND;
import static lexer.TokenType.OR;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Compiler {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(
                Path.of("testfile.txt"),
                StandardCharsets.UTF_8
        );

        List<String> errors = new ArrayList<>();

        // 词法分析
        LexerAnalyzer analyzer = new LexerAnalyzer(source);
        List<Token> tokens = analyzer.analyze();
        collectLexerError(tokens, errors);

        // 语法分析
        Parser parser = new Parser(tokens);
        List<String> output = parser.analyze();
        AstNode root = parser.getRoot();
        collectSyntaxError(root, errors);
        
        sortErrors(errors);

        try (
                BufferedWriter lexerWriter = Files.newBufferedWriter(
                        Path.of("lexer.txt"), StandardCharsets.UTF_8
                );
                BufferedWriter parserWriter = Files.newBufferedWriter(
                        Path.of("parser.txt"), StandardCharsets.UTF_8
                );
                BufferedWriter errorWriter = Files.newBufferedWriter(
                        Path.of("error.txt"), StandardCharsets.UTF_8
                )
        ) {
            // 词法输出
            for (Token token : tokens) {
                lexerWriter.write(
                        token.getType().name()
                                + " " + token.getContent()
                );
                lexerWriter.newLine();
            }

            // 语法输出
            for (String line : output) {
                parserWriter.write(line);
                parserWriter.newLine();
            }

            // 错误输出
            for (String error : errors) {
                errorWriter.write(error);
                errorWriter.newLine();
            }
        }
    }

    private static void sortErrors(List<String> errors) {
        errors.sort((first, second) -> {
            int firstLine = Integer.parseInt(first.split(" ")[0]);
            int secondLine = Integer.parseInt(second.split(" ")[0]);
            return Integer.compare(firstLine, secondLine);
        });
    }

    private static void collectLexerError(List<Token> tokens, List<String> errors) {
        for (Token token : tokens) {
            if (token.getType() == TokenType.WRONGOPAND) {
                errors.add(token.getNum() + " a");
                token.setType(AND);
            }

            if (token.getType() == TokenType.WRONGOPOR) {
                errors.add(token.getNum() + " a");
                token.setType(OR);
            }
        }
    }

    private static void collectSyntaxError(AstNode node, List<String> errors) {
        if (node.isIError) {
            errors.add(node.token.getNum() + " i");
        }

        if (node.isJError) {
            errors.add(node.token.getNum() + " j");
        }

        if (node.isKError) {
            errors.add(node.token.getNum() + " k");
        }

        for (AstNode child : node.children) {
            collectSyntaxError(child, errors);
        }
    }

    
}
