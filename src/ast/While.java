package ast;

import java.util.List;

public class While extends Stmt {

    public final Expr cond;
    public final Stmt stmt;

    public While(Expr cond, Stmt stmt) {
        this.cond = cond;
        this.stmt = stmt;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitWhile(this);
    }
}
