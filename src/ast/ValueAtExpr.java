package ast;

public class ValueAtExpr extends Expr {
    public final Expr exp;

    public ValueAtExpr(Expr exp) {
        this.exp = exp;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitValueAtExpr(this);
    }
}
