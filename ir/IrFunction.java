package ir;

import java.util.ArrayList;
import java.util.List;

public class IrFunction {
    private final String name;
    private final IrType returnType;
    private final boolean declaration;

    private final List<IrValue> parameters = new ArrayList<>();
    private final List<IrBasicBlock> blocks = new ArrayList<>();

    public IrFunction(String name, IrType returnType) {
        this.name = name;
        this.returnType = returnType;
        this.declaration = false;
    }

    public IrFunction(String name, IrType returnType, boolean declaration) {
        this.name = name;
        this.returnType = returnType;
        this.declaration = declaration;
    }

    public String getName() {
        return name;
    }

    public IrType getReturnType() {
        return returnType;
    }

    public List<IrValue> getParameters() {
        return parameters;
    }

    public List<IrBasicBlock> getBlocks() {
        return blocks;
    }

    public boolean isDeclaration() {
        return declaration;
    }

    public void addParameter(IrValue parameter) {
        parameters.add(parameter);
    }

    public void addBlock(IrBasicBlock block) {
        blocks.add(block);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        // 函数名和返回类型
        result.append(declaration ? "declare " : "define ")
                .append(returnType)
                .append(" @")
                .append(name)
                .append("(");

        // 形参列表，参数之间用逗号分隔
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }

            IrValue parameter = parameters.get(i);
            result.append(parameter.getType());

            if (!declaration) {
                result.append(" ").append(parameter.asOperand());
            }
        }

        result.append(")");

        if (declaration) {
            result.append("\n");
            return result.toString();
        }

        result.append(" {\n");

        // 函数体：依次输出各个基本块
        for (IrBasicBlock block : blocks) {
            result.append(block);
        }

        result.append("}\n");

        return result.toString();
    }
}
