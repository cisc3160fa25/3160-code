package ch09;

// no changes from previous chapter

record Token(TokenType type, String lexeme, Object literal, int line) {
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
