package semantic;

import static lexer.TokenType.CHARCON;
import static lexer.TokenType.INTCON;

import java.util.ArrayList;
import java.util.List;

import parser.AstNode;
import semantic.Symbol.Type;
import lexer.*;

public class SemanticAnalyzer {
    private SymbolTable symbolTable = new SymbolTable();
    private int loopDepth = 0; // 用于记录当前是否在循环内
    private int switchDepth = 0; // 用于记录当前是否在 switch 内
    AstNode root;
    List<String> errors;

    public SemanticAnalyzer(AstNode root, List<String> errors) {
        this.root = root;
        this.errors = errors;
        symbolTable.registerRuntimeFunctions();
    }

    public List<Scope> analyeze() {
        analyzeCompUnit(root, errors);
        return symbolTable.getAllScopes();
    }

    private void analyzeCompUnit(AstNode node, List<String> errors) {
        for (AstNode child : node.children) {
            switch (child.getType()) {
                case "Decl": 
                    AstNode tmp = child.getChildren().get(0);

                    if ("ConstDecl".equals(tmp.getType())) {
                        analyzeConstDecl(tmp);
                    }

                    else {
                        analyzeVarDecl(tmp);
                    }
                    break;

                case "FuncDef" : 
                    analyzeFuncDef(child);
                    break;

                case "MainFuncDef" : 
                    analyzeMainFuncDef(child);
                    break;

                default:
                    break;
            }
        }
    }

    private void analyzeConstDecl(AstNode node) {
        Symbol.Type type = null;
        for (AstNode child : node.children) {
            if (child.getType().equals("BType")) {
                AstNode tmp = child.getChildren().get(0);
                if (tmp.getType().equals("INTTK")) {
                    type = Symbol.Type.INT;
                }

                else {
                    type = Symbol.Type.CHAR;
                }
            }

            else if(child.getType().equals("ConstDef")) {
                analyzeConstDef(child, type);
            }
        }
    }

    private void analyzeVarDecl(AstNode node) {
        Symbol.Type type = null;
        boolean isStatic = false;
        for (AstNode child : node.children) {
            if (child.getType().equals("STATICTK")) {
                isStatic = true;
            }

            else if (child.getType().equals("BType")) {
                AstNode tmp = child.getChildren().get(0);
                if (tmp.getType().equals("INTTK")) {
                    type = Symbol.Type.INT;
                }

                else {
                    type = Symbol.Type.CHAR;
                }
            }

            else if(child.getType().equals("VarDef")) {
                analyzeVarDef(child, type, isStatic);
            }
        }
    }

    private void analyzeConstDef(AstNode node, Symbol.Type type) {
        Symbol.Kind kind = Symbol.Kind.CONSTANT;
        boolean isArray = false;
        Token token = node.getChildren().get(0).getToken();
        if (node.getChildren().get(1).getType().equals("LBRACK")) {
            isArray = true;
        }

        if (isArray) {
            if (type == Symbol.Type.INT) {
                type = Symbol.Type.INT_ARRAY;
            }

            else {
                type = Symbol.Type.CHAR_ARRAY;
            }
        }

        Symbol symbol = new Symbol(token.getContent(), token.getNum(), kind, type);

        if (!symbolTable.define(symbol)) {
            errors.add(token.getNum() + " b");
        }

        if (isArray) {
            analyzeConstExp(node.getChildren().get(2));
        }

        analyzeConstInitVal(node.getChildren().get(node.getChildren().size() - 1));
    }

    private void analyzeConstInitVal(AstNode node) {
        for (AstNode child : node.getChildren()) {
            if (child.getType().equals("ConstExp")) {
                analyzeConstExp(child);
            }
        }
    }

    private void analyzeConstExp(AstNode node) {
        AstNode child = node.getChildren().get(0);
        analyzeAddExp(child);
    }

    private void analyzeVarDef(AstNode node, Symbol.Type type, boolean isStatic) {
        Symbol.Kind kind = Symbol.Kind.VARIABLE;
        boolean isArray = false;
        Token token = node.getChildren().get(0).getToken();
        if (node.getChildren().size() > 1 && node.getChildren().get(1).getType().equals("LBRACK")) {
            isArray = true;
        }

        if (isArray) {
            if (type == Symbol.Type.INT) {
                type = Symbol.Type.INT_ARRAY;
            }

            else {
                type = Symbol.Type.CHAR_ARRAY;
            }
        }

        Symbol symbol = new Symbol(token.getContent(), token.getNum(), kind, type, isStatic);

        if (!symbolTable.define(symbol)) {
            errors.add(token.getNum() + " b");
        }

        if (isArray) {
            analyzeConstExp(node.getChildren().get(2));
        }

        AstNode lastNode = node.getChildren().get(node.getChildren().size() - 1);

        if (lastNode.getType().equals("InitVal")) {
            analyzeInitVal(lastNode);
        }

    }

