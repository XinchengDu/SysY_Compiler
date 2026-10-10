package parser;
import lexer.*;
import static parser.PaserContext.*;

import java.util.ArrayList;
import java.util.List;

public class AstNode {
    public String type;
    public Token token;
    public List<AstNode> children = new ArrayList<>();
    public AstNode father;
    public boolean isIError = false;
    public boolean isKError = false;
    public boolean isJError = false;

    public AstNode(String type) {
        this.type = type;
        this.token = null;
    } //语法节点，非终结符

    public AstNode(Token token) {
        this.type = token.getType().name();
        this.token = token;
    } //Token 节点，终结符

    public void setFather(AstNode father) {
        this.father = father;
    }

    public void addChildren(AstNode child) {
        child.setFather(this);
        this.children.add(child);
    }

    public void delChildren(AstNode child) {
        this.children.remove(child);
    }

    public String getType() {
        return type;
    }

    public Token getToken() {
        return token;
    }

    public List<AstNode> getChildren() {
        return children;
    }

    public AstNode getFather() {
        return father;
    }

    public void extractLValFromExp(AstNode expNode) {
        AstNode tmpNode = expNode;
        int layers = 0;

        while (tmpNode.getType() != "LVal") {
            tmpNode = tmpNode.getChildren().get(0);
            layers++;
        }

        tmpNode.father.delChildren(tmpNode);

        this.delChildren(expNode);
        expNode.father = null;
        this.addChildren(tmpNode);

        for (int i = 0; i < layers; i++) {
            PaserContext.output.remove(PaserContext.output.size() - 1);
        }
    }

    public void extract(AstNode node, int tmp, int outputSize) {
        this.delChildren(node);
        node.father = null;

        index = tmp;

        PaserContext.output.subList(
            outputSize, PaserContext.output.size()
        ).clear();
    }
}
