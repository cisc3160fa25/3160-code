package ch06;

import java.util.Objects;

class AstStringifier {
    String stringify(Expr expr) {
        return switch (expr) {
            case Binary(Expr left, Token operator, Expr right) -> "(" + operator.lexeme() + " " + stringify(left) + " " + stringify(right) + ")";
            case Grouping(Expr expression) -> "(group " + stringify(expression) + ")";
            case Literal(Object value) -> Objects.toString(value, "nil");
            case Unary(Token operator, Expr right) -> "(" + operator.lexeme() + " " + stringify(right) + ")";
        };
    }
}
