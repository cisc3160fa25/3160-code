package ch10;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Function declaration;

    LoxFunction(Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.params().size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(interpreter.globals);

        for (int i = 0; i < declaration.params().size(); i++) {
            environment.define(
                    declaration.params().get(i).lexeme(),
                    arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body(), environment);
        } catch (Return returnObject) {
            return returnObject.value;
        }

        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name().lexeme() + ">";
    }
}
