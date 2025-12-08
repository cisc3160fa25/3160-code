package ch12;

import java.util.List;

sealed interface Expr {}

record Binary(Expr left, Token operator, Expr right) implements Expr {}

record Grouping(Expr expression) implements Expr {}

record Literal(Object value) implements Expr {}

record Unary(Token operator, Expr right) implements Expr {}

record Variable(Token name) implements Expr {}

record Assign(Token name, Expr value) implements Expr {}

record Logical(Expr left, Token operator, Expr right) implements Expr {}

record Call(Expr callee, Token paren, List<Expr> arguments) implements Expr {}

record Get(Expr object, Token name) implements Expr {}

record Set(Expr object, Token name, Expr value) implements Expr {}

record This(Token keyword) implements Expr {}