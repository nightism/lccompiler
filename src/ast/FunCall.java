package ast;

import java.util.List;

public class FunCall extends Expr {
    public final String name;
    public final List<Expr> params;

    public FunCall(String name, List<Expr> params) {
        this.name = name;
        this.params = params;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitFunCall(this);
    }
}
