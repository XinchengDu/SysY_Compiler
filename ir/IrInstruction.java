package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IrInstruction extends IrValue{

    public enum Op {
        ADD, SUB, MUL,
        SDIV, UDIV, SREM, UREM, // 有（无）符号除，有（无）符号取模
        ALLOCA, LOAD, STORE,
        GEP,
        ICMP,
        ZEXT, TRUNC,
        CALL,
        BR, RET
    }

    public enum Predicate {
        EQ, NE,
        SLT, SLE, SGT, SGE,
        ULT, ULE, UGT, UGE
    }

    private final Op op;
    private final List<IrValue> operands;
    private final List<IrBasicBlock> targets;
    private final IrFunction callee;
    private final Predicate predicate;

    public IrInstruction(
        IrType resultType,
        String name,
        Op op,
        List<IrValue> operands, // 操作数
        List<IrBasicBlock> targets,
        IrFunction callee,
        Predicate predicate
    ) {
        super(resultType, name);
        this.op = op;
        this.operands = new ArrayList<>(operands);
        this.targets = new ArrayList<>(targets);
        this.callee = callee;
        this.predicate = predicate;
    }
    
    public Op getOp() {
        return op;
    }

    public List<IrValue> getOperands() {
        return Collections.unmodifiableList(operands);
    }

    public List<IrBasicBlock> getTargets() {
        return Collections.unmodifiableList(targets);
    }

    public IrFunction getCallee() {
        return callee;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public boolean hasResult() {
        return !IrType.VOID.equals(getType());
    }

    public boolean isTerminator() {
        return op == Op.BR || op == Op.RET;
    }

    @Override
    public String asOperand() {
        if (!hasResult()) {
            throw new IllegalStateException("无结果指令不能作为操作数");
        }
        return super.asOperand();
    }

    private String typedOperand(IrValue value) {
        return value.getType() + " " + value.asOperand();
    }

    @Override 
    public String toString() {
        switch (op) {
            // %t0 = add i32 3, 5
            case ADD:
            case SUB:
            case MUL:
            case SDIV:
            case UDIV:
            case SREM:
            case UREM:
                return getName() + " = "
                + op.name().toLowerCase(java.util.Locale.ROOT)
                + " " + getType()
                + " " + operands.get(0).asOperand()
                + ", " + operands.get(1).asOperand();

            // %p = alloca i32
            case ALLOCA:
                return getName() + " = alloca "
                + getType().getElementType();

            // %t1 = load i32, i32* %p
            case LOAD:
                return getName() + " = load "
                + getType()
                + ", " + typedOperand(operands.get(0));

            // store i32 3, i32* %p
            case STORE:
                return "store "
                + typedOperand(operands.get(0))
                + ", " + typedOperand(operands.get(1));

            // ret i32 0 或 ret void
            case RET:
                if (operands.isEmpty()) {
                    return "ret void";
                }
                return "ret " + typedOperand(operands.get(0));

            // %t0 = icmp slt i32 %a, %b
            case ICMP:
                return getName() + " = icmp "
                + predicate.name().toLowerCase(java.util.Locale.ROOT)
                + " " + typedOperand(operands.get(0))
                + ", " + operands.get(1).asOperand();

             /*
                %t1 = zext i8 %c to i32 零扩展，将 char 转换成 int
                %t2 = trunc i32 %x to i8 截断，将 int 转换成 char
             */
            case ZEXT:
            case TRUNC:
                return getName() + " = "
                + op.name().toLowerCase(java.util.Locale.ROOT)
                + " " + typedOperand(operands.get(0))
                + " to " + getType();

             /*
                无条件时：br label %end
                有条件时：br i1 %cond, label %then, label %else
             */
            case BR:
                if (operands.isEmpty()) {
                    return "br label %" + targets.get(0).getName();
                }

                return "br " + typedOperand(operands.get(0))
                + ", label %" + targets.get(0).getName()
                + ", label %" + targets.get(1).getName();
            
             /*
                有返回值：%t3 = call i32 @sum(i32 3, i32 5)
                无返回值：call void @put_int(i32 %t3)
             */
            case CALL: {
                StringBuilder result = new StringBuilder();

                if (hasResult()) {
                    result.append(getName()).append(" = ");
                }

                result.append("call ")
                    .append(callee.getReturnType())
                    .append(" @")
                    .append(callee.getName())
                    .append("(");

                for (int i = 0; i < operands.size(); i++) {
                    if (i > 0) {
                        result.append(", ");
                    }

                    result.append(typedOperand(operands.get(i)));
                }

                result.append(")");
                return result.toString();
            }

            /*
            * GEP：只计算地址，不读取或修改内存。
            * 例如 a[2] = 7：先用 GEP 求出 a[2] 的地址，再用 STORE 写入 7。
            *
            * operands[0] 是基地址，后面的操作数是索引。
            *
            * 本文法只支持一维数组，但 GEP 的索引个数取决于基地址类型：
            *
            * 1. 基地址指向整个数组，如 [4 x i32]*：
            *    operands = [数组地址, 0, 2]
            *    输出：getelementptr [4 x i32], [4 x i32]* %a, i32 0, i32 2
            *    0：跨过 0 个完整数组，仍选当前数组。
            *    2：选中该数组的 a[2]。
            *    地址偏移 = 0 * 数组总大小 + 2 * 元素大小。
            *    注意：两个索引不代表二维数组！
            *
            * 2. 基地址指向首元素，如数组形参的 i32*：
            *    operands = [首元素地址, 2]
            *    输出：getelementptr i32, i32* %a, i32 2
            *    直接向后移动 2 个元素，得到 a[2] 的地址。
            *
            * 两种情况下，结果都是元素地址：int 元素为 i32*，char 元素为 i8*。
            * 索引按元素计数，不是字节偏移；int 元素占 4 字节，char 元素占 1 字节。
            */
            case GEP: {
                IrValue base = operands.get(0);

                StringBuilder result = new StringBuilder();

                result.append(getName())
                    .append(" = getelementptr ")
                    .append(base.getType().getElementType())
                    .append(", ")
                    .append(typedOperand(base));

                for (int i = 1; i < operands.size(); i++) {
                    result.append(", ")
                        .append(typedOperand(operands.get(i)));
                }

                return result.toString();
            }

            default:
            throw new UnsupportedOperationException(
                    "尚未实现该指令的文本输出：" + op
            );
        }
    }
}
