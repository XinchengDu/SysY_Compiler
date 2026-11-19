package ir;

public class IrValue {
    private final String name;
    private final IrType type;

    public IrValue(IrType type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public IrType getType() {
        return type;
    }

    public String asOperand() {
        return name;
    }
}
