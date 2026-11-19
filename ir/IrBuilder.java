package ir;

import java.util.List;
import java.util.ArrayList;

public class IrBuilder {
    private IrFunction currentFunction;
    private IrBasicBlock currentBlock;

    private int valueId = 0;
    private int blockId = 0;
    private int parameterId = 0;

    public IrFunction beginFunction(String name, IrType returnType) {
        currentFunction = new IrFunction(name, returnType);

        valueId = 0;
        blockId = 0;
        parameterId = 0;

        currentBlock = createBlock();
        return currentFunction;
    }

    public IrBasicBlock createBlock() {
        IrBasicBlock block = new IrBasicBlock("b" + blockId++);
        currentFunction.addBlock(block);
        return block;
    }

    public void setCurrentBlock(IrBasicBlock block) {
        currentBlock = block;
    }

    private String nextValueName() {
        return "%t" + valueId++;
    }

    public IrValue addParameter(IrType type) {
        IrValue parameter = new IrValue(
            type,
            "%arg" + parameterId++
        );

        currentFunction.addParameter(parameter);
        return parameter;
    }

    public IrBasicBlock getCurrentBlock() {
        return currentBlock;
    }

    public IrInstruction emitBinary(
        IrInstruction.Op op,
        IrValue left,
        IrValue right
    ) {
        switch(op) {
            case ADD:
            case SUB:
            case MUL:
            case SDIV:
            case UDIV:
            case SREM:
            case UREM:
                break;

            default:
                throw new IllegalArgumentException("不是算术指令：" + op);
        }

        if (!left.getType().equals(right.getType()) || left.getType().getKind() != IrType.Kind.INTEGER) {
            throw new IllegalArgumentException("算术操作数必须是同类型整数");
        }

        IrInstruction instruction = new IrInstruction(
            left.getType(),
            nextValueName(),
            op,
            List.of(left, right),
            List.of(),
            null,
            null
        );

        currentBlock.addInstruction(instruction);
        return instruction;
    }

    public IrInstruction emitCompare(
        IrInstruction.Predicate predicate,
        IrValue left,
        IrValue right
    ) {
        if (!left.getType().equals(right.getType()) || left.getType().getKind() != IrType.Kind.INTEGER) {
            throw new IllegalArgumentException("算术操作数必须是同类型整数");
        }

        IrInstruction instruction = new IrInstruction(
            IrType.I1,
            nextValueName(),
            IrInstruction.Op.ICMP,
            List.of(left, right),
            List.of(),
            null,
            predicate
        );

        currentBlock.addInstruction(instruction);
        return instruction;
    }

    public IrValue emitCast(IrValue value, IrType targetType) {
        IrType sourceType = value.getType();

        if (sourceType.getKind() != IrType.Kind.INTEGER || targetType.getKind() != IrType.Kind.INTEGER) {
            throw new IllegalArgumentException("只支持整数类型转换");
        }

        if (sourceType.equals(targetType)) {
            return value;
        }

        IrInstruction.Op op;

        if (sourceType.getBitWidth() < targetType.getBitWidth()) {
            op = IrInstruction.Op.ZEXT;
        } else {
            op = IrInstruction.Op.TRUNC;
        }

        IrInstruction instruction = new IrInstruction(targetType, nextValueName(), op, List.of(value), List.of(), null, null);
        
        currentBlock.addInstruction(instruction);
        return instruction;
    }

    public IrInstruction emitAlloca(IrType objectType) {
        IrInstruction instruction = new IrInstruction(IrType.pointer(objectType), nextValueName(), IrInstruction.Op.ALLOCA, List.of(), List.of(), null, null);
        IrBasicBlock entryBlock = currentFunction.getBlocks().get(0);
        entryBlock.addAlloca(instruction);

        return instruction;
    }

    public IrInstruction emitLoad(IrValue address) {
        if (address.getType().getKind() != IrType.Kind.POINTER) {
            throw new IllegalArgumentException("LOAD 的操作数必须是地址");
        }

        IrInstruction instruction = new IrInstruction(address.getType().getElementType(), nextValueName(), IrInstruction.Op.LOAD, List.of(address), List.of(), null, null);

        currentBlock.addInstruction(instruction);
        return instruction;
    }

