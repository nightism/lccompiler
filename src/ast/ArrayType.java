package ast;

/**
 * @author myiking
 */
public class ArrayType implements Type {

    public final Type type;
    public final IntLiteral number;

    public ArrayType (Type t, IntLiteral n) {
        this.type = t;
        this.number = n;
    }

    public ArrayType (Type t, int n) {
        this(t, new IntLiteral(n));
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitArrayType(this);
    }

    public int size() {
        return number.number * type.size();
    }

}
