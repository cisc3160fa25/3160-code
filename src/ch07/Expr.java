package ch07;

// no changes from the previous chapter

sealed interface Expr {}

record Binary(Expr left, Token operator, Expr right) implements Expr {}

record Grouping(Expr expression) implements Expr {}

record Literal(Object value) implements Expr {}

record Unary(Token operator, Expr right) implements Expr {}