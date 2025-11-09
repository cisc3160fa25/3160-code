package ch04;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    static boolean hadError = false;

    static void main(String[] args) throws IOException {
        if (args.length > 1) {
            IO.println("Usage: java Lox.java [script]");
            System.exit(1);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    static void runFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        String fileContent = Files.readString(path);
        run(fileContent);

        if (hadError) {
            System.exit(1);
        }
    }

    static void runPrompt() {
        IO.println("Lox REPL. Enter empty line to exit.");
        String line = IO.readln("> ");

        while (!line.isEmpty()) {
            run(line);

            hadError = false;
            line = IO.readln("> ");
        }
    }

    static void run(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        // For now, just print the tokens.
        for (Token token : tokens) {
            IO.println(token);
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}