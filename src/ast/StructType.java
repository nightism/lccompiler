package ast;

/**
 * @author myiking
 */
public class StructType implements Type {

    public final String name;

    public StructType (String n) {
        this.name = n;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructType(this);
    }

}