    public void emitStore(IrValue value, IrValue address)  {
        if (address.getType().getKind() != IrType.Kind.POINTER) {
            throw new IllegalArgumentException("LOAD 的操作数必须是地址");
        }

        if (!address.getType().getElementType().equals(value.getType())) {
            throw new IllegalArgumentException("写入值与目标存储类型不一致");
        }

        IrInstruction instruction = new IrInstruction(IrType.VOID, null, IrInstruction.Op.STORE, List.of(value, address), List.of(), null, null);
        currentBlock.addInstruction(instruction);
    }

    public IrInstruction emitGep(IrValue base, IrValue index) {
        if (base.getType().getKind() != IrType.Kind.POINTER) {
            throw new IllegalArgumentException("GEP 的基地址必须是指针");
        }

        if (!index.getType().equals(IrType.I32)) {
            throw new IllegalArgumentException("数组下标必须是 I32");
        }

        IrType objectType = base.getType().getElementType();
        IrType elementType;

        List<IrValue> operands = new ArrayList<>();
        operands.add(base);

        if (objectType.getKind() == IrType.Kind.ARRAY) {
            elementType = objectType.getElementType();
            operands.add(new IrConstant(IrType.I32, 0));
        } else {
            elementType = objectType;
        }

        if (!elementType.equals(IrType.I32) && !elementType.equals(IrType.I8)) {
            throw new IllegalArgumentException("数组元素必须是 int 或 char");
        }

        operands.add(index);

        IrInstruction instruction = new IrInstruction(IrType.pointer(elementType), nextValueName(), IrInstruction.Op.GEP, operands, List.of(), null, null);

        currentBlock.addInstruction(instruction);
        return instruction;
    }

    public IrInstruction emitCall(IrFunction function, List<IrValue> arguments) {
        List<IrValue> parameters = function.getParameters();

        if (parameters.size() != arguments.size()) {
            throw new IllegalArgumentException("函数调用参数数量不匹配");
        }

        for (int i = 0; i < arguments.size(); i++) {
            if (!arguments.get(i).getType().equals(parameters.get(i).getType())) {
                throw new IllegalArgumentException(
                        "函数调用第 " + (i + 1) + " 个参数类型不匹配"
                );
            }
        }

        IrType returnType = function.getReturnType();

        String name = null;
        if (!returnType.equals(IrType.VOID)) {
            name = nextValueName();
        }

        IrInstruction instruction = new IrInstruction(returnType, name, IrInstruction.Op.CALL, arguments, List.of(), function, null);
        currentBlock.addInstruction(instruction);
        return instruction;
    }

    public void emitBranch(IrBasicBlock target) {
        IrInstruction instruction = new IrInstruction(IrType.VOID, null, IrInstruction.Op.BR, List.of(), List.of(target), null, null);
        currentBlock.addInstruction(instruction);
    }

    public void emitCondBranch(IrValue condition, IrBasicBlock trueBlock, IrBasicBlock falseBlock) {
        if (!condition.getType().equals(IrType.I1)) {
            throw new IllegalArgumentException("条件跳转需要 I1 类型的条件");
        }

        IrInstruction instruction = new IrInstruction(IrType.VOID, null, IrInstruction.Op.BR, List.of(condition), List.of(trueBlock, falseBlock), null, null);

        currentBlock.addInstruction(instruction);
    }

    public void emitReturn(IrValue value) {
        IrType actualType = value == null ? IrType.VOID : value.getType();

        if (!actualType.equals(currentFunction.getReturnType())) {
            throw new IllegalArgumentException("返回值类型与函数返回类型不一致");
        }

        IrInstruction instruction = new IrInstruction(IrType.VOID, null, IrInstruction.Op.RET, value == null ? List.of() : List.of(value), List.of(), null, null);

        currentBlock.addInstruction(instruction);
    }
}