    public void analyzeInitVal(AstNode node) {
        for (AstNode child : node.getChildren()) {
            if (child.getType().equals("Exp")) {
                analyzeExp(child);
            }
        }
    }

    private void analyzeFuncDef(AstNode node) {
        Symbol.Kind kind = Symbol.Kind.FUNCTION;
        AstNode funcTypeNode = node.getChildren().get(0).getChildren().get(0);
        Symbol.Type type = null;
        if (funcTypeNode.getType().equals("INTTK")) {
            type = Symbol.Type.INT;
        }

        else if (funcTypeNode.getType().equals("VOIDTK")){
            type = Symbol.Type.VOID;
        }

        else {
            type = Symbol.Type.CHAR;
        }

        Token token = node.getChildren().get(1).getToken();
        Symbol symbol = new Symbol(token.getContent(), token.getNum(), kind, type);

        if (!symbolTable.define(symbol)) {
            errors.add(token.getNum() + " b");
        }

        symbolTable.enterScope();

        List<Symbol.Type> paramsType = new ArrayList<>();

        AstNode tmp = node.getChildren().get(3);
        if (tmp.getType().equals("FuncFParams")) {
            analyzeFuncFParams(tmp, paramsType);
        }

        symbol.setParameterTypes(paramsType);

        for (AstNode child : node.getChildren()) {
            if (child.getType().equals("Block")) {
                analyzeBlock(child, type);
                checkReturnType(child, type);
            }
        }
        
        symbolTable.exitScope();
    }

    private void analyzeFuncFParams(AstNode node, List<Symbol.Type> paramsType) {
        for (AstNode child : node.children) {
            if (child.getType().equals("FuncFParam")) {
                analyzeFuncFParam(child, paramsType);
            }
        }
    }

    private void analyzeFuncFParam(AstNode node, List<Symbol.Type> paramsType) {
        Symbol.Kind kind = Symbol.Kind.PARAMETER;
        Symbol.Type type = null;
        Token token = node.getChildren().get(1).getToken();
        AstNode tmp = node.getChildren().get(0).getChildren().get(0);
        if (tmp.getType().equals("INTTK")) {
            type = Symbol.Type.INT;
        }

        else {
            type = Symbol.Type.CHAR;
        }

        if (node.getChildren().size() > 2 && "LBRACK".equals(node.getChildren().get(2).getType())) {
            type = type == Symbol.Type.INT
                    ? Symbol.Type.INT_ARRAY
                    : Symbol.Type.CHAR_ARRAY;
        }

        Symbol symbol = new Symbol(token.getContent(), token.getNum(), kind, type);

        if (!symbolTable.define(symbol)) {
            errors.add(token.getNum() + " b");
        }

        paramsType.add(type);
    }

    private void analyzeBlock(AstNode node, Symbol.Type returnType) {
        for (AstNode child : node.getChildren()) {
            if (child.getType().equals("BlockItem")) {
                analyzeBlockItem(child, returnType);
            }
        }
    }

    private void analyzeBlockItem(AstNode node, Symbol.Type returnType) {
        AstNode child = node.getChildren().get(0);
        if (child.getType().equals("Decl")) {
            if (child.getChildren().get(0).getType().equals("VarDecl")) {
                analyzeVarDecl(child.getChildren().get(0));
            }
            else {
                analyzeConstDecl(child.getChildren().get(0));
            }
        }

        else {
            analyzeStmt(child, returnType);
        }
    }

