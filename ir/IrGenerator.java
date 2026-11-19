package ir;

import parser.AstNode;
import semantic.Symbol;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Deque;

import lexer.Token;

public class IrGenerator {
    private final AstNode root;
    private final IrModule module = new IrModule();
    private final IrBuilder builder = new IrBuilder();
    private final Map<Symbol, IrValue> addresses = new HashMap<>();
    private final Map<Symbol, List<IrConstant>> constantValues = new HashMap<>();
    private final Map<Symbol, IrFunction> functions = new HashMap<>();
    private final Map<String, IrFunction> libraryFunctions = new HashMap<>();
    private final IrFunction putInt;
    private final IrFunction putChar;
    private final IrFunction putString;
    private final Deque<IrBasicBlock> breakTargets = new ArrayDeque<>();
    private final Deque<IrBasicBlock> continueTargets = new ArrayDeque<>();
    private IrFunction currentFunction = null;
    private int staticVariableId = 0;

    public IrGenerator(AstNode root) {
        this.root = root;

        declareLibraryFunction("get_int", IrType.I32);
        declareLibraryFunction("get_char", IrType.I8);
        declareLibraryFunction("get_string", IrType.VOID, IrType.pointer(IrType.I8), IrType.I32);

        putInt = declareLibraryFunction("put_int", IrType.VOID, IrType.I32);
        putChar = declareLibraryFunction("put_char", IrType.VOID, IrType.I8);
        putString = declareLibraryFunction("put_string", IrType.VOID, IrType.pointer(IrType.I8));
    }

    private IrFunction declareLibraryFunction(String name, IrType type, IrType... parameters) {
        IrFunction function = new IrFunction(name, type, true); // true 表示只声明，不生成函数体

        for (int i = 0; i < parameters.length; i++) {
            function.addParameter(new IrValue(parameters[i], "%arg" + i));
        }

        module.addFunction(function);
        libraryFunctions.put(name, function);
        return function;
    }

    public IrModule generate() {
        emitCompUnit(root);
        return module;
    }

    private void emitCompUnit(AstNode node) {
        for (AstNode child : node.getChildren()) {
            switch (child.getType()) {
                case "Decl":
                    emitDecl(child);
                    break;
                
                case "FuncDef":
                    emitFuncDef(child);
                    break;

                case "MainFuncDef":
                    emitMainFuncDef(child);
                    break;
            }
        }
    }

    private void emitDecl(AstNode node) {
        AstNode child = node.getChildren().get(0);

        if (child.getType().equals("ConstDecl")) {
            emitConstDecl(child);
        }

        else {
            emitVarDecl(child);
        }
    }

    private void emitConstDecl(AstNode node) {
        for (AstNode child : node.getChildren()) {
            if ("ConstDef".equals(child.getType())) {
                emitConstDef(child);
            }
        }
    }

    private void emitConstDef(AstNode node) {
        Symbol symbol = node.getSymbol();
        Symbol.Type type = symbol.getType();
        boolean isArray = false;
        IrType elementType;

        if (type == Symbol.Type.INT_ARRAY || type == Symbol.Type.CHAR_ARRAY) {
            isArray = true;
        }

        if (type == Symbol.Type.INT || type == Symbol.Type.INT_ARRAY) {
            elementType = IrType.I32;
        }

        else {
            elementType = IrType.I8;
        }

        int count = 1; // 记录数组元素个数，标量记为 1

        if (isArray) {
            AstNode constExpNode = node.getChildren().get(2);
            IrConstant length = emitConstExp(constExpNode);
            count = length.getValue();
        }

        IrType objectType;

        if (isArray) {
            objectType = IrType.array(count, elementType);
        } else {
            objectType = elementType;
        }

        List<AstNode> children = node.getChildren();
        AstNode constInitValNode = children.get(children.size() - 1);

        List<IrConstant> values = emitConstInitVal(constInitValNode, objectType);

        IrValue address;

        if (currentFunction == null) {
            IrGlobalVariable global = new IrGlobalVariable(symbol.getName(), objectType, true, values);

            module.addGlobalVariable(global);
            address = global;
        } else {
            address = builder.emitAlloca(objectType);

            if (isArray) {
                for (int i = 0; i < count; i++) {
                    IrValue elementAddress = builder.emitGep(address, new IrConstant(IrType.I32, i));

                    builder.emitStore(values.get(i), elementAddress);
                }
            } else {
                builder.emitStore(values.get(0), address);
            }
        }

        addresses.put(symbol, address);
        constantValues.put(symbol, values);
    }

