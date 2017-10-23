package ast;

import java.util.List;

public class ExprStmt extends Stmt {

    public final Expr exp;

    public ExprStmt(Expr exp) {
        this.exp = exp;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitExprStmt(this);
    }
}
