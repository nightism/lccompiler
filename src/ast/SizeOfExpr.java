package ast;

import java.util.List;

public class SizeOfExpr extends Expr {
    public final Type target;

    public SizeOfExpr(Type t) {
        this.target = t;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitSizeOfExpr(this);
    }
}
