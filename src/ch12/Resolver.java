package ch12;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver {
    private enum FunctionType {
        NONE,
        FUNCTION,
        METHOD,
        INITIALIZER
    }

    private enum ClassType {
        NONE,
        CLASS
    }

    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    // The Boolean value associated with each variable name represents
    // whether we have finished resolving that variable’s initializer.
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    // ----------------------------- resolving statements -----------------------------

    private void resolve(Stmt stmt) {
        switch (stmt) {
            case Block blockStmt -> visitBlockStmt(blockStmt);
            case Expression expressionStmt -> visitExpressionStmt(expressionStmt);
            case Function functionStmt -> visitFunctionStmt(functionStmt);
            case If ifStmt -> visitIfStmt(ifStmt);
            case Print printStmt -> visitPrintStmt(printStmt);
            case ReturnStmt returnStmt -> visitReturnStmt(returnStmt);
            case Var varStmt -> visitVarStmt(varStmt);
            case While whileStmt -> visitWhileStmt(whileStmt);
            case ClassStmt classStmt -> visitClassStmt(classStmt);
        }
    }

    private void visitBlockStmt(Block blockStmt) {
        beginScope();
        resolve(blockStmt.statements());
        endScope();
    }

    private void visitExpressionStmt(Expression expressionStmt) {
        resolve(expressionStmt.expression());
    }

    private void visitFunctionStmt(Function functionStmt) {
        declare(functionStmt.name());
        define(functionStmt.name());
        // We define the name eagerly, before resolving the function’s body.
        // This lets a function recursively refer to itself inside its own body.
        resolveFunction(functionStmt, FunctionType.FUNCTION);
    }

    private void visitIfStmt(If ifStmt) {
        resolve(ifStmt.condition());
        resolve(ifStmt.thenBranch());

        if (ifStmt.elseBranch() != null) {
            resolve(ifStmt.elseBranch());
        }
    }

    private void visitPrintStmt(Print printStmt) {
        resolve(printStmt.expression());
    }

    private void visitReturnStmt(ReturnStmt returnStmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(returnStmt.keyword(), "Can't return from top-level code.");
        }

        if (returnStmt.value() != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(returnStmt.keyword(), "Can't return a value from an initializer.");
            }

            resolve(returnStmt.value());
        }
    }

    private void visitVarStmt(Var varStmt) {
        declare(varStmt.name());

        if (varStmt.initializer() != null) {
            resolve(varStmt.initializer());
        }

        define(varStmt.name());
    }

    private void visitWhileStmt(While whileStmt) {
        resolve(whileStmt.condition());
        resolve(whileStmt.body());
    }

    private void visitClassStmt(ClassStmt classStmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;
        declare(classStmt.name());
        define(classStmt.name());
        beginScope();
        scopes.peek().put("this", true);

        for (Function method : classStmt.methods()) {
            FunctionType declaration = method.name().lexeme().equals("init") ? FunctionType.INITIALIZER : FunctionType.METHOD;
            resolveFunction(method, declaration);
        }

        endScope();
        currentClass = enclosingClass;
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    // Declaration adds the name to the innermost scope so that it shadows any outer one
    // and so that we know the name exists. We mark it as false, i.e., “not ready yet”.
    private void declare(Token name) {
        if (!scopes.isEmpty()) {
            Map<String, Boolean> scope = scopes.peek();

            if (scope.containsKey(name.lexeme())) {
                Lox.error(name, "Already a variable with this name in this scope.");
            }

            scope.put(name.lexeme(), false);
        }
    }

    private void define(Token name) {
        if (!scopes.isEmpty()) {
            scopes.peek().put(name.lexeme(), true);
        }
    }

    private void resolveFunction(Function functionStmt, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();

        for (Token param : functionStmt.params()) {
            declare(param);
            define(param);
        }

        resolve(functionStmt.body());
        endScope();

        currentFunction = enclosingFunction;
    }


    // ----------------------------- resolving expressions -----------------------------

    private void resolve(Expr expr) {
        switch (expr) {
            case Assign assignExpr -> visitAssignExpr(assignExpr);
            case Binary binaryExpr -> visitBinaryExpr(binaryExpr);
            case Call callExpr -> visitCallExpr(callExpr);
            case Grouping groupingExpr -> visitGroupingExpr(groupingExpr);
            case Literal literalExpr -> visitLiteralExpr(literalExpr);
            case Logical logicalExpr -> visitLogicalExpr(logicalExpr);
            case Unary unaryExpr -> visitUnaryExpr(unaryExpr);
            case Variable variableExpr -> visitVariableExpr(variableExpr);
            case Get getExpr -> visitGetExpr(getExpr);
            case Set setExpr -> visitSetExpr(setExpr);
            case This thisExpr -> visitThisExpr(thisExpr);
        }
    }

    private void visitGetExpr(Get getExpr) {
        resolve(getExpr.object());
    }

    private void visitSetExpr(Set setExpr) {
        resolve(setExpr.value());
        resolve(setExpr.object());
    }

    private void visitThisExpr(This thisExpr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(thisExpr.keyword(), "Can't use 'this' outside of a class.");
        } else {
            resolveLocal(thisExpr, thisExpr.keyword());
        }
    }

    private void visitVariableExpr(Variable variableExpr) {
        // check if we've declared the variable but not yet defined it
        if (!scopes.isEmpty() && scopes.peek().get(variableExpr.name().lexeme()) == Boolean.FALSE) {
            Lox.error(variableExpr.name(), "Can't read local variable in its own initializer.");
        }

        resolveLocal(variableExpr, variableExpr.name());
    }

    private void visitAssignExpr(Assign assignExpr) {
        resolve(assignExpr.value());
        resolveLocal(assignExpr, assignExpr.name());
    }

    private void visitBinaryExpr(Binary binaryExpr) {
        resolve(binaryExpr.left());
        resolve(binaryExpr.right());
    }

    private void visitCallExpr(Call callExpr) {
        resolve(callExpr.callee());

        for (Expr argument : callExpr.arguments()) {
            resolve(argument);
        }
    }

    private void visitGroupingExpr(Grouping groupingExpr) {
        resolve(groupingExpr.expression());
    }

    private void visitLiteralExpr(Literal literalExpr) {
        // nothing to resolve, since no variables or subexpressions
    }

    private void visitLogicalExpr(Logical logicalExpr) {
        resolve(logicalExpr.left());
        resolve(logicalExpr.right());
    }

    private void visitUnaryExpr(Unary unaryExpr) {
        resolve(unaryExpr.right());
    }

    private void resolveLocal(Expr expr, Token name) {
        /*
        We start at the innermost scope and work outwards, looking in each map
        for a matching name. If we find the variable, we resolve it, passing in
        the number of scopes between the current innermost scope and the scope
        where the variable was found. So, if the variable was found in the current
        scope, we pass in 0. If it’s in the immediately enclosing scope, 1. Etc.
        If we walk through all the block scopes and never find the variable, we
        leave it unresolved and assume it’s global.
        */
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme())) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }
}
