import lexer.LexerAnalyzer;
import lexer.Token;
import lexer.TokenType;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Compiler {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(
                Path.of("testfile.txt"),
                StandardCharsets.UTF_8
        );

        LexerAnalyzer analyzer = new LexerAnalyzer(source);
        List<Token> tokens = analyzer.analyze();

        try (
                BufferedWriter lexerWriter = Files.newBufferedWriter(
                        Path.of("lexer.txt"), StandardCharsets.UTF_8
                );
                BufferedWriter errorWriter = Files.newBufferedWriter(
                        Path.of("error.txt"), StandardCharsets.UTF_8
                )
        ) {
            for (Token token : tokens) {
                if (token.getType() == TokenType.WRONGOP) {
                    errorWriter.write(token.getNum() + " a");
                    errorWriter.newLine();
                } else {
                    lexerWriter.write(
                            token.getType().name()
                                    + " " + token.getContent()
                    );
                    lexerWriter.newLine();
                }
            }
        }
    }
}
