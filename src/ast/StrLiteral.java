package ast;

import java.util.List;

public class StrLiteral extends Expr {
    public final String str;

    public StrLiteral(String str) {
        this.str = str;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitStrLiteral(this);
    }
}
