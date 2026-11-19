package ir;

import java.util.Objects;

public final class IrType {
    public enum Kind {
        VOID,
        INTEGER,
        POINTER,
        ARRAY
    }

    public static final IrType VOID =
            new IrType(Kind.VOID, 0, null, 0);

    public static final IrType I1 =
            new IrType(Kind.INTEGER, 1, null, 0);

    public static final IrType I8 =
            new IrType(Kind.INTEGER, 8, null, 0);

    public static final IrType I32 =
            new IrType(Kind.INTEGER, 32, null, 0);

    private final Kind kind;
    private final int bitWidth;
    private final IrType elementType; // 指针，数组内存的元素类型
    private final int length;

    private IrType(
            Kind kind,
            int bitWidth,
            IrType elementType,
            int length
    ) {
        this.kind = kind;
        this.bitWidth = bitWidth;
        this.elementType = elementType;
        this.length = length;
    }

    public static IrType pointer(IrType elementType) {
        checkElementType(elementType);
        return new IrType(Kind.POINTER, 0, elementType, 0);
    }

    public static IrType array(int length, IrType elementType) {
        checkElementType(elementType);

        if (length < 0) {
            throw new IllegalArgumentException("数组长度不能为负数");
        }

        return new IrType(Kind.ARRAY, 0, elementType, length);
    }

    private static void checkElementType(IrType elementType) {
        if (elementType == null || elementType.kind == Kind.VOID) {
            throw new IllegalArgumentException("元素类型不能为空或 void");
        }
    }

    public Kind getKind() {
        return kind;
    }

    public int getBitWidth() {
        return bitWidth;
    }

    public IrType getElementType() {
        return elementType;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        switch (kind) {
            case VOID:
                return "void";
            case INTEGER:
                return "i" + bitWidth;
            case POINTER:
                return elementType + "*";
            case ARRAY:
                return "[" + length + " x " + elementType + "]";
            default:
                throw new IllegalStateException("未知类型：" + kind);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof IrType)) {
            return false;
        }

        IrType other = (IrType) obj;

        return kind == other.kind
                && bitWidth == other.bitWidth
                && length == other.length
                && Objects.equals(elementType, other.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, bitWidth, elementType, length);
    }
}