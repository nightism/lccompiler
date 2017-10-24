package ast;

/**
 * @author myiking
 */
public class ArrayType implements Type {

    public final Type type;
    public IntLiteral number;

    public ArrayType (Type t, IntLiteral n) {
        this.type = t;
        this.number = n;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitArrayType(this);
    }

}
