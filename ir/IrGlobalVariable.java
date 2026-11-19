package ir;

import java.util.ArrayList;
import java.util.List;

public class IrGlobalVariable extends IrValue {
    private final IrType objectType;
    private final boolean constant;
    private final List<IrConstant> initialValues = new ArrayList<>();

    public IrGlobalVariable(String name, IrType objectType, boolean constant, List<IrConstant> values) {
        super(IrType.pointer(objectType), "@" + name);
        this.objectType = objectType;
        this.constant = constant;

        boolean isArray = objectType.getKind() == IrType.Kind.ARRAY;

        IrType elementType = isArray ? objectType.getElementType() : objectType;

        int count = isArray ? objectType.getLength() : 1;

        if (!elementType.equals(IrType.I32) && !elementType.equals(IrType.I8)) {
            throw new IllegalArgumentException("全局对象必须是整数、字符或其一维数组");
        }

        if (values.size() > count) {
            throw new IllegalArgumentException("初值数量超过存储容量");
        }
        
        for (IrConstant value : values) {
            if (!value.getType().equals(elementType)) {
                throw new IllegalArgumentException("初值类型与存储类型不一致");
            }

            initialValues.add(value);
        }

        while (initialValues.size() < count) {
            initialValues.add(new IrConstant(elementType, 0));
        }
    }

    public IrType getObjectType() {
        return objectType;
    }

    public boolean isConstant() {
        return constant;
    }

    public List<IrConstant> getInitialValues() {
        return initialValues;
    }

    @Override 
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(asOperand())
            .append(" = ")
            .append(constant ? "constant " : "global ")
            .append(objectType)
            .append(" ");

        if (objectType.getKind() == IrType.Kind.ARRAY) {
            result.append("[");

            for (int i = 0; i < initialValues.size(); i++) {
                if (i > 0) {
                    result.append(", ");
                }

                result.append(initialValues.get(i));
            }

            result.append("]");
        } else {
            result.append(initialValues.get(0).asOperand());
        }

        return result.toString();
    }
}
