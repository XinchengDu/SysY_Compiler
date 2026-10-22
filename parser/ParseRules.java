package parser;
import static parser.PaserContext.*;

import static lexer.TokenType.CHARTK;
import static lexer.TokenType.INTTK;
import static lexer.TokenType.LPARENT;
import static parser.GrammarJudge.*;

import lexer.Token;
import lexer.TokenType;

public class ParseRules {
    Token expect(TokenType tokenType) {
        if (index >= length) {
            //抛出越界错误
            throw new IllegalStateException(
                "Token 越界"
            );
        }

        Token current = getCurToken();
        if (current.getType() != tokenType) {
            throw new IllegalStateException(
                "与期望不符"
            );
        }

        output.add(current.getType().name() + " " + current.getContent());
        index++;
        return current;
    }

    /*
    分析非终结符方法
    */

    public AstNode parseCompUnit() {
        // 编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
        AstNode compUnitNode = new AstNode("CompUnit");

        while (isDecl()) {
            compUnitNode.addChildren(parseDecl());
        }

        while (isFuncDef()) {
            compUnitNode.addChildren(parseFuncDef());
        }

        compUnitNode.addChildren(parseMainFuncDef());

        output.add("<CompUnit>");
        return compUnitNode;
    }

    public AstNode parseDecl() {
        // 声明 Decl → ConstDecl | VarDecl
        AstNode declNode = new AstNode("Decl");

        if (isConstDecl()) {
            declNode.addChildren(parseConstDecl());
        }

        else {
            declNode.addChildren(parseVarDecl());
        }

        return declNode;
    }

    public AstNode parseConstDecl() {
        // 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';' // i
        AstNode constDeclNode = new AstNode("ConstDecl");

        constDeclNode.addChildren(parseCONSTTK());
        constDeclNode.addChildren(parseBType());
        constDeclNode.addChildren(parseConstDef());

        while (isCOMMA()) {
            constDeclNode.addChildren(parseCOMMA());
            constDeclNode.addChildren(parseConstDef());
        }

        if (isSEMICN()) {
            constDeclNode.addChildren(parseSEMICN());
        }
        else {
            int line = tokens.get(index - 1).getNum();
            Token missing = new Token(TokenType.SEMICN, ";", line);
            AstNode iErrorNode = new AstNode(missing);
            iErrorNode.isIError = true;
            constDeclNode.addChildren(iErrorNode);
            
        }

        output.add("<ConstDecl>");
        return constDeclNode;
    }

    public AstNode parseBType() {
        // 基本类型 BType → 'int' | 'char'
        AstNode bTypNode = new AstNode("BType");

        if (isINTTK()) {
            bTypNode.addChildren(parseINTTK());
        }

        else {
            bTypNode.addChildren(parseCHARTK());
        }

        return bTypNode;
    }

    public AstNode parseConstDef() {
        // 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal // b, k
        AstNode constDefNode = new AstNode("ConstDef");

        constDefNode.addChildren(parseIdent());

        if (isLBRACK()) {
            constDefNode.addChildren(parseLBRACK());

            constDefNode.addChildren(parseConstExp());

            if (isRBRACK()) {
                constDefNode.addChildren(parseRBRACK());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RBRACK, "]", line);
                AstNode kErrorNode = new AstNode(missing);
                kErrorNode.isKError = true;
                constDefNode.addChildren(kErrorNode);
            }
        }

        constDefNode.addChildren(parseASSIGN());
        constDefNode.addChildren(parseConstInitVal());