    private void analyzeStmt(AstNode node, Symbol.Type returnType) {
        if (node.getChildren().get(0).getType().equals("LVal")) {
            AstNode lValNode = node.getChildren().get(0);
            AstNode identNode = lValNode.getChildren().get(0);
            Symbol symbol = symbolTable.lookup(identNode.getToken().getContent());
            Symbol.Type type = null;
            boolean isArray = false;


            if (symbol == null) {
                errors.add(identNode.getToken().getNum() + " c");
            } 
            
            else if (symbol.isFunction()) {
                errors.add(identNode.getToken().getNum() + " e");
            }

            else if (symbol.isConst()) {
                errors.add(identNode.getToken().getNum() + " h");
            }

            else {
                type = symbol.getType();
                boolean leftValid = true;

                isArray = lValNode.getChildren().size() > 1
                        && "LBRACK".equals(
                                lValNode.getChildren().get(1).getType());

                if (isArray) {
                    if (type == Symbol.Type.INT || type == Symbol.Type.CHAR) {
                        errors.add(identNode.getToken().getNum() + " e");
                        leftValid = false;
                    }

                    Symbol.Type indexType =
                            analyzeExp(lValNode.getChildren().get(2));

                    if (indexType == null) {
                        leftValid = false;
                    } else if (indexType != Symbol.Type.INT) {
                        if (leftValid) {
                            errors.add(identNode.getToken().getNum() + " e");
                        }
                        leftValid = false;
                    }
                } 
                
                else {
                    if (type == Symbol.Type.INT_ARRAY
                            || type == Symbol.Type.CHAR_ARRAY) {
                        errors.add(identNode.getToken().getNum() + " e");
                        leftValid = false;
                    }
                }

                Symbol.Type rightType = analyzeExp(node.getChildren().get(2));

                // 左侧有效时，才比较赋值左右类型。
                if (leftValid
                        && rightType != null
                        && rightType != degrade(type)) {
                    errors.add(identNode.getToken().getNum() + " e");
                }
            }
        }

        else if (node.getChildren().get(0).getType().equals("Exp")) {
            analyzeExp(node.getChildren().get(0));
        }

        else if (node.getChildren().get(0).getType().equals("Block")) {
            symbolTable.enterScope();
            analyzeBlock(node.getChildren().get(0), returnType);
            symbolTable.exitScope();
        }

        else if (node.getChildren().get(0).getType().equals("IFTK")) {
            AstNode condNode = node.getChildren().get(2);
            analyzeCond(condNode);

            AstNode stmt1Node = node.getChildren().get(4);
            analyzeStmt(stmt1Node, returnType);

            if (node.getChildren().size() > 5) {
                AstNode stmt2Node = node.getChildren().get(6);
                analyzeStmt(stmt2Node, returnType);
            }
        }

        else if (node.getChildren().get(0).getType().equals("WHILETK")) {
            AstNode condNode = node.getChildren().get(2);
            analyzeCond(condNode);

            AstNode stmtNode = node.getChildren().get(4);

            loopDepth++;
            analyzeStmt(stmtNode, returnType);
            loopDepth--;
        }

        else if (node.getChildren().get(0).getType().equals("SWITCHTK")) {
            AstNode expNode = node.getChildren().get(2);
            Symbol.Type curType = analyzeExp(expNode);
            List<String> stringList = new ArrayList<>();

            if (curType != Symbol.Type.INT && curType != Symbol.Type.CHAR) {
                errors.add(getFirstLine(expNode) + " e");
            }

            switchDepth++;

            for (AstNode child : node.getChildren()) {
                if ("CaseStmt".equals(child.getType())) {
                    String num = analyzeCaseStmt(child, curType, returnType);

                    if (stringList.contains(num)) {
                        errors.add(getFirstLine(child) + " n");
                    }

                    else {
                        stringList.add(num);
                    }
                }
            }

            switchDepth--;
        }

        else if (node.getChildren().get(0).getType().equals("BREAKTK")) {
            if (loopDepth == 0 && switchDepth == 0) {
                errors.add(node.getChildren().get(0).getToken().getNum() + " m");
            }
        }

        else if (node.getChildren().get(0).getType().equals("CONTINUETK")) {
            if (loopDepth == 0) {
                errors.add(node.getChildren().get(0).getToken().getNum() + " m");
            }
        }

        else if (node.getChildren().get(0).getType().equals("RETURNTK")) {
            List<AstNode> child = node.getChildren();
            Symbol.Type type = Symbol.Type.VOID;
            boolean hasExp = child.get(1).getType().equals("Exp");
            if (hasExp) {
                type = analyzeExp(child.get(1));
            }

            if (type == null) {
                return;
            }

            if (returnType == Symbol.Type.VOID && hasExp) {
                errors.add(child.get(0).getToken().getNum() + " f");
            }

            else if (returnType != Symbol.Type.VOID && returnType != type) {
                errors.add(child.get(0).getToken().getNum() + " e");
            }
        }

        else if (node.getChildren().get(0).getType().equals("PRINTFTK")){
            List<AstNode> child = node.getChildren();
            AstNode stringConstNode = child.get(2);
            List<Symbol.Type> typeList = analyzeStringConst(stringConstNode);
            List<Symbol.Type> actualList = new ArrayList<>();
            for (AstNode expNode : child) {
                if (expNode.getType().equals("Exp")) {
                    Symbol.Type type = analyzeExp(expNode);
                    actualList.add(type);
                }
            }

            if (typeList.size() != actualList.size()) {
                errors.add(child.get(0).getToken().getNum() + " l");
            }

            else if (actualList.contains(null)) {
                return;
            }

            else if (!typeList.equals(actualList)) {
                errors.add(child.get(0).getToken().getNum() + " e");
            }
        }

    }

