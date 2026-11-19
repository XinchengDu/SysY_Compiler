package ir;

import java.util.ArrayList;
import java.util.List;

public class IrBasicBlock {
    private final String name;
    private final List<IrInstruction> instructions = new ArrayList<>();

    public IrBasicBlock(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<IrInstruction> getInstructions() {
        return instructions;
    }

    public void addInstruction(IrInstruction instruction) {
        if (isTerminated()) {
            throw new IllegalStateException(
                "基本块 " + name + " 已结束，不能继续添加指令"
            );
        }

        instructions.add(instruction);
    }

    public boolean isTerminated() {
        if (instructions.isEmpty()) {
            return false;
        }

        IrInstruction last = instructions.get(instructions.size() - 1);
        return last.isTerminator();
    }

    public void addAlloca(IrInstruction instruction) {
        if (instruction.getOp() != IrInstruction.Op.ALLOCA) {
            throw new IllegalArgumentException("这里只能插入 ALLOCA 指令");
        }

        int index = 0;

        while (index < instructions.size() && instructions.get(index).getOp() == IrInstruction.Op.ALLOCA) {
            index++;
        }

        instructions.add(index, instruction);
    }

    @Override 
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(name).append(":\n");

        for (IrInstruction instruction : instructions) {
            result.append("    ")
                    .append(instruction)
                    .append("\n");
        }

        return result.toString();
    }
}