    private void emitVarDecl(AstNode node) {
        for (AstNode child : node.getChildren()) {
            if (child.getType().equals("VarDef")) {
                emitVarDef(child);
            }
        }
    }

    private void emitVarDef(AstNode node) {
        Symbol symbol = node.getSymbol();
        Symbol.Type type = symbol.getType();
        boolean isArray = false;
        IrType elementType;
        boolean isStatic = symbol.isStatic();
        if (type == Symbol.Type.INT_ARRAY || type == Symbol.Type.CHAR_ARRAY) {
            isArray = true;
        }

        if (type == Symbol.Type.INT || type == Symbol.Type.INT_ARRAY) {
            elementType = IrType.I32;
        } else {
            elementType = IrType.I8;
        }

        int count = 1; // 记录数组元素个数，标量记为 1

        if (isArray) {
            AstNode constExpNode = node.getChildren().get(2);
            IrConstant length = emitConstExp(constExpNode);
            count = length.getValue();
        }

        IrType objectType;

        if (isArray) {
            objectType = IrType.array(count, elementType);
        } else {
            objectType = elementType;
        }

        List<AstNode> children = node.getChildren();
        AstNode last = children.get(children.size() - 1);

        AstNode initNode = null;
        if (last.getType().equals("InitVal")) {
            initNode = last;
        }

        if (currentFunction == null || isStatic) {
            List<IrConstant> values;

            if (initNode == null) {
                // IrGlobalVariable 会将没有提供的初值补零。
                values = List.of();
            } else {
                values = emitConstInitVal(initNode, objectType);
            }

            String name = symbol.getName();

            if (isStatic) {
                name = ".static." + staticVariableId++ + "." + symbol.getName();
            }

            IrGlobalVariable global = new IrGlobalVariable(name, objectType, false, values);

            module.addGlobalVariable(global);
            addresses.put(symbol, global);
        } else {
            IrValue address = builder.emitAlloca(objectType);
            addresses.put(symbol, address);

            if (initNode != null) {
                emitInitVal(initNode, objectType, address);
            }
        }
    }

    private List<IrConstant> emitConstInitVal(AstNode node, IrType objectType) {
        boolean isArray = false;
        if (objectType.getKind() == IrType.Kind.ARRAY) {
            isArray = true;
        }

        IrType elementType;
        if (isArray) {
            elementType = objectType.getElementType();
        } else {
            elementType = objectType;
        }

        int count;
        if (isArray) {
            count = objectType.getLength();
        } else {
            count = 1;
        }

        List<IrConstant> values = new ArrayList<>();
        AstNode first = node.getChildren().get(0);

        if (first.getType().equals("STRCON")) {
            List<IrConstant> characters = emitStringConst(first);

            int copyCount = Math.min(count, characters.size());

            for (int i = 0; i < copyCount; i++) {
                values.add(characters.get(i));
            }
        } else {
            for (AstNode child : node.getChildren()) {
                if (child.getType().equals("ConstExp") || child.getType().equals("Exp")) {
                    values.add(emitConstExp(child));
                }
            }
        }

        while (values.size() < count) {
            values.add(new IrConstant(elementType, 0));
        }

        return values;
    }

