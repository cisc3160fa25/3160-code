package ch07;

import java.util.Objects;

class Interpreter {
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            IO.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private Object evaluate(Expr expr) {
        return switch (expr) {
            case Grouping grouping -> visitGroupingExpression(grouping);
            case Literal literal -> visitLiteralExpression(literal);
            case Unary unary -> visitUnaryExpr(unary);
            case Binary binary -> visitBinaryExpr(binary);
        };
    }

    private Object visitLiteralExpression(Literal literal) {
        return literal.value();
    }

    private Object visitGroupingExpression(Grouping grouping) {
        return evaluate(grouping.expression());
    }

    private Object visitUnaryExpr(Unary unary) {
        Object right = evaluate(unary.right());

        return switch (unary.operator().type()) {
            case BANG -> !isTruthy(right);
            case MINUS -> {
                if (right instanceof Double d) {
                    yield -d;
                } else {
                    throw new RuntimeError(unary.operator(), "Operand must be a number.");
                }
            }
            default -> throw new AssertionError("Should be unreachable!");
        };
    }

    private Object visitBinaryExpr(Binary binary) {
        Object left = evaluate(binary.left());
        Object right = evaluate(binary.right());

        return switch (binary.operator().type()) {
            case EQUAL_EQUAL -> Objects.equals(left, right);
            case BANG_EQUAL -> !Objects.equals(left, right);
            case PLUS -> {
                if (left instanceof Double d1 && right instanceof Double d2) {
                    yield d1 + d2;
                } else if (left instanceof String s1 && right instanceof String s2) {
                    yield s1 + s2;
                } else {
                    throw new RuntimeError(binary.operator(), "Operands must be two numbers or two strings.");
                }
            }
            case MINUS -> {
                if (left instanceof Double d1 && right instanceof Double d2) {
                    yield d1 - d2;
                } else {
                    throw new RuntimeError(binary.operator(), "Operands must be numbers.");
                }
            }
            case STAR -> {
                if (left instanceof Double d1 && right instanceof Double d2) {
                    yield d1 * d2;
                } else {
                    throw new RuntimeError(binary.operator(), "Operands must be numbers.");
                }
            }
            case SLASH -> {
                if (left instanceof Double d1 && right instanceof Double d2) {
                    yield d1 / d2;
                } else {
                    throw new RuntimeError(binary.operator(), "Operands must be numbers.");
                }
            }
            case GREATER -> {
                if (left instanceof Double d1 && right instanceof Double d2) {
                    yield d1 > d2;
                } else {
                    throw new RuntimeError(binary.operator(), "Operands must be numbers.");
                }
            }
            case GREATER_EQUAL -> {
                if (left instanceof Double d1 && right instanceof Double d2) {
                    yield d1 >= d2;
                } else {
                    throw new RuntimeError(binary.operator(), "Operands must be numbers.");
                }
            }
            case LESS -> {
                if (left instanceof Double d1 && right instanceof Double d2) {
                    yield d1 < d2;
                } else {
                    throw new RuntimeError(binary.operator(), "Operands must be numbers.");
                }
            }
            case LESS_EQUAL -> {
                if (left instanceof Double d1 && right instanceof Double d2) {
                    yield d1 <= d2;
                } else {
                    throw new RuntimeError(binary.operator(), "Operands must be numbers.");
                }
            }
            default -> throw new AssertionError("Should be unreachable!");
        };
    }

    private boolean isTruthy(Object object) {
        return switch (object) {
            case null -> false;
            case Boolean b -> b;
            default -> true;
        };
    }

    private String stringify(Object object) {
        return switch (object) {
            case null -> "nil";
            case Double d -> {
                String text = d.toString();

                if (text.endsWith(".0")) {
                    yield text.substring(0, text.length() - 2);
                } else {
                    yield text;
                }
            }
            default -> object.toString();
        };
    }
}
