package sem;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private Scope outer;
    private Map<String, Symbol> symbolTable;
    
    public Scope(Scope outer) { 
        this.outer = outer; 
        symbolTable = new HashMap<String, Symbol>();
    }
    
    public Scope() { this(null); }

    /**
    * @description for variable use in name analysis
    * @return return null if there is no symbol called name in the whole scope
    *         return the symbol if existing
    */
    public Symbol lookup(String name) {
        Symbol current = symbolTable.get(name);
        if (current == null && outer != null) {
            return outer.lookup(name);
        } else {
            return current;
        }
    }

    /**
    * @description for variable declaration in name analysis
    * @return return null if there is no symbol called name in the current scope
    *         return the symbol if existing
    */
    public Symbol lookupCurrent(String name) {
        return symbolTable.get(name);
    }

    public void put(Symbol sym) {
        symbolTable.put(sym.name, sym);
    }
}
