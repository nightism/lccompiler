package ast;

import java.util.List;

public class Assign extends Stmt {

    public final Expr assignee;
    public final Expr assigner;

    public Assign(Expr assignee, Expr assigner) {
        this.assignee = assignee;
        this.assigner = assigner;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitAssign(this);
    }
}
