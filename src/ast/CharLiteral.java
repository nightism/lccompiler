package ast;

import java.util.List;

public class CharLiteral extends Expr {
    public final char character;

    public CharLiteral(char c) {
        this.character = c;
    }

    public CharLiteral(String c) {
        this(c.charAt(0));
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitCharLiteral(this);
    }
}
