package ast;

public class FieldAccessExpr extends Expr {
    public final Expr base;
    public final String field;

    public FieldAccessExpr(Expr base, String field) {
        this.field = field;
        this.base = base;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitFieldAccessExpr(this);
    }
}
