package parser;
import static parser.PaserContext.*;
import static lexer.TokenType.*;

public class GrammarJudge {
    public static boolean isDecl() {
        return isConstDecl() || isVarDecl();
    }

    public static boolean isConstDecl() {
        return index < length && getCurToken().getType() == CONSTTK;
    }

    public static boolean isVarDecl() {
        if (index >= length) {
            return false;
        }

        if (getCurToken().getType() == STATICTK) {
            return true;
        }

        if (isBType() && getFollowToken(1).getType() == IDENFR && !(getFollowToken(2).getType() == LPARENT)) {
            return true;
        } //防止 CompUnit 中的 VarDecl 后的 FuncDef 误被识别为 VarDecl
        
        return false;
    }

    public static boolean isFuncDef() {
        return isFuncType() && getFollowToken(1).getType() == IDENFR;
    }

    public static boolean isFuncType() {
        return isVOIDTK() || isINTTK() || isCHARTK();
    }

    public static boolean isVOIDTK() {
        return getCurToken().getType() == VOIDTK;
    }

    public static boolean isBType() {
        return getCurToken().getType() == INTTK || getCurToken().getType() == CHARTK;
    }

    public static boolean isCONSTTK() {
        return getCurToken().getType() == CONSTTK;
    }

    public static boolean isConstDef() {
        return getCurToken().getType() == IDENFR;
    }

    public static boolean isCOMMA() {
        return getCurToken().getType() == COMMA;
    }

    public static boolean isSEMICN() {
        return getCurToken().getType() == SEMICN;
    }

    public static boolean isINTTK() {
        return getCurToken().getType() == INTTK;
    }
    
    public static boolean isLBRACK() {
        return getCurToken().getType() == LBRACK;
    }

    public static boolean isRBRACK() {
        return getCurToken().getType() == RBRACK;
    }

    public static boolean isLBRACE() {
        return getCurToken().getType() == LBRACE;
    }

    public static boolean isRBRACE() {
        return getCurToken().getType() == RBRACE;
    }

    public static boolean isSTATICTK() {
        return getCurToken().getType() == STATICTK;
    }

    public static boolean isASSIGN() {
        return getCurToken().getType() == ASSIGN;
    }

    public static boolean isLPARENT() {
        return getCurToken().getType() == LPARENT;
    }

    public static boolean isRPARENT() {
        return getCurToken().getType() == RPARENT;
    }

    public static boolean isCHARTK() {
        return getCurToken().getType() == CHARTK;
    }

    public static boolean isIFTK() {
        return getCurToken().getType() == IFTK;
    }

    public static boolean isELSETK() {
        return getCurToken().getType() == ELSETK;
    }

    public static boolean isWHILETK() {
        return getCurToken().getType() == WHILETK;
    }

    public static boolean isSWITCHTK() {
        return getCurToken().getType() == SWITCHTK;
    }

    public static boolean isBREAKTK() {
        return getCurToken().getType() == BREAKTK;
    }

    public static boolean isCONTINUETK() {
        return getCurToken().getType() == CONTINUETK;
    }

    public static boolean isRETURNTK() {
        return getCurToken().getType() == RETURNTK;
    }

    public static boolean isIdent() {
        return getCurToken().getType() == IDENFR;
    }

    public static boolean isNumber() {
        return getCurToken().getType() == INTCON || getCurToken().getType() == CHARCON;
    }

    public static boolean isUnaryOp() {
        return getCurToken().getType() == PLUS || getCurToken().getType() == MINU || getCurToken().getType() == NOT;
    }

    public static boolean isPRINTFTK() {
        return getCurToken().getType() == PRINTFTK;
    }

    public static boolean isExp() {
        return isUnaryExp();
    }

    public static boolean isUnaryExp() {
        return isPrimaryExp() || (isIdent() && getFollowToken(1).getType() == LPARENT) || isUnaryOp();
    }

    public static boolean isPrimaryExp() {
        return isLPARENT() || isIdent() || isNumber();
    }

    public static boolean isCASETK() {
        return getCurToken().getType() == CASETK;
    }

    public static boolean isDEFAULTTK() {
        return getCurToken().getType() == DEFAULTTK;
    }

    public static boolean isINTCON() {
        return getCurToken().getType() == INTCON;
    }

    public static boolean isPLUS() {
        return getCurToken().getType() == PLUS;
    }

    public static boolean isMINU() {
        return getCurToken().getType() == MINU;
    }

    public static boolean isMULT() {
        return getCurToken().getType() == MULT;
    }

    public static boolean isDIV() {
        return getCurToken().getType() == DIV;
    }

    public static boolean isMOD() {
        return getCurToken().getType() == MOD;
    }

    public static boolean isGRE() {
        return getCurToken().getType() == GRE;
    }

    public static boolean isLSS() {
        return getCurToken().getType() == LSS;
    }

    public static boolean isLEQ() {
        return getCurToken().getType() == LEQ;
    }

    public static boolean isGEQ() {
        return getCurToken().getType() == GEQ;
    }

    public static boolean isEQL() {
        return getCurToken().getType() == EQL;
    }

    public static boolean isNEQ() {
        return getCurToken().getType() == NEQ;
    }

    public static boolean isAND() {
        return getCurToken().getType() == AND;
    }

    public static boolean isOR() {
        return getCurToken().getType() == OR;
    }

    public static boolean isStringConst() {
        return getCurToken().getType() == STRCON;
    }
}