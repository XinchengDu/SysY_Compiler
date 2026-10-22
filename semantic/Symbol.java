package semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Symbol {
    public enum Kind {
        VARIABLE, CONSTANT, PARAMETER, FUNCTION
    }

    public enum Type {
        INT, CHAR, INT_ARRAY, CHAR_ARRAY, VOID
    }

    private final String name;
    private final int lineNum;
    private final Kind kind;
    private final Type type;
    private final boolean isStatic;
    private final List<Type> parameterTypes = new ArrayList<>(); // 函数参数

    public Symbol(String name, int lineNum, Kind kind, Type type) {
        this(name, lineNum, kind, type, false);
    }

    public Symbol(String name, int lineNum, Kind kind, Type type, boolean isStatic) {
        this.name = name;
        this.lineNum = lineNum;
        this.kind = kind;
        this.type = type;
        this.isStatic = isStatic;
    }

    public String getName() {
        return name;
    }

    public int getLineNum() {
        return lineNum;
    }

    public Kind getKind() {
        return kind;
    }

    public Type getType() {
        return type;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isConst() {
        return kind == Kind.CONSTANT;
    }

    public boolean isFunction() {
        return kind == Kind.FUNCTION;
    }

    public void setParameterTypes(List<Type> types) {
        List<Type> copy = new ArrayList<>(types);
        parameterTypes.clear();
        parameterTypes.addAll(copy);
    }

    public List<Type> getParameterTypes() {
        return Collections.unmodifiableList(parameterTypes);
    }

    public String getOutputTypeName() {
        if (isFunction()) {
            switch (type) {
                case INT:
                    return "IntFunc";
                case CHAR:
                    return "CharFunc";
                case VOID:
                    return "VoidFunc";
                default:
                    throw new IllegalStateException("Invalid function return type: " + type);
            }
        }

        String prefix = isConst() ? "Const" : (isStatic ? "Static" : "");
        switch (type) {
            case INT:
                return prefix + "Int";
            case CHAR:
                return prefix + "Char";
            case INT_ARRAY:
                return prefix + "IntArray";
            case CHAR_ARRAY:
                return prefix + "CharArray";
            default:
                throw new IllegalStateException("Invalid variable type: " + type);
        }
    }
}
