package ir;

import java.util.ArrayList;
import java.util.List;

public class IrModule {
    private final List<IrGlobalVariable> globalVariables = new ArrayList<>();
    private final List<IrFunction> functions = new ArrayList<>();

    public void addGlobalVariable(IrGlobalVariable variable) {
        globalVariables.add(variable);
    }

    public void addFunction(IrFunction function) {
        functions.add(function);
    }

    public List<IrGlobalVariable> getGlobalVariables() {
        return globalVariables;
    }

    public List<IrFunction> getFunctions() {
        return functions;
    }

    @Override 
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (IrGlobalVariable variable : globalVariables) {
            result.append(variable).append("\n");
        }

        for (IrFunction function : functions) {
            result.append(function).append("\n");
        }

        return result.toString();
    }
}
