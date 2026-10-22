package semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SymbolTable {
    private Scope currentScope = null;
    private List<Scope> allScopes = new ArrayList<>();
    private int ScopeId = 0;

    public SymbolTable() {
        enterScope();
    }

    public void enterScope() {
        ScopeId++;
        Scope newScope = new Scope(ScopeId, currentScope);

        allScopes.add(newScope);
        currentScope = newScope;
    }

    public void exitScope() {
        if (currentScope.getParent() == null) {
            throw new IllegalStateException("不能退出全局作用域");
        }
        
        currentScope = currentScope.getParent();
    }

    public boolean define(Symbol symbol) {
        return currentScope.define(symbol);
    }

    public Symbol lookup(String name) {
        Scope scope = currentScope;

        while (scope != null) {
            Symbol symbol = scope.findLocal(name);

            if (symbol != null) {
                return symbol;
            }

            scope = scope.getParent();
        }

        return null;
    }

    public Scope getCurrentScope() {
        return currentScope;
    }

    public List<Scope> getAllScopes() {
        return Collections.unmodifiableList(allScopes);
    }

    public void registerRuntimeFunctions() {
        Symbol getIntSymbol = new Symbol("get_int", 0, Symbol.Kind.FUNCTION, Symbol.Type.INT);
        this.define(getIntSymbol);

        Symbol getCharSymbol = new Symbol("get_char", 0, Symbol.Kind.FUNCTION, Symbol.Type.CHAR);
        this.define(getCharSymbol);

        Symbol getStringSymbol = new Symbol("get_string", 0, Symbol.Kind.FUNCTION, Symbol.Type.VOID);
        List<Symbol.Type> list1 = new ArrayList<>(List.of(Symbol.Type.CHAR_ARRAY, Symbol.Type.INT));
        getStringSymbol.setParameterTypes(list1);
        this.define(getStringSymbol);

        Symbol putIntSymbol = new Symbol("put_int", 0, Symbol.Kind.FUNCTION, Symbol.Type.VOID);
        List<Symbol.Type> list2 = new ArrayList<>(List.of(Symbol.Type.INT));
        putIntSymbol.setParameterTypes(list2);
        this.define(putIntSymbol);

        Symbol putCharSymbol = new Symbol("put_char", 0, Symbol.Kind.FUNCTION, Symbol.Type.VOID);
        List<Symbol.Type> list3 = new ArrayList<>(List.of(Symbol.Type.CHAR));
        putCharSymbol.setParameterTypes(list3);
        this.define(putCharSymbol);

        Symbol putStringSymbol = new Symbol("put_string", 0, Symbol.Kind.FUNCTION, Symbol.Type.VOID);
        List<Symbol.Type> list4 = new ArrayList<>(List.of(Symbol.Type.CHAR_ARRAY));
        putStringSymbol.setParameterTypes(list4);
        this.define(putStringSymbol);
    }
}
