package sem;

import ast.VarDecl;

public class VarSymbol extends Symbol {
    public final VarDecl vd;

    public VarSymbol(VarDecl vd) {
        this.vd = vd;
        this.name = vd.varName;
    }

    public boolean isVar() {
        return true;
    }

    public boolean isFun() {
        return false;
    }
}