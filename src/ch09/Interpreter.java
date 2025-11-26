package ch09;

import java.util.List;
import java.util.Objects;

class Interpreter {
    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        switch (stmt) {
            case Expression expressionStmt -> visitExpressionStmt(expressionStmt);
            case Print printStmt -> visitPrintStmt(printStmt);
            case Var varStmt -> visitVarStmt(varStmt);
            case Block blockStmt -> visitBlockStmt(blockStmt);
            case If ifStmt -> visitIfStmt(ifStmt);
            case While whileStmt -> visitWhileStmt(whileStmt);
        }
    }

    private void visitIfStmt(If ifStmt) {
        if (isTruthy(evaluate(ifStmt.condition()))) {
            execute(ifStmt.thenBranch());
        } else if (ifStmt.elseBranch() != null) {
            execute(ifStmt.elseBranch());
        }
    }

    private void visitWhileStmt(While whileStmt) {
        while (isTruthy(evaluate(whileStmt.condition()))) {
            execute(whileStmt.body());
        }
    }

    private void visitExpressionStmt(Expression expressionStmt) {
        evaluate(expressionStmt.expression());
    }

    private void visitPrintStmt(Print printStmt) {
        Object value = evaluate(printStmt.expression());
        IO.println(stringify(value));
    }

    private void visitVarStmt(Var varStmt) {
        Object value = varStmt.initializer() == null ? null : evaluate(varStmt.initializer());
        environment.define(varStmt.name().lexeme(), value);
    }

    private void visitBlockStmt(Block blockStmt) {
        executeBlock(blockStmt.statements(), new Environment(environment));
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;

        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private Object evaluate(Expr expr) {
        return switch (expr) {
            case Grouping grouping -> visitGroupingExpr(grouping);
            case Literal literal -> visitLiteralExpr(literal);
            case Unary unary -> visitUnaryExpr(unary);
            case Binary binary -> visitBinaryExpr(binary);
            case Variable variable -> visitVariableExpr(variable);
            case Assign assign -> visitAssignExpr(assign);
            case Logical logical -> visitLogicalExpr(logical);
        };
    }

    private Object visitLogicalExpr(Logical logical) {
        Object left = evaluate(logical.left());

        if (logical.operator().type() == TokenType.OR) {
            if (isTruthy(left)) {
                return left;
            }
        } else { // logical.operator().type() == TokenType.AND
            if (!isTruthy(left)) {
                return left;
            }
        }

        return evaluate(logical.right());
    }

    private Object visitLiteralExpr(Literal literal) {
        return literal.value();
    }

    private Object visitGroupingExpr(Grouping grouping) {
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

    private Object visitVariableExpr(Variable variable) {
        return environment.lookup(variable.name());
    }

    private Object visitAssignExpr(Assign assignExpr) {
        Object value = evaluate(assignExpr.value());
        environment.assign(assignExpr.name(), value);
        return value;
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
