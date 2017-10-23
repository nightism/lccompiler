package ast;

public class ArrayAccessExpr extends Expr {
    public final Expr base;
    public final Expr index;

    public ArrayAccessExpr(Expr base, Expr index) {
        this.index = index;
        this.base = base;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitArrayAccessExpr(this);
    }
}
