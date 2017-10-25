package ast;

public class TypecastExpr extends Expr {
    public final Type targetType;
    public final Expr exp;

    public TypecastExpr(Type targetType, Expr exp) {
        this.targetType = targetType;
        this.exp = exp;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitTypecastExpr(this);
    }
}
