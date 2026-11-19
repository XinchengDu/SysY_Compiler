package ir;

public class IrConstant extends IrValue {

    private final int value;

    public IrConstant(IrType type, int value) {
        super(type, null);
        if (IrType.I8.equals(type)) {
            this.value = value & 0xff;
        } else if (IrType.I1.equals(type)) {
            this.value = value & 1;
        } else if (IrType.I32.equals(type)) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("标量常量需要整数类型");
        }
    }

    public int getValue() {
        return value;
    }

    @Override 
    public String asOperand() {
        return Integer.toString(value);
    }

    @Override 
    public String toString() {
        return getType() + " " + asOperand();
    }
}
