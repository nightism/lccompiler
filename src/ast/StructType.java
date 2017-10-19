package ast;

/**
 * @author myiking
 */
public class StructType implements Type {

    String name;

    public StructType (String n) {
        this.name = n;
    }

    public <T> T accept(ASTVisitor<T> v) {
        //return v.visitPointerType(this);
        return null;
    }

}
