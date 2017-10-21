package ast;

import java.util.List;

public class ChrLiteral extends Expr {
    public final char character;

    public ChrLiteral(char c) {
        this.character = c;
    }

    public ChrLiteral(String c) {
        this(c.charAt(0));
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitChrLiteral(this);
    }
}
