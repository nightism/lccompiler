package sem;

import ast.FunDecl;

public class FunSymbol extends Symbol {
    public final FunDecl fd;

    public FunSymbol(FunDecl fd) {
        this.fd = fd;
        this.name = fd.name;
    }

    public boolean isFun() {
        return true;
    }

    public boolean isVar() {
        return false;
    }
}