    private List<Symbol.Type> analyzeStringConst(AstNode node) {
        List<Symbol.Type> types = new ArrayList<>();
        String content = node.getToken().getContent();

        for (int i = 0; i + 1 < content.length(); i++) {
            if (content.charAt(i) == '%') {
                if (content.charAt(i + 1) == 'd') {
                    types.add(Symbol.Type.INT);
                }
                
                else if (content.charAt(i + 1) == 'c') {
                    types.add(Symbol.Type.CHAR);
                }

                else if (content.charAt(i + 1) == 's') {
                    types.add(Symbol.Type.CHAR_ARRAY);
                }
            }
        }

        return types;
    }

    private String analyzeCaseStmt(AstNode node, Symbol.Type curType, Symbol.Type returnType) {
        List<AstNode> child = node.getChildren();
        String num;
        if (child.get(0).getType().equals("CASETK")) {
            AstNode numberNode = child.get(1);

            if (numberNode.getChildren().get(0).getToken().getType() == INTCON) {
                if (curType == Symbol.Type.CHAR) {
                    errors.add(numberNode.getChildren().get(0).getToken().getNum() + " e");
                }
            }

            else {
                if (curType == Symbol.Type.INT) {
                    errors.add(numberNode.getChildren().get(0).getToken().getNum() + " e");
                }
            }

            num = numberNode.getChildren().get(0).getToken().getContent();
        }

        else{
            num = "default";
        }

        for (AstNode stmtNode : node.getChildren()) {
            if (stmtNode.getType().equals("Stmt")) {
                analyzeStmt(stmtNode, returnType);
            }
        }

        return num;
    }

    private void analyzeCond(AstNode node) {
        analyzeLOrExp(node.getChildren().get(0));
    }

    private Symbol.Type analyzeLOrExp(AstNode node) {
        List<AstNode> children = node.getChildren();

        for (int i = 0; i < children.size(); i += 2) {
            if (analyzeLAndExp(children.get(i)) == null) {
                return null;
            }
        }

        return Symbol.Type.INT;
    }

    private Symbol.Type analyzeLAndExp(AstNode node) {
        List<AstNode> child = node.getChildren();
        Symbol.Type curType = analyzeEqExp(child.get(0));
        if (curType == null) {
            return null;
        }

        if (curType != Symbol.Type.INT && curType != Symbol.Type.CHAR) {
            errors.add(getFirstLine(child.get(0)) + " e");
            return null;
        }

        for (int i = 2; i < child.size(); i = i + 2) {
            Symbol.Type type = analyzeEqExp(child.get(i));
            if (type == null) {
                return null;
            }

            if (type != Symbol.Type.INT && type != Symbol.Type.CHAR) {
                errors.add(getFirstLine(child.get(i)) + " e");
                return null;
            }

            if (curType != type) {
                Token operator = child.get(i - 1).getToken();
                errors.add(operator.getNum() + " e");
                return null;
            }

            curType = Symbol.Type.INT;

        }

        return Symbol.Type.INT;
    }