    private void emitInitVal(AstNode node, IrType objectType, IrValue address) {
        boolean isArray = false;
        if (objectType.getKind() == IrType.Kind.ARRAY) {
            isArray = true;
        }

        IrType elementType;
        if (isArray) {
            elementType = objectType.getElementType();
        } else {
            elementType = objectType;
        }

        int count;
        if (isArray) {
            count = objectType.getLength();
        } else {
            count = 1;
        }

        // List<IrValue> values = new ArrayList<>();
        AstNode first = node.getChildren().get(0);

        int index = 0;

        if (first.getType().equals("STRCON")) {
            List<IrConstant> characters = emitStringConst(first);

            int copyCount = Math.min(count, characters.size());

            for (int i = 0; i < copyCount; i++) {
                IrValue elementAddress = builder.emitGep(address, new IrConstant(IrType.I32, i));
                builder.emitStore(characters.get(index), elementAddress);
                index++;
            }
        } else {
            for (AstNode child : node.getChildren()) {
                if (child.getType().equals("Exp")) {
                    IrValue value = emitExp(child);
                    IrValue elementAddress = builder.emitGep(address, new IrConstant(IrType.I32, index));
                    builder.emitStore(value, elementAddress);
                    index++;
                }
            }
        }

        while (index < count) {
            IrValue targetAddress = address; // 标量直接使用自身地址

            if (isArray) {
                targetAddress = builder.emitGep(address, new IrConstant(IrType.I32, index));
            }

            builder.emitStore(new IrConstant(elementType, 0), targetAddress);
            index++;
        }
    }

    private List<IrConstant> emitStringConst(AstNode node) {
        List<IrConstant> character = new ArrayList<>();
        Token token = node.getToken();
        String content = token.getContent();
        for (int i = 1; i < content.length() - 1; i++) {
            char value = content.charAt(i);
            if (value == '\\' && i + 1 < content.length() - 1 && content.charAt(i + 1) == 'n') {
                value = '\n';
                i++;
            }

            character.add(new IrConstant(IrType.I8, value));
        }
        return character;
    }

    private IrConstant emitConstExp(AstNode node) {
        return emitConstAddExp(node.getChildren().get(0));
    }

    private IrConstant emitConstAddExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        IrConstant result = emitConstMulExp(children.get(0));
        for (int i = 2; i < children.size(); i = i + 2) {
            String operator = children.get(i - 1).getType();
            IrConstant right = emitConstMulExp(children.get(i));

            int value;
            if (operator.equals("PLUS")) {
                value = right.getValue() + result.getValue();
            } else {
                value = result.getValue() - right.getValue();
            }

            result = new IrConstant(result.getType(), value);
        }

