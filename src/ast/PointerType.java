package ast;

/**
 * @author myiking
 */
public class PointerType implements Type {

    public final Type type;

    public PointerType (Type t) {
        this.type = t;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitPointerType(this);
    }

    public int size() {
        return type.size();
    }

}