    private Symbol.Type analyzeEqExp(AstNode node) {
        List<AstNode> child = node.getChildren();
        Symbol.Type curType = analyzeRelExp(child.get(0));
        if (curType == null) {
            return null;
        }

        if (curType != Symbol.Type.INT && curType != Symbol.Type.CHAR) {
            errors.add(getFirstLine(child.get(0)) + " e");
            return null;
        }

        for (int i = 2; i < child.size(); i = i + 2) {
            Symbol.Type type = analyzeRelExp(child.get(i));
            if (type == null) {
                return null;
            }

            if (type != Symbol.Type.INT && type != Symbol.Type.CHAR) {
                errors.add(getFirstLine(child.get(i)) + " e");
                return null;
            }

            if (curType != type) {
                Token operator = child.get(i - 1).getToken();
                errors.add(operator.getNum() + " e");
                return null;
            }

            curType = Symbol.Type.INT;
        }

        return curType;

    }

    private Symbol.Type analyzeRelExp(AstNode node) {
        List<AstNode> child = node.getChildren();
        Symbol.Type curType = analyzeAddExp(child.get(0));
        if (curType == null) {
            return null;
        }

        if (curType != Symbol.Type.INT && curType != Symbol.Type.CHAR) {
            errors.add(getFirstLine(child.get(0)) + " e");
            return null;
        }

        for (int i = 2; i < child.size(); i = i + 2) {
            Symbol.Type type = analyzeAddExp(child.get(i));
            if (type == null) {
                return null;
            }

            if (type != Symbol.Type.INT && type != Symbol.Type.CHAR) {
                errors.add(getFirstLine(child.get(i)) + " e");
                return null;
            }

            if (curType != type) {
                Token operator = child.get(i - 1).getToken();
                errors.add(operator.getNum() + " e");
                return null;
            }

            curType = Symbol.Type.INT;
        }

        return curType;

    }



    private Symbol.Type analyzeExp(AstNode node) {
        AstNode child = node.getChildren().get(0);
        Symbol.Type type = analyzeAddExp(child);
        if (type == null) {
            return null;
        }
        return type;
    }

    private Symbol.Type analyzeAddExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        Symbol.Type curType = analyzeMulExp(children.get(0));
        
        if (curType == null) {
            return null;
        }

        for (int i = 2; i < children.size(); i = i + 2) {
            Symbol.Type type = analyzeMulExp(children.get(i));

            if (type == null) {
                return null;
            }

            if (curType != type || (curType != Symbol.Type.INT && curType != Symbol.Type.CHAR)) {
                Token operator = children.get(i - 1).getToken();
                errors.add(operator.getNum() + " e");
                return null;
            }
        }