        output.add("<ConstDef>");
        return constDefNode;
    }

    public AstNode parseConstInitVal() {
        // 常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}' | StringConst
        AstNode constInitvalNode = new AstNode("ConstInitVal");

        if (isStringConst()) {
            constInitvalNode.addChildren(parseStringConst());
        }
        
        else if (!isLBRACE()) {
            constInitvalNode.addChildren(parseConstExp());
        }

        else {
            constInitvalNode.addChildren(parseLBRACE());

            if (!isRBRACE()) {
                constInitvalNode.addChildren(parseConstExp());

                while (isCOMMA()) {
                    constInitvalNode.addChildren(parseCOMMA());
                    constInitvalNode.addChildren(parseConstExp());
                }
            }

            constInitvalNode.addChildren(parseRBRACE());
        }

        output.add("<ConstInitVal>");
        return constInitvalNode;
    }

    public AstNode parseVarDecl() {
        // 变量声明 VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';' // i
        AstNode varDeclNode = new AstNode("VarDecl");

        if (isSTATICTK()) {
            varDeclNode.addChildren(parseSTATICTK());
        }

        varDeclNode.addChildren(parseBType());
        varDeclNode.addChildren(parseVarDef());

        while (isCOMMA()) {
            varDeclNode.addChildren(parseCOMMA());
            varDeclNode.addChildren(parseVarDef());
        }

        if (isSEMICN()) {
            varDeclNode.addChildren(parseSEMICN());
        }

        else {
            int line = tokens.get(index - 1).getNum();
            Token missing = new Token(TokenType.SEMICN, ";", line);
            AstNode iErrorNode = new AstNode(missing);
            iErrorNode.isIError = true;
            varDeclNode.addChildren(iErrorNode);
        }

        output.add("<VarDecl>");
        return varDeclNode;
    }

    public AstNode parseVarDef() {
        // 变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal // b, k
        AstNode varDefNode = new AstNode("VarDef");

        varDefNode.addChildren(parseIdent());

        if (isLBRACK()) {
            varDefNode.addChildren(parseLBRACK());
            varDefNode.addChildren(parseConstExp());
            if (isRBRACK()) {
                varDefNode.addChildren(parseRBRACK());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RBRACK, "]", line);
                AstNode kErrorNode = new AstNode(missing);
                kErrorNode.isKError = true;
                varDefNode.addChildren(kErrorNode);
            }
        }

        if (isASSIGN()) {
            varDefNode.addChildren(parseASSIGN());
            varDefNode.addChildren(parseInitVal());
        }

        output.add("<VarDef>");
        return varDefNode;
    }

    public AstNode parseInitVal() {
        // 变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst
        AstNode initValNode = new AstNode("InitVal");

        if (isStringConst()) {
            initValNode.addChildren(parseStringConst());
        }

        else if (!isLBRACE()) {
            initValNode.addChildren(parseExp());
        }

        else {
            initValNode.addChildren(parseLBRACE());

            if (!isRBRACE()) {
                initValNode.addChildren(parseExp());

                while (isCOMMA()) {
                    initValNode.addChildren(parseCOMMA());
                    initValNode.addChildren(parseExp());
                }
            }

            initValNode.addChildren(parseRBRACE());
        }

        output.add("<InitVal>");
        return initValNode;
    }

    public AstNode parseFuncDef() {
        // 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block // b, e, g, j
        AstNode funcDefNode = new AstNode("FuncDef");

        funcDefNode.addChildren(parseFuncType());
        funcDefNode.addChildren(parseIdent());
        funcDefNode.addChildren(parseLPARENT());

        if (isBType()) {
            funcDefNode.addChildren(parseFuncFParams());
        }

        if (isRPARENT()) {
            funcDefNode.addChildren(parseRPARENT());
        }

        else {
            int line = tokens.get(index - 1).getNum();
            Token missing = new Token(TokenType.RPARENT, ")", line);
            AstNode jErrorNode = new AstNode(missing);
            jErrorNode.isJError = true;
            funcDefNode.addChildren(jErrorNode);
        }

        funcDefNode.addChildren(parseBlock());

        output.add("<FuncDef>");
        return funcDefNode;
    }

    public AstNode parseMainFuncDef() {
        // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block // g, j
        AstNode mainFuncDefNode = new AstNode("MainFuncDef");

        mainFuncDefNode.addChildren(parseINTTK());
        mainFuncDefNode.addChildren(parseMAINTK());
        mainFuncDefNode.addChildren(parseLPARENT());

        if (isRPARENT()) {
            mainFuncDefNode.addChildren(parseRPARENT());
        }

        else {
            int line = tokens.get(index - 1).getNum();
            Token missing = new Token(TokenType.RPARENT, ")", line);
            AstNode jErrorNode = new AstNode(missing);
            jErrorNode.isJError = true;
            mainFuncDefNode.addChildren(jErrorNode);
        }

        mainFuncDefNode.addChildren(parseBlock());

        output.add("<MainFuncDef>");
        return mainFuncDefNode;
    }

    public AstNode parseFuncType() {
        // 函数类型 FuncType → 'void' | 'int' | 'char'
        AstNode funcTypeNode = new AstNode("FuncType");
        if (isINTTK()) {
            funcTypeNode.addChildren(parseINTTK());
        }

        else if (isCHARTK()) {
            funcTypeNode.addChildren(parseCHARTK());
        }

        else {
            funcTypeNode.addChildren(parseVOIDTK());
        }

        output.add("<FuncType>");
        return funcTypeNode;
    }

    public AstNode parseFuncFParams() {
        // 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
        AstNode funcFParamsNode = new AstNode("FuncFParams");
        funcFParamsNode.addChildren(parseFuncFParam());

        while (isCOMMA()) {
            funcFParamsNode.addChildren(parseCOMMA());
            funcFParamsNode.addChildren(parseFuncFParam());
        }

        output.add("<FuncFParams>");
        return funcFParamsNode;
    }

    public AstNode parseFuncFParam() {
        // 函数形参 FuncFParam → BType Ident ['[' ']'] // b, k
        AstNode funcFParamNode = new AstNode("FuncFParam");
        funcFParamNode.addChildren(parseBType());
        funcFParamNode.addChildren(parseIdent());

        if (isLBRACK()) {
            funcFParamNode.addChildren(parseLBRACK());

            if (isRBRACK()) {
                funcFParamNode.addChildren(parseRBRACK());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RBRACK, "]", line);
                AstNode kErrorNode = new AstNode(missing);
                kErrorNode.isKError = true;
                funcFParamNode.addChildren(kErrorNode);
            }
        }

        output.add("<FuncFParam>");
        return funcFParamNode;
    }

    public AstNode parseBlock() {
        // 语句块 Block → '{' { BlockItem } '}'
        AstNode blockNode = new AstNode("Block");
        blockNode.addChildren(parseLBRACE());
        while (!isRBRACE()) {
            blockNode.addChildren(parseBlockItem());
        }
        blockNode.addChildren(parseRBRACE());

        output.add("<Block>");
        return blockNode;
    }

    public AstNode parseBlockItem() {
        // 语句块项 BlockItem → Decl | Stmt
        AstNode blockItemNode = new AstNode("BlockItem");
        if (isCONSTTK() || isSTATICTK() || isBType()) {
            blockItemNode.addChildren(parseDecl());
        }

        else {
            blockItemNode.addChildren(parseStmt());
        }

        return blockItemNode;
    }

    public AstNode parseStmt() {
         /* 语句 Stmt → LVal '=' Exp ';' // e, h, i
         [Exp] ';' // i
         Block
         'if' '(' Cond ')' Stmt [ 'else' Stmt ] // j
         'while' '(' Cond ')' Stmt // j
         'switch' '(' Exp ')' '{' { CaseStmt } '}' // j
         'break' ';' | 'continue' ';' // i, m
         'return' [Exp] ';' // e, f, i
         'printf' '(' StringConst { ',' Exp } ')' ';' // e, i, j, l
         */

        // 此处 Exp *→ LVal ，需单独区分
        
        AstNode stmtNode = new AstNode("Stmt");
        if (isLBRACE()) {
            stmtNode.addChildren(parseBlock());
        }

        else if (isIFTK()) {
            stmtNode.addChildren(parseIFTK());
            stmtNode.addChildren(parseLPARENT());
            stmtNode.addChildren(parseCond());
            if (isRPARENT()) {
                stmtNode.addChildren(parseRPARENT());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RPARENT, ")", line);
                AstNode jErrorNode = new AstNode(missing);
                jErrorNode.isJError = true;
                stmtNode.addChildren(jErrorNode);
            }

            stmtNode.addChildren(parseStmt());
            if (isELSETK()) {
                stmtNode.addChildren(parseELSETK());
                stmtNode.addChildren(parseStmt());
            }
        }

        else if (isWHILETK()) {
            stmtNode.addChildren(parseWHILETK());
            stmtNode.addChildren(parseLPARENT());
            stmtNode.addChildren(parseCond());
            if (isRPARENT()) {
                stmtNode.addChildren(parseRPARENT());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RPARENT, ")", line);
                AstNode jErrorNode = new AstNode(missing);
                jErrorNode.isJError = true;
                stmtNode.addChildren(jErrorNode);
            }

            stmtNode.addChildren(parseStmt());
        }

        else if (isSWITCHTK()) {
            stmtNode.addChildren(parseSWITCHTK());
            stmtNode.addChildren(parseLPARENT());
            stmtNode.addChildren(parseExp());
            if (isRPARENT()) {
                stmtNode.addChildren(parseRPARENT());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RPARENT, ")", line);
                AstNode jErrorNode = new AstNode(missing);
                jErrorNode.isJError = true;
                stmtNode.addChildren(jErrorNode);
            }

            stmtNode.addChildren(parseLBRACE());
            while (!isRBRACE()) {
                stmtNode.addChildren(parseCaseStmt());
            }
            stmtNode.addChildren(parseRBRACE());
        }

        else if (isBREAKTK()) {
            stmtNode.addChildren(parseBREAKTK());
            if (isSEMICN()) {
                stmtNode.addChildren(parseSEMICN());
            }
            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.SEMICN, ";", line);
                AstNode iErrorNode = new AstNode(missing);
                iErrorNode.isIError = true;
                stmtNode.addChildren(iErrorNode);
                
            }
        }

        else if (isCONTINUETK()) {
            stmtNode.addChildren(parseCONTINUETK());
            if (isSEMICN()) {
                stmtNode.addChildren(parseSEMICN());
            }
            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.SEMICN, ";", line);
                AstNode iErrorNode = new AstNode(missing);
                iErrorNode.isIError = true;
                stmtNode.addChildren(iErrorNode);
                
            }
        }

        else if (isRETURNTK()) {
            stmtNode.addChildren(parseRETURNTK());
            if (isLPARENT() || isIdent() || isNumber() || isUnaryOp()) {
                int tmp = index;
                int outputSize = output.size();

                AstNode expNode = parseExp();
                stmtNode.addChildren(expNode);

                if (isASSIGN()) {
                    stmtNode.extract(expNode, tmp, outputSize);
                }
            }

            if (isSEMICN()) {
                stmtNode.addChildren(parseSEMICN());
            }
            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.SEMICN, ";", line);
                AstNode iErrorNode = new AstNode(missing);
                iErrorNode.isIError = true;
                stmtNode.addChildren(iErrorNode);
                
            }
        }

        else if (isPRINTFTK()) {
            stmtNode.addChildren(parsePRINTFTK());
            stmtNode.addChildren(parseLPARENT());
            stmtNode.addChildren(parseStringConst());
            while (isCOMMA()) {
                stmtNode.addChildren(parseCOMMA());
                stmtNode.addChildren(parseExp());
            }

            if (isRPARENT()) {
                stmtNode.addChildren(parseRPARENT());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RPARENT, ")", line);
                AstNode jErrorNode = new AstNode(missing);
                jErrorNode.isJError = true;
                stmtNode.addChildren(jErrorNode);
            }

            if (isSEMICN()) {
                stmtNode.addChildren(parseSEMICN());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.SEMICN, ";", line);
                AstNode iErrorNode = new AstNode(missing);
                iErrorNode.isIError = true;
                stmtNode.addChildren(iErrorNode);
            }
        }

        else if (isExp()) {
            AstNode expNode = parseExp();
            stmtNode.addChildren(expNode);

            if (isASSIGN()) {
                stmtNode.extractLValFromExp(expNode); // 待补充
                stmtNode.addChildren(parseASSIGN());
                stmtNode.addChildren(parseExp());

                if (isSEMICN()) {
                    stmtNode.addChildren(parseSEMICN());
                }
                else {
                    int line = tokens.get(index - 1).getNum();
                    Token missing = new Token(TokenType.SEMICN, ";", line);
                    AstNode iErrorNode = new AstNode(missing);
                    iErrorNode.isIError = true;
                    stmtNode.addChildren(iErrorNode);
                    
                }
            }

            else {
                if (isSEMICN()) {
                    stmtNode.addChildren(parseSEMICN());
                }
                else {
                    int line = tokens.get(index - 1).getNum();
                    Token missing = new Token(TokenType.SEMICN, ";", line);
                    AstNode iErrorNode = new AstNode(missing);
                    iErrorNode.isIError = true;
                    stmtNode.addChildren(iErrorNode);
                    
                }
            }
        }

        else {
            if (isSEMICN()) {
                stmtNode.addChildren(parseSEMICN());
            }
            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.SEMICN, ";", line);
                AstNode iErrorNode = new AstNode(missing);
                iErrorNode.isIError = true;
                stmtNode.addChildren(iErrorNode);
                
            }
        }

        output.add("<Stmt>");
        return stmtNode;
    }

    public AstNode parseCaseStmt() {
        // Case语句 CaseStmt → 'case' Number ':' { Stmt } | 'default' ':' { Stmt } // e, n
        AstNode caseStmtNode = new AstNode("CaseStmt");

        if (isCASETK()) {
            caseStmtNode.addChildren(parseCASETK());
            caseStmtNode.addChildren(parseNumber());
            caseStmtNode.addChildren(parseCOLON());

            while (!(isRBRACE() || isCASETK() || isDEFAULTTK())) {
                caseStmtNode.addChildren(parseStmt());
            }
        }

        else {
            caseStmtNode.addChildren(parseDEFAULTTK());
            caseStmtNode.addChildren(parseCOLON());

            while (!(isRBRACE() || isCASETK() || isDEFAULTTK())) {
                caseStmtNode.addChildren(parseStmt());
            }
        }

        output.add("<CaseStmt>");
        return caseStmtNode;
    }

    public AstNode parseExp() {
        // 表达式 Exp → AddExp
        AstNode expNode = new AstNode("Exp");

        expNode.addChildren(parseAddExp());

        output.add("<Exp>");
        return expNode;
    }

    public AstNode parseCond() {
        // 条件表达式 Cond → LOrExp
        AstNode condNode = new AstNode("Cond");

        condNode.addChildren(parseLOrExp());

        output.add("<Cond>");
        return condNode;
    }

    public AstNode parseLVal() {
        // 左值表达式 LVal → Ident ['[' Exp ']'] // c, k
        AstNode lValNode = new AstNode("LVal");

        lValNode.addChildren(parseIdent());

        if (isLBRACK()) {
            lValNode.addChildren(parseLBRACK());
            lValNode.addChildren(parseExp());

            if (isRBRACK()) {
                lValNode.addChildren(parseRBRACK());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RBRACK, "]", line);
                AstNode kErrorNode = new AstNode(missing);
                kErrorNode.isKError = true;
                lValNode.addChildren(kErrorNode);
            }
        }

        output.add("<LVal>");
        return lValNode;
    }

    public AstNode parsePrimaryExp() {
        // 基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number // j
        AstNode primaryExpNode = new AstNode("PrimaryExp");
        if (isLPARENT()) {
            primaryExpNode.addChildren(parseLPARENT());
            primaryExpNode.addChildren(parseExp());

            if (isRPARENT()) {
                primaryExpNode.addChildren(parseRPARENT());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RPARENT, ")", line);
                AstNode jErrorNode = new AstNode(missing);
                jErrorNode.isJError = true;
                primaryExpNode.addChildren(jErrorNode);
            }
        }

        else if (isNumber()) {
            primaryExpNode.addChildren(parseNumber());
        }

        else {
            primaryExpNode.addChildren(parseLVal());
        }

        output.add("<PrimaryExp>");
        return primaryExpNode;
    }

    public AstNode parseNumber() {
        // 数值 Number → IntConst | CharConst
        AstNode numberNode = new AstNode("Number");
        if (isINTCON()) {
            numberNode.addChildren(parseINTCON());
        }

        else {
            numberNode.addChildren(parseCHARCON());
        }

        output.add("<Number>");
        return numberNode;
    }

    public AstNode parseUnaryExp() {
        // ⼀元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp | '(' BType ')' UnaryExp // c, d, e, j
        AstNode unaryExpNode = new AstNode("UnaryExp");

        if (isIdent() && getFollowToken(1).getType() == LPARENT) {
            unaryExpNode.addChildren(parseIdent());
            unaryExpNode.addChildren(parseLPARENT());

            if (isExp()) {
                unaryExpNode.addChildren(parseFuncRParams());
            }

            if (isRPARENT()) {
                unaryExpNode.addChildren(parseRPARENT());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RPARENT, ")", line);
                AstNode jErrorNode = new AstNode(missing);
                jErrorNode.isJError = true;
                unaryExpNode.addChildren(jErrorNode);
            }
        }

        else if (isUnaryOp()) {
            unaryExpNode.addChildren(parseUnaryOp());
            unaryExpNode.addChildren(parseUnaryExp());
        }

        else if (isLPARENT() && (getFollowToken(1).getType() == INTTK || getFollowToken(1).getType() == CHARTK)) {
            unaryExpNode.addChildren(parseLPARENT());
            unaryExpNode.addChildren(parseBType());

            if (isRPARENT()) {
                unaryExpNode.addChildren(parseRPARENT());
            }

            else {
                int line = tokens.get(index - 1).getNum();
                Token missing = new Token(TokenType.RPARENT, ")", line);
                AstNode jErrorNode = new AstNode(missing);
                jErrorNode.isJError = true;
                unaryExpNode.addChildren(jErrorNode);
            }

            unaryExpNode.addChildren(parseUnaryExp());
        }

        else {
            unaryExpNode.addChildren(parsePrimaryExp());
        }

        output.add("<UnaryExp>");
        return unaryExpNode;
    }

    public AstNode parseUnaryOp() {
        // 单⽬运算符 UnaryOp → '+' | '−' | '!' 注：'!'仅出现在条件表达式中
        AstNode unaryOpNode = new AstNode("UnaryOp");

        if (isPLUS()) {
            unaryOpNode.addChildren(parsePLUS());
        }

        else if (isMINU()) {
            unaryOpNode.addChildren(parseMINU());
        }

        else {
            unaryOpNode.addChildren(parseNOT());
        }

        output.add("<UnaryOp>");
        return unaryOpNode;
    }

    public AstNode parseFuncRParams() {
        // 函数实参表 FuncRParams → Exp { ',' Exp }
        AstNode funcRParamsNode = new AstNode("FuncRParams");

        funcRParamsNode.addChildren(parseExp());

        while (isCOMMA()) {
            funcRParamsNode.addChildren(parseCOMMA());
            funcRParamsNode.addChildren(parseExp());
        }

        output.add("<FuncRParams>");
        return funcRParamsNode;
    }

    public AstNode parseMulExp() {
        // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        AstNode mulExpNode = new AstNode("MulExp");

        mulExpNode.addChildren(parseUnaryExp());

        while (isMULT() || isDIV() || isMOD()) {
            output.add("<MulExp>");

            if (isMULT()) {
                mulExpNode.addChildren(parseMULT());
            }

            else if (isDIV()) {
                mulExpNode.addChildren(parseDIV());
            }

            else {
                mulExpNode.addChildren(parseMOD());
            }

            mulExpNode.addChildren(parseUnaryExp());
        }

        output.add("<MulExp>");
        return mulExpNode;
    }

    public AstNode parseAddExp() {
        // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
        AstNode addExpNode = new AstNode("AddExp");

        addExpNode.addChildren(parseMulExp());

        while (isPLUS() || isMINU()) {
            output.add("<AddExp>");

            if (isPLUS()) {
                addExpNode.addChildren(parsePLUS());
            }

            else {
                addExpNode.addChildren(parseMINU());
            }

            addExpNode.addChildren(parseMulExp());
        }

        output.add("<AddExp>");
        return addExpNode;
    }

    public AstNode parseRelExp() {
        // 关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        AstNode relExpNode = new AstNode("RelExp");

        relExpNode.addChildren(parseAddExp());

        while (isLSS() || isGRE() || isLEQ() || isGEQ()) {
            output.add("<RelExp>");

            if (isLSS()) {
                relExpNode.addChildren(parseLSS());
            }

            else if (isGRE()) {
                relExpNode.addChildren(parseGRE());
            }

            else if (isLEQ()) {
                relExpNode.addChildren(parseLEQ());
            }

            else {
                relExpNode.addChildren(parseGEQ());
            }

            relExpNode.addChildren(parseAddExp());
        }

        output.add("<RelExp>");
        return relExpNode;
    }

    public AstNode parseEqExp() {
        // 相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
        AstNode eqExpNode = new AstNode("EqExp");

        eqExpNode.addChildren(parseRelExp());

        while (isEQL() || isNEQ()) {
            output.add("<EqExp>");

            if (isEQL()) {
                eqExpNode.addChildren(parseEQL());
            }

            else {
                eqExpNode.addChildren(parseNEQ());
            }

            eqExpNode.addChildren(parseRelExp()); 
        }

        output.add("<EqExp>");
        return eqExpNode;
    }

    public AstNode parseLAndExp() {
        // 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp // a
        AstNode lAndExpNode = new AstNode("LAndExp");

        lAndExpNode.addChildren(parseEqExp());

        while (isAND()) {
            output.add("<LAndExp>");

            lAndExpNode.addChildren(parseAND());
            lAndExpNode.addChildren(parseEqExp());
        }

        output.add("<LAndExp>");
        return lAndExpNode;
    }

    public AstNode parseLOrExp() {
        // 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp // a
        AstNode lOrExpNode = new AstNode("LOrExp");

        lOrExpNode.addChildren(parseLAndExp());

        while (isOR()) {
            output.add("<LOrExp>");

            lOrExpNode.addChildren(parseOR());
            lOrExpNode.addChildren(parseLAndExp());
        }

        output.add("<LOrExp>");
        return lOrExpNode;
    }

    public AstNode parseConstExp() {
        AstNode constExpNode = new AstNode("ConstExp");

        constExpNode.addChildren(parseAddExp());

        output.add("<ConstExp>");
        return constExpNode;
    }

    /*
    <------------- 终结符与非终结符辨析函数分界线 ------------->
    */

    public AstNode parseIdent() {
        return new AstNode(expect(TokenType.IDENFR));
    }

    public AstNode parseINTCON() {
        return new AstNode(expect(TokenType.INTCON));
    }

    public AstNode parseCHARCON() {
        return new AstNode(expect(TokenType.CHARCON));
    }

    public AstNode parseStringConst() {
        return new AstNode(expect(TokenType.STRCON));
    }

    public AstNode parseCONSTTK() {
        return new AstNode(expect(TokenType.CONSTTK));
    }

    public AstNode parseSTATICTK() {
        return new AstNode(expect(TokenType.STATICTK));
    }

    public AstNode parseINTTK() {
        return new AstNode(expect(TokenType.INTTK));
    }

    public AstNode parseCHARTK() {
        return new AstNode(expect(TokenType.CHARTK));
    }

    public AstNode parseVOIDTK() {
        return new AstNode(expect(TokenType.VOIDTK));
    }

    public AstNode parseMAINTK() {
        return new AstNode(expect(TokenType.MAINTK));
    }

    public AstNode parseIFTK() {
        return new AstNode(expect(TokenType.IFTK));
    }

    public AstNode parseELSETK() {
        return new AstNode(expect(TokenType.ELSETK));
    }

    public AstNode parseWHILETK() {
        return new AstNode(expect(TokenType.WHILETK));
    }

    public AstNode parseSWITCHTK() {
        return new AstNode(expect(TokenType.SWITCHTK));
    }

    public AstNode parseCASETK() {
        return new AstNode(expect(TokenType.CASETK));
    }

    public AstNode parseDEFAULTTK() {
        return new AstNode(expect(TokenType.DEFAULTTK));
    }

    public AstNode parseBREAKTK() {
        return new AstNode(expect(TokenType.BREAKTK));
    }

    public AstNode parseCONTINUETK() {
        return new AstNode(expect(TokenType.CONTINUETK));
    }

    public AstNode parseRETURNTK() {
        return new AstNode(expect(TokenType.RETURNTK));
    }

    public AstNode parsePRINTFTK() {
        return new AstNode(expect(TokenType.PRINTFTK));
    }

    public AstNode parseLPARENT() {
        return new AstNode(expect(TokenType.LPARENT));
    }

    public AstNode parseRPARENT() {
        return new AstNode(expect(TokenType.RPARENT));
    }

    public AstNode parseLBRACK() {
        return new AstNode(expect(TokenType.LBRACK));
    }

    public AstNode parseRBRACK() {
        return new AstNode(expect(TokenType.RBRACK));
    }

    public AstNode parseLBRACE() {
        return new AstNode(expect(TokenType.LBRACE));
    }

    public AstNode parseRBRACE() {
        return new AstNode(expect(TokenType.RBRACE));
    }

    public AstNode parseSEMICN() {
        return new AstNode(expect(TokenType.SEMICN));
    }

    public AstNode parseCOMMA() {
        return new AstNode(expect(TokenType.COMMA));
    }

    public AstNode parseCOLON() {
        return new AstNode(expect(TokenType.COLON));
    }

    public AstNode parseASSIGN() {
        return new AstNode(expect(TokenType.ASSIGN));
    }

    public AstNode parsePLUS() {
        return new AstNode(expect(TokenType.PLUS));
    }

    public AstNode parseMINU() {
        return new AstNode(expect(TokenType.MINU));
    }

    public AstNode parseMULT() {
        return new AstNode(expect(TokenType.MULT));
    }

    public AstNode parseDIV() {
        return new AstNode(expect(TokenType.DIV));
    }

    public AstNode parseMOD() {
        return new AstNode(expect(TokenType.MOD));
    }

    public AstNode parseNOT() {
        return new AstNode(expect(TokenType.NOT));
    }

    public AstNode parseLSS() {
        return new AstNode(expect(TokenType.LSS));
    }

    public AstNode parseGRE() {
        return new AstNode(expect(TokenType.GRE));
    }

    public AstNode parseLEQ() {
        return new AstNode(expect(TokenType.LEQ));
    }

    public AstNode parseGEQ() {
        return new AstNode(expect(TokenType.GEQ));
    }

    public AstNode parseEQL() {
        return new AstNode(expect(TokenType.EQL));
    }

    public AstNode parseNEQ() {
        return new AstNode(expect(TokenType.NEQ));
    }

    public AstNode parseAND() {
        return new AstNode(expect(TokenType.AND));
    }

    public AstNode parseOR() {
        return new AstNode(expect(TokenType.OR));
    }

}
