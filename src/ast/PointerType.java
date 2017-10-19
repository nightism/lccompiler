package ast;

/**
 * @author myiking
 */
public class PointerType implements Type {

    Type type;

    public PointerType (Type t) {
        this.type = t;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitPointerType(this);
    }

}
