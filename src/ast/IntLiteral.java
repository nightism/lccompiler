package ast;

import java.util.List;

public class IntLiteral extends Expr {
    public final int number;

    public IntLiteral(int n) {
        this.number = n;
    }

    public IntLiteral(String n) {
        this(Integer.valueOf(n));
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitIntLiteral(this);
    }
}
