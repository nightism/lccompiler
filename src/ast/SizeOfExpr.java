package ast;

import java.util.List;

public class SizeOfExpr extends Expr {
    public final Type type;

    public SizeOfExpr(Type t) {
        this.type = t;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitSizeOfExpr(this);
    }
}
