package ast;

import java.util.List;

public class If extends Stmt {

    public final Expr cond;
    public final Stmt ifStmt;
    public final Stmt elseStmt; // could be null

    public If(Expr cond, Stmt ifStmt, Stmt elseStmt) {
        this.cond = cond;
        this.ifStmt = ifStmt;
        this.elseStmt = elseStmt;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitIf(this);
    }
}
