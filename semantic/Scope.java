package semantic;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

public class Scope {
    private int id;
    private Scope parent;
    private LinkedHashMap<String, Symbol> symbols;

    public Scope(int id, Scope parent) {
        this.id = id;
        this.parent = parent;
        this.symbols = new LinkedHashMap<>();
    }

    public int getId() {
        return id;
    }

    public Scope getParent() {
        return parent;
    }

    public Symbol findLocal(String name) {
        return symbols.get(name);
    } // 只在当前作用域查找

    public boolean define(Symbol symbol) {
        String name = symbol.getName();
        if (symbols.containsKey(name)) {
            return false;
        }

        symbols.put(name, symbol);
        return true;
    }

    public Collection<Symbol> getSymbols() {
        return Collections.unmodifiableCollection(symbols.values());
    }
}
