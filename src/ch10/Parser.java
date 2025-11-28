package ch10;

import java.util.ArrayList;
import java.util.List;

import static ch10.TokenType.*;

/*
program        → declaration* EOF ;
declaration    → funDecl | varDecl | statement ;
funDecl        → "fun" function ;
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
statement      → exprStmt | printStmt | block | ifStmt | whileStmt | forStmt | returnStmt ;
exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
block          → "{" declaration* "}" ;
ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
whileStmt      → "while" "(" expression ")" statement ;
forStmt        → "for" "("
                 ( varDecl | exprStmt | ";" )
                 expression? ";"
                 expression? ")"
                 statement ;
returnStmt     → "return" expression? ";" ;

expression     → assignment ;
assignment     → IDENTIFIER "=" assignment
               | logic_or ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | call ;
call           → primary ( "(" arguments? ")" )* ;
arguments      → expression ( "," expression )* ;
primary        → NUMBER | STRING
               | "true" | "false" | "nil"
               | "(" expression ")"
               | IDENTIFIER ;
 */

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // program        → declaration* EOF ;
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();

        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    // declaration    → funDecl | varDecl | statement ;
    // funDecl        → "fun" function ;
    private Stmt declaration() {
        try {
            if (match(FUN)) {
                return function("function");
            } else if (match(VAR)) {
                return varDeclaration();
            } else {
                return statement();
            }
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    // function       → IDENTIFIER "(" parameters? ")" block ;
    // parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
    private Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                // done in book, but not necessary:
                // if (parameters.size() >= 255) {
                //     error(peek(), "Can't have more than 255 parameters.");
                // }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Function(name, parameters, body);
    }

    // varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = match(EQUAL) ? expression() : null;
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Var(name, initializer);
    }

    // statement      → exprStmt | printStmt | block | ifStmt | whileStmt | forStmt | returnStmt ;
    private Stmt statement() {
        if (match(IF)) {
            return ifStatement();
        } else if (match(WHILE)) {
            return whileStatement();
        } else if (match(FOR)) {
            return forStatement();
        } else if (match(PRINT)) {
            return printStatement();
        } else if (match(LEFT_BRACE)) {
            return new Block(block());
        } else if (match(RETURN)) {
            return returnStatement();
        } else {
            return expressionStatement();
        }
    }

    // returnStmt     → "return" expression? ";" ;
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = check(SEMICOLON) ? null : expression();
        consume(SEMICOLON, "Expect ';' after return value.");
        return new ReturnStmt(keyword, value);
    }

    // ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = match(ELSE) ? statement() : null;
        return new If(condition, thenBranch, elseBranch);
    }

    // whileStmt      → "while" "(" expression ")" statement ;
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();
        return new While(condition, body);
    }

    // forStmt        → "for" "("
    //                 ( varDecl | exprStmt | ";" )
    //                 expression? ";"
    //                 expression? ")"
    //                 statement ;
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = check(SEMICOLON) ? null : expression();
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr update = check(RIGHT_PAREN) ? null : expression();
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        // now desugar: turn the for loop into a while loop

        if (update != null) {
            body = new Block(List.of(body, new Expression(update)));
        }

        if (condition == null) {
            body = new While(new Literal(true), body);
        } else {
            body = new While(condition, body);
        }

        if (initializer != null) {
            body = new Block(List.of(initializer, body));
        }

        return body;
    }

    // printStmt      → "print" expression ";"
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Print(value);
    }

    // exprStmt       → expression ";" ;
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Expression(expr);
    }

    // block          → "{" declaration* "}" ;
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    // expression     → assignment ;
    private Expr expression() {
        return assignment();
    }

    // assignment     → IDENTIFIER "=" assignment
    //                | logic_or ;
    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Variable(Token name)) {
                return new Assign(name, value);
            } else {
                // Report but don't throw an error.
                error(equals, "Invalid assignment target.");
            }
        }

        return expr;
    }

    // logic_or       → logic_and ( "or" logic_and )* ;
    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Logical(expr, operator, right);
        }

        return expr;
    }

    // logic_and      → equality ( "and" equality )* ;
    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Logical(expr, operator, right);
        }

        return expr;
    }

    // equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    // comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    // term           → factor ( ( "-" | "+" ) factor )* ;
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    // factor         → unary ( ( "/" | "*" ) unary )* ;
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    // unary          → ( "!" | "-" ) unary
    //                | call ;
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Unary(operator, right);
        } else {
            return call();
        }
    }

    // call           → primary ( "(" arguments? ")" )* ;
    // arguments      → expression ( "," expression )* ;
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                // in book, but not necessary:
                // if (arguments.size() >= 255) {
                //     error(peek(), "Can't have more than 255 arguments.");
                // }

                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Call(callee, paren, arguments);
    }

    // primary        → NUMBER | STRING | "true" | "false" | "nil"
    //                | "(" expression ")" | IDENTIFIER ;
    private Expr primary() {
        if (match(FALSE)) {
            return new Literal(false);
        } else if (match(TRUE)) {
            return new Literal(true);
        } else if (match(NIL)) {
            return new Literal(null);
        } else if (match(NUMBER, STRING)) {
            return new Literal(previous().literal());
        } else if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Grouping(expr);
        } else if (match(IDENTIFIER)) {
            return new Variable(previous());
        } else {
            throw error(peek(), "Expect expression.");
        }
    }

    /**
     * Checks if the current token has any of the given types.
     * If so, consumes the token and returns true.
     * Otherwise, returns false and leaves the current token alone.
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether the current token is of the given type.
     */
    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type() == type;
    }

    /**
     * Consumes the current token and returns it.
     */
    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }

        return previous();
    }

    /**
     * Determines whether we've run out of tokens to parse.
     */
    private boolean isAtEnd() {
        return peek().type() == EOF;
    }

    /**
     * Returns the current token without consuming it.
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Returns the most recently consumed token.
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        } else {
            throw error(peek(), message);
        }
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    /**
     * Discards tokens until it thinks it has found a statement boundary.
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type() == SEMICOLON) {
                return;
            }

            switch (peek().type()) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> {
                    return;
                }
            }

            advance();
        }
    }
}
