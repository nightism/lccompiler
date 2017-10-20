package ast;

public class TypecastExpr extends Expr {
    public final Type type;
    public final Expr exp;

    public TypecastExpr(Type type, Expr exp) {
        this.type = type;
        this.exp = exp;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitTypecastExpr(this);
    }
}