        return result;
    }

    private IrConstant emitConstMulExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        IrConstant result = emitConstUnaryExp(children.get(0));
        for (int i = 2; i < children.size(); i = i + 2) {
            String operator = children.get(i - 1).getType();
            IrConstant right = emitConstUnaryExp(children.get(i));

            int value;
            if (operator.equals("MULT")) {
                value = right.getValue() * result.getValue();
            } else if (operator.equals("DIV")) {
                value = result.getValue() / right.getValue();
            } else {
                value = result.getValue() % right.getValue();
            }

            result = new IrConstant(result.getType(), value);
        }

        return result;
    }

    private IrConstant emitConstUnaryExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        AstNode first = children.get(0);

        if (first.getType().equals("PrimaryExp")) {
            return emitConstPrimaryExp(first);
        }

        if (first.getType().equals("UnaryOp")) {
            String operator = first.getChildren().get(0).getType();
            IrConstant operand = emitConstUnaryExp(children.get(1));

            if (operator.equals("PLUS")) {
                return operand;
            } else if (operator.equals("MINU")) {
                return new IrConstant(operand.getType(), -operand.getValue());
            } else {
                if (operand.getValue() == 0) {
                    return new IrConstant(IrType.I32, 1);
                } else {
                    return new IrConstant(IrType.I32, 0);
                }
            }
        }

        if (first.getType().equals("LPARENT")) {
            AstNode bType = children.get(1);
            IrConstant operand = emitConstUnaryExp(children.get(3));

            if (bType.getChildren().get(0).getType().equals("INTTK")) {
                return new IrConstant(IrType.I32, operand.getValue());
            } else {
                return new IrConstant(IrType.I8, operand.getValue());
            }
        }

        throw new IllegalArgumentException("常量表达式不能包含函数调用");
    }

    private IrConstant emitConstPrimaryExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        AstNode first = children.get(0);

        if (first.getType().equals("LPARENT")) {
            return emitConstExp(children.get(1));
        } else if (first.getType().equals("LVal")) {
            return emitConstLVal(first);
        } else {
            return emitNumber(first);
        }
    }

    private IrConstant emitConstLVal(AstNode node) {
        Symbol symbol = node.getSymbol();
        List<IrConstant> values = constantValues.get(symbol);
        List<AstNode> children = node.getChildren();

        if (children.size() == 1) {
            return values.get(0);
        } else {
            IrConstant index = emitConstExp(children.get(2));
            return values.get(index.getValue());
        }
    }

    private IrConstant emitNumber(AstNode node) {
        AstNode child = node.getChildren().get(0);
        Token token = child.getToken();
        String numberString = token.getContent();
        if (child.getType().equals("INTCON")) {
            int value = Integer.parseInt(numberString);
            return new IrConstant(IrType.I32, value);
        } else {
            char value = numberString.charAt(1);
            if (value == '\\') {
                char escape = numberString.charAt(2);

                switch (escape) {
                    case 'n':
                        value = '\n';
                        break;
                    case 't':
                        value = '\t';
                        break;
                    case 'r':
                        value = '\r';
                        break;
                    case '0':
                        value = '\0';
                        break;
                    case '\\':
                        value = '\\';
                        break;
                    case '\'':
                        value = '\'';
                        break;
                }
            }

            return new IrConstant(IrType.I8, value);
        }
    }

    private IrValue emitExp(AstNode node) {
        return emitAddExp(node.getChildren().get(0));
    }

    private IrValue emitAddExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        IrValue result = emitMulExp(children.get(0));

        for (int i = 2; i < children.size(); i = i + 2) {
            String operator = children.get(i - 1).getType();
            IrValue right = emitMulExp(children.get(i));

            if (operator.equals("PLUS")) {
                result = builder.emitBinary(IrInstruction.Op.ADD, result, right);
            } else {
                result = builder.emitBinary(IrInstruction.Op.SUB, result, right);
            }
        }

        return result;
    }

    private IrValue emitMulExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        IrValue result = emitUnaryExp(children.get(0));

        for (int i = 2; i < children.size(); i = i + 2) {
            String operator = children.get(i - 1).getType();
            IrValue right = emitUnaryExp(children.get(i));

            if (operator.equals("MULT")) {
                result = builder.emitBinary(IrInstruction.Op.MUL, result, right);
            } else if (operator.equals("DIV")) {
                if (result.getType().equals(IrType.I8)) {
                    result = builder.emitBinary(IrInstruction.Op.UDIV, result, right);
                } else {
                    result = builder.emitBinary(IrInstruction.Op.SDIV, result, right);
                }
            } else {
                if (result.getType().equals(IrType.I8)) {
                    result = builder.emitBinary(IrInstruction.Op.UREM, result, right);
                } else {
                    result = builder.emitBinary(IrInstruction.Op.SREM, result, right);
                }
            }
        }

        return result;
    }

    private IrValue emitUnaryExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        AstNode first = children.get(0);

        if (first.getType().equals("PrimaryExp")) {
            return emitPrimaryExp(first);
        }

        if (first.getType().equals("IDENFR")) {
            Symbol symbol = node.getSymbol();
            IrFunction function = functions.get(symbol);

            if (function == null && symbol.getLineNum() == 0) {
                function = libraryFunctions.get(symbol.getName()); // 符号表用第 0 行标记内置函数
            }

            List<IrValue> arguments = new ArrayList<>();

            AstNode params = children.get(2);
            if (params.getType().equals("FuncRParams")) {
                for (AstNode child : params.getChildren()) {
                    if (child.getType().equals("Exp")) {
                        arguments.add(emitExp(child));
                    }
                }
            }

            return builder.emitCall(function, arguments);
        }

        if (first.getType().equals("UnaryOp")) {
            String operator = first.getChildren().get(0).getType();
            IrValue operand = emitUnaryExp(children.get(1));

            if (operator.equals("PLUS")) {
                return operand;
            } else if (operator.equals("MINU")) {
                IrConstant zero = new IrConstant(operand.getType(), 0);
                return builder.emitBinary(IrInstruction.Op.SUB, zero, operand);
            } else {
                IrConstant zero = new IrConstant(operand.getType(), 0);
                IrValue result = builder.emitCompare(IrInstruction.Predicate.EQ, operand, zero);
                return builder.emitCast(result, IrType.I32);
            }
        }

        String bType = children.get(1).getChildren().get(0).getType();
        IrValue value = emitUnaryExp(children.get(3));
        if (bType.equals("INTTK")) {
            return builder.emitCast(value, IrType.I32);
        } else {
            return builder.emitCast(value, IrType.I8);
        }
    }

    private IrValue emitPrimaryExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        AstNode first = children.get(0);

        if (first.getType().equals("LPARENT")) {
            return emitExp(children.get(1));
        }

        if (first.getType().equals("LVal")) {
            IrValue address = emitLVal(first);
            Symbol.Type type = first.getSymbol().getType();

            boolean isArray = false;
            if (type == Symbol.Type.INT_ARRAY || type == Symbol.Type.CHAR_ARRAY) {
                isArray = true;
            }

            if (isArray && first.getChildren().size() == 1) {
                IrType objectType = address.getType().getElementType();
                if (objectType.getKind() == IrType.Kind.ARRAY) {
                    return builder.emitGep(address, new IrConstant(IrType.I32, 0));
                } else {
                    return address;
                }
            }

            return builder.emitLoad(address);
        }

        return emitNumber(children.get(0));
    }

    private IrValue emitLVal(AstNode node) {
        Symbol symbol = node.getSymbol();
        IrValue address = addresses.get(symbol);
        List<AstNode> children = node.getChildren();

        if (children.size() == 1) {
            return address;
        } else {
            IrValue index = emitExp(children.get(2));
            return builder.emitGep(address, index);
        }
    }

    private void emitFuncDef(AstNode node) {
        List<AstNode> children = node.getChildren();
        Symbol symbol = node.getSymbol();
        String type = children.get(0).getChildren().get(0).getType();
        IrType returnType;
        if (type.equals("VOIDTK")) {
            returnType = IrType.VOID;
        } else if (type.equals("INTTK")) {
            returnType = IrType.I32;
        } else {
            returnType = IrType.I8;
        }

        currentFunction = builder.beginFunction(symbol.getName(), returnType);

        module.addFunction(currentFunction);
        functions.put(symbol, currentFunction);

        for (AstNode child : children) {
            if (child.getType().equals("FuncFParams")) {
                emitFuncFParams(child);
            } else if (child.getType().equals("Block")) {
                emitBlock(child);
            }
        }

        if (returnType.equals(IrType.VOID) && !builder.getCurrentBlock().isTerminated()) {
            builder.emitReturn(null);
        }

        currentFunction = null;
    }

    private void emitFuncFParams(AstNode node) {
        for (AstNode child : node.getChildren()) {
            if (child.getType().equals("FuncFParam")) {
                emitFuncFParam(child);
            }
        }
    }

    private void emitFuncFParam(AstNode node) {
        Symbol symbol = node.getSymbol();
        boolean isArray = false;
        IrValue address;
        IrType elementType;

        if (symbol.getType() == Symbol.Type.INT_ARRAY || symbol.getType() == Symbol.Type.CHAR_ARRAY) {
            isArray = true;
        }

        if (symbol.getType() == Symbol.Type.INT || symbol.getType() == Symbol.Type.INT_ARRAY) {
            elementType = IrType.I32;
        } else {
            elementType = IrType.I8;
        }

        if (isArray) {
            IrValue parameter = builder.addParameter(IrType.pointer(elementType));
            address = parameter;
        } // 数组形参不分配空间，只取其首地址

        else {
            IrValue parameter = builder.addParameter(elementType);
            address = builder.emitAlloca(elementType);
            builder.emitStore(parameter, address);
        }

        addresses.put(symbol, address);
    }

    private void emitBlock(AstNode node) {
        for (AstNode child : node.getChildren()) {
            if (child.getType().equals("BlockItem")) {
                if (builder.getCurrentBlock().isTerminated()) {
                    break;
                }

                emitBlockItem(child);
            }
        }
    }

    private void emitBlockItem(AstNode node) {
        AstNode child = node.getChildren().get(0);
        if (child.getType().equals("Decl")) {
            emitDecl(child);
        } else {
            emitStmt(child);
        }
    }

    private void emitStmt(AstNode node) {
        List<AstNode> children = node.getChildren();
        AstNode first = children.get(0);

        if (first.getType().equals("LVal")) {
            IrValue right = emitExp(children.get(2));
            builder.emitStore(right, emitLVal(first));
            return;
        }

        if (first.getType().equals("Exp")) {
            emitExp(first);
            return;
        }

        if (first.getType().equals("SEMICN")) {
            return;
        }

        if (first.getType().equals("Block")) {
            emitBlock(first);
        }

        if (first.getType().equals("IFTK")) {
            boolean hasElse = false;
            if (children.size() > 5) {
                hasElse = true;
            }

            IrBasicBlock thenBlock = builder.createBlock();
            IrBasicBlock falseBlock = builder.createBlock();

            emitCond(children.get(2), thenBlock, falseBlock);

            builder.setCurrentBlock(thenBlock);
            emitStmt(children.get(4));
            IrBasicBlock thenEnd = builder.getCurrentBlock();

            if (!hasElse) {
                if (!thenEnd.isTerminated()) {
                    builder.emitBranch(falseBlock);
                }

                builder.setCurrentBlock(falseBlock);
                return;
            }

            builder.setCurrentBlock(falseBlock);
            emitStmt(children.get(6));
            IrBasicBlock elseEnd = builder.getCurrentBlock();

            if (thenEnd.isTerminated() && elseEnd.isTerminated()) {
                return;
            }

            IrBasicBlock endBlock = builder.createBlock();

            if (!thenEnd.isTerminated()) {
                builder.setCurrentBlock(thenEnd);
                builder.emitBranch(endBlock);
            }

            if (!elseEnd.isTerminated()) {
                builder.setCurrentBlock(elseEnd);
                builder.emitBranch(endBlock);
            }

            builder.setCurrentBlock(endBlock);
        }

        if (first.getType().equals("WHILETK")) {
            IrBasicBlock condBlock = builder.createBlock();
            IrBasicBlock thenBlock = builder.createBlock();
            IrBasicBlock falseBlock = builder.createBlock();

            builder.emitBranch(condBlock);
            builder.setCurrentBlock(condBlock);
            emitCond(children.get(2), thenBlock, falseBlock);

            builder.setCurrentBlock(thenBlock);
            breakTargets.push(falseBlock);
            continueTargets.push(condBlock);

            emitStmt(children.get(4));

            continueTargets.pop();
            breakTargets.pop();

            IrBasicBlock thenEnd = builder.getCurrentBlock();

            if (!thenEnd.isTerminated()) {
                builder.emitBranch(condBlock);
            }

            builder.setCurrentBlock(falseBlock);
            return;
        }

        if (first.getType().equals("SWITCHTK")) {
            emitSwitchStmt(node);
            return;
        }

        if (first.getType().equals("BREAKTK")) {
            builder.emitBranch(breakTargets.peek()); // 跳到最近一层 switch 或 while 的出口
            return;
        }

        if (first.getType().equals("CONTINUETK")) {
            builder.emitBranch(continueTargets.peek());
            return;
        }

        if (first.getType().equals("RETURNTK")) {
            IrValue value = null;
            if (children.size() > 2) {
                value = emitExp(children.get(1));
            }
            builder.emitReturn(value);
            return;
        }

        if (first.getType().equals("PRINTFTK")) {
            List<IrConstant> format = emitStringConst(children.get(2));
            List<IrValue> arguments = new ArrayList<>();

            for (AstNode child : children) {
                if (child.getType().equals("Exp")) {
                    arguments.add(emitExp(child));
                }
            }

            int index = 0;

            for (int i = 0; i < format.size(); i++) {
                IrConstant ch = format.get(i);

                if (ch.getValue() == '%' && i + 1 < format.size()) {
                    int next = format.get(i + 1).getValue();

                    switch (next) {
                        case 'd':
                            builder.emitCall(putInt, List.of(arguments.get(index)));
                            break;
                    
                        case 'c':
                            builder.emitCall(putChar, List.of(arguments.get(index)));
                            break;

                        case 's':
                            builder.emitCall(putString, List.of(arguments.get(index)));
                            break;

                        default:
                            builder.emitCall(putChar, List.of(ch));
                            continue;
                    }

                    index++;
                    i++;
                } else {
                    builder.emitCall(putChar, List.of(ch));
                }
            }
        }
    }

    private void emitMainFuncDef(AstNode node) {
        IrType returnType = IrType.I32;
        currentFunction = builder.beginFunction("main", returnType);
        Symbol symbol = new Symbol("main", node.getChildren().get(0).getToken().getNum(), Symbol.Kind.FUNCTION, Symbol.Type.INT);

        module.addFunction(currentFunction);
        functions.put(symbol, currentFunction);
        emitBlock(node.getChildren().get(4));

        currentFunction = null;
    }

    private void emitSwitchStmt(AstNode node) {
        List<AstNode> children = node.getChildren();
        IrValue value = emitExp(children.get(2)); // switch(x) 的 x 只计算一次
        IrBasicBlock endBlock = builder.createBlock(); // 整个 switch 的出口
        IrBasicBlock defaultBlock = endBlock; // 没写 default 时，匹配失败就退出

        List<AstNode> cases = new ArrayList<>();
        List<IrBasicBlock> bodyBlocks = new ArrayList<>();

        for (AstNode child : children) {
            if (child.getType().equals("CaseStmt")) {
                IrBasicBlock bodyBlock = builder.createBlock();
                cases.add(child);
                bodyBlocks.add(bodyBlock);

                if (child.getChildren().get(0).getType().equals("DEFAULTTK")) {
                    defaultBlock = bodyBlock;
                }
            }
        }

        for (int i = 0; i < cases.size(); i++) {
            List<AstNode> parts = cases.get(i).getChildren();
            if (parts.get(0).getType().equals("DEFAULTTK")) {
                continue;
            }

            IrValue caseValue = emitNumber(parts.get(1));
            IrValue equal = builder.emitCompare(IrInstruction.Predicate.EQ, value, caseValue);
            IrBasicBlock nextCheck = builder.createBlock();
            builder.emitCondBranch(equal, bodyBlocks.get(i), nextCheck);
            builder.setCurrentBlock(nextCheck);
        }
        builder.emitBranch(defaultBlock);

        breakTargets.push(endBlock);
        for (int i = 0; i < cases.size(); i++) {
            builder.setCurrentBlock(bodyBlocks.get(i));

            for (AstNode part : cases.get(i).getChildren()) {
                if (builder.getCurrentBlock().isTerminated()) {
                    break;
                }
                if (part.getType().equals("Stmt")) {
                    emitStmt(part);
                }
            }

            if (!builder.getCurrentBlock().isTerminated()) {
                if (i + 1 < cases.size()) {
                    builder.emitBranch(bodyBlocks.get(i + 1));
                } else {
                    builder.emitBranch(endBlock);
                }
            }
        }
        breakTargets.pop();
        builder.setCurrentBlock(endBlock);
    }

    private void emitCond(AstNode node, IrBasicBlock trueBlock, IrBasicBlock falseBlock) {
        AstNode child = node.getChildren().get(0);
        emitLOrExp(child, trueBlock, falseBlock);
    }

    private void emitLOrExp(AstNode node, IrBasicBlock trueBlock, IrBasicBlock falseBlock) {
        List<AstNode> children = node.getChildren();

        for (int i = 0; i < children.size(); i += 2) {
            AstNode child = children.get(i);

            if (i == children.size() - 1) {
                emitLAndExp(child, trueBlock, falseBlock);
            } else {
                IrBasicBlock nextBlock = builder.createBlock();
                emitLAndExp(child, trueBlock, nextBlock);
                builder.setCurrentBlock(nextBlock);
            }
        }
    }

    private void emitLAndExp(AstNode node, IrBasicBlock trueBlock, IrBasicBlock falseBlock) {
        List<AstNode> children = node.getChildren();

        for (int i = 0; i < children.size(); i += 2) {
            AstNode child = children.get(i);

            if (i == children.size() - 1) {
                emitEqExp(child, trueBlock, falseBlock);
            } else {
                IrBasicBlock nextBlock = builder.createBlock();
                emitEqExp(child, nextBlock, falseBlock);
                builder.setCurrentBlock(nextBlock);
            }
        }
    }

    private void emitEqExp(AstNode node, IrBasicBlock trueBlock, IrBasicBlock falseBlock) {
        List<AstNode> children = node.getChildren();
        IrValue result = emitRelExp(children.get(0));

        for (int i = 2; i < children.size(); i += 2) {
            String operator = children.get(i - 1).getType();
            IrValue right = emitRelExp(children.get(i));

            if (operator.equals("EQL")) {
                result = builder.emitCompare(IrInstruction.Predicate.EQ, result, right);
            } else {
                result = builder.emitCompare(IrInstruction.Predicate.NE, result, right);
            }

            result = builder.emitCast(result, IrType.I32);
        }

        IrConstant zero = new IrConstant(result.getType(), 0);
        IrValue condition = builder.emitCompare(IrInstruction.Predicate.NE, result, zero);
        builder.emitCondBranch(condition, trueBlock, falseBlock);
    }

    private IrValue emitRelExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        IrValue result = emitAddExp(children.get(0));
        
        for (int i = 2; i < children.size(); i += 2) {
            String operator = children.get(i - 1).getType();
            IrValue right = emitAddExp(children.get(i));
            IrInstruction.Predicate predicate;
            if (result.getType().equals(IrType.I8)) { // char 按无符号数比较
                switch (operator) {
                    case "LSS":
                        predicate = IrInstruction.Predicate.ULT; // <
                        break;
                    case "GRE":
                        predicate = IrInstruction.Predicate.UGT; // >
                        break;
                    case "LEQ":
                        predicate = IrInstruction.Predicate.ULE; // <=
                        break;
                    default:
                        predicate = IrInstruction.Predicate.UGE; // >=
                        break;
                }
            } else { // int 按有符号数比较
                switch (operator) {
                    case "LSS":
                        predicate = IrInstruction.Predicate.SLT; // <
                        break;
                    case "GRE":
                        predicate = IrInstruction.Predicate.SGT; // >
                        break;
                    case "LEQ":
                        predicate = IrInstruction.Predicate.SLE; // <=
                        break;
                    default:
                        predicate = IrInstruction.Predicate.SGE; // >=
                        break;
                }
            }

            result = builder.emitCompare(predicate, result, right);
            result = builder.emitCast(result, IrType.I32);
        }

        return result;
    }

    
}
