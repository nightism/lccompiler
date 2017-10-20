package ast;

import java.util.List;

public class If extends Stmt {

    public final Expr cond;
    public final List<Stmt> stmts;

    public If(Expr cond, List<Stmt> stmts) {
        this.cond = cond;
        this.stmts = stmts;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // If v.visitIf(this);
    }
}
