package ch09;

// no changes from previous chapter

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ch09.TokenType.*;
import static java.util.Map.entry;

class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;   // the index of the first character in the current lexeme being scanned
    private int current = 0; // the index of the current character being considered
    private int line = 1;    // the line we are on

    private static final Map<String, TokenType> keywords = Map.ofEntries(
            entry("and",    AND),
            entry("class",  CLASS),
            entry("else",   ELSE),
            entry("false",  FALSE),
            entry("for",    FOR),
            entry("fun",    FUN),
            entry("if",     IF),
            entry("nil",    NIL),
            entry("or",     OR),
            entry("print",  PRINT),
            entry("return", RETURN),
            entry("super",  SUPER),
            entry("this",   THIS),
            entry("true",   TRUE),
            entry("var",    VAR),
            entry("while",  WHILE)
    );

    Lexer(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            // single-character tokens
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case ',' -> addToken(COMMA);
            case '.' -> addToken(DOT);
            case '-' -> addToken(MINUS);
            case '+' -> addToken(PLUS);
            case ';' -> addToken(SEMICOLON);
            case '*' -> addToken(STAR);

            // one- or two-character tokens
            case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
            case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
            case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);

            // slash or comment
            case '/' -> {
                if (match('/')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else {
                    addToken(SLASH);
                }
            }

            // whitespace and newline
            case ' ', '\r', '\t' -> {
                // Ignore whitespace.
            }
            case '\n' -> line++;

            // strings
            case '"' -> string();

            // numbers, keywords, identifiers, unexpected
            default  -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    keywordOrIdentifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
            }
        }
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }

            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // The closing ".
        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void keywordOrIdentifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }

        String text = source.substring(start, current);
        TokenType type = keywords.getOrDefault(text, IDENTIFIER);
        addToken(type);
    }

    /**
     * Determines whether we have reached the end of the source.
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Returns the current character and advances to the next character.
     */
    private char advance() {
        return source.charAt(current++);
    }

    /**
     * Returns the current character without advancing to the next one.
     * Returns '\0' if we have reached the end of the source.
     */
    private char peek() {
        if (isAtEnd()) {
            return '\0';
        } else {
            return source.charAt(current);
        }
    }

    /**
     * Returns the next character without advancing.
     * Returns '\0' if there is no next character.
     */
    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        } else {
            return source.charAt(current + 1);
        }
    }

    /**
     * Advances and returns true if the current character equals the expected character.
     * Otherwise, returns false and does not advance.
     */
    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        } else if (source.charAt(current) != expected) {
            return false;
        } else {
            current++;
            return true;
        }
    }

    /**
     * Determines whether the specified character is a digit.
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Determines whether the specified character is a letter or _.
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /**
     * Determines whether the specified character is a digit, letter, or _.
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Adds a token to the list without a meaningful value.
     */
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Adds a token to the list for the current lexeme, which
     * is from index start until (but not including) index current.
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}