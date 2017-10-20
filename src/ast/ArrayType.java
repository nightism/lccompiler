package ast;

/**
 * @author myiking
 */
public class ArrayType implements Type {

    public final Type type;
    public int number;

    public ArrayType (Type t, int n) {
        this.type = t;
        this.number = n;
    }

    public ArrayType (Type t, String n) {
        this(t, Integer.valueOf(n));
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitArrayType(this);
    }

}