        return curType;
    }

    private Symbol.Type analyzeMulExp(AstNode node) {
        List<AstNode> children = node.getChildren();
        Symbol.Type curType = analyzeUnaryExp(children.get(0));
        
        if (curType == null) {
            return null;
        }

        for (int i = 2; i < children.size(); i = i + 2) {
            Symbol.Type type = analyzeUnaryExp(children.get(i));

            if (type == null) {
                return null;
            }

            if (curType != type || (curType != Symbol.Type.INT && curType != Symbol.Type.CHAR)) {
                Token operator = children.get(i - 1).getToken();
                errors.add(operator.getNum() + " e");
                return null;
            }
        }

        return curType;
    }

    private Symbol.Type analyzeUnaryExp(AstNode node) {
        List<AstNode> child = node.getChildren();
        if (child.get(0).getType().equals("PrimaryExp")) {
            return analyzePrimaryExp(child.get(0));
        }

        else if (child.get(0).getType().equals("IDENFR")) {
            AstNode identNode = child.get(0);
            Symbol symbol = symbolTable.lookup(identNode.getToken().getContent());
            if (symbol == null) {
                errors.add(identNode.getToken().getNum() + " c");
                return null;
            }

            if (symbol.getKind() != Symbol.Kind.FUNCTION) {
                errors.add(identNode.getToken().getNum() + " e");
                return null;
            }

            List<Symbol.Type> formalTypes = symbol.getParameterTypes();

            List<Symbol.Type> actualTypes = new ArrayList<>();

            if ("FuncRParams".equals(child.get(2).getType())) {
                actualTypes = analyzeFuncRParams(child.get(2));
            }

            int lineNum = identNode.getToken().getNum();

            if (formalTypes.size() != actualTypes.size()) {
                errors.add(lineNum + " d");
                return null;
            }

            if (actualTypes.contains(null)) {
                return null;
            }

            if (!formalTypes.equals(actualTypes)) {
                errors.add(lineNum + " e");
                return null;
            }

            return symbol.getType();
        }

        else if (child.get(0).getType().equals("UnaryOp")) {
            Symbol.Type operandType = analyzeUnaryExp(child.get(1));
            if (operandType == null) {
                return null;
            }

            AstNode operator = child.get(0).getChildren().get(0);

            // 单目运算的操作数必须是基本类型。
            if (operandType != Symbol.Type.INT && operandType != Symbol.Type.CHAR) {
                errors.add(operator.getToken().getNum() + " e");
                return null;
            }

            // !的结果为INT。
            if ("NOT".equals(operator.getType())) {
                return Symbol.Type.INT;
            }

            return operandType;
        }

        else {
            AstNode bTypeNode = child.get(1);
            AstNode unaryExpNode = child.get(3);

            Symbol.Type operandType = analyzeUnaryExp(unaryExpNode);
            if (operandType == null) {
                return null;
            }

            if ("INTTK".equals(bTypeNode.getChildren().get(0).getType())) {
                return Symbol.Type.INT;
            } 
            
            else {
                return Symbol.Type.CHAR;
            }
        }
    }

    private List<Symbol.Type> analyzeFuncRParams(AstNode node) {
        List<Symbol.Type> typeList = new ArrayList<>();
        List<AstNode> child = node.getChildren();
        for (int i = 0; i < child.size(); i = i + 2) {
            typeList.add(analyzeExp(child.get(i)));
        }
        return typeList;
    }

    private Symbol.Type analyzePrimaryExp(AstNode node) {
        List<AstNode> child = node.getChildren();
        if (child.get(0).getType().equals("LVal")) {
            AstNode identNode = child.get(0).getChildren().get(0);
            Symbol symbol = symbolTable.lookup(identNode.getToken().getContent());
            if (symbol == null) {
                errors.add(identNode.getToken().getNum() + " c");
                return null;
            }

            if (symbol.isFunction()) {
                errors.add(identNode.getToken().getNum() + " e");
                return null;
            }

            boolean isArray = false;
            Symbol.Type type = symbol.getType();
            if (child.get(0).getChildren().size() > 1) {
                isArray = true;
                Symbol.Type type2 = analyzeExp((child.get(0).getChildren().get(2)));

                if (type2 == null) {
                    return null;
                }

                if (type2 != Symbol.Type.INT) {
                    errors.add(child.get(0).getChildren().get(1).getToken().getNum() + " e");
                    return null;
                }

                if (symbol.getType() != Symbol.Type.INT_ARRAY && symbol.getType() != Symbol.Type.CHAR_ARRAY) {
                    errors.add(child.get(0).getChildren().get(1).getToken().getNum() + " e");
                    return null;
                }
            }

            if (isArray) {
                return degrade(type);
            }

            return type;
        }

        else if (child.get(0).getType().equals("Number")) {
            AstNode numberNode = child.get(0);
            if (numberNode.getChildren().get(0).getToken().getType() == INTCON) {
                return Symbol.Type.INT;
            }

            return Symbol.Type.CHAR;
        }

        else {
            AstNode expNode = child.get(1);
            return analyzeExp(expNode);
        }
    }

    private void analyzeMainFuncDef(AstNode node) {
        symbolTable.enterScope();;

        analyzeBlock(node.getChildren().get(4), Symbol.Type.INT);
        checkReturnType(node.getChildren().get(4), Symbol.Type.INT);

        symbolTable.exitScope();
    }

    private Symbol.Type degrade(Symbol.Type type) {
        if (type == Symbol.Type.INT_ARRAY) {
            return Symbol.Type.INT;
        }

        else if (type == Symbol.Type.CHAR_ARRAY) {
            return Symbol.Type.CHAR;
        }

        else {
            return type;
        }
    }

    private int getFirstLine(AstNode node) {
        while (node.getToken() == null) {
            node = node.getChildren().get(0);
        }
        return node.getToken().getNum();
    }

    private void checkReturnType(AstNode node, Symbol.Type type) {
        if (type == Symbol.Type.VOID) {
            return;
        }

        List<AstNode> children = node.getChildren();
        int size = children.size();
        boolean hasReturn = false;

        // 空 Block 没有 BlockItem
        if (size > 2) {
            AstNode lastBlockItem = children.get(size - 2);
            AstNode lastItem = lastBlockItem.getChildren().get(0);

            hasReturn = lastItem.getType().equals("Stmt")
                    && lastItem.getChildren().get(0)
                            .getType().equals("RETURNTK");
        }

        if (!hasReturn) {
            errors.add(children.get(size - 1).getToken().getNum() + " g");
        }
    }
}
