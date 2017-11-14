package ast;

/**
 * @author myiking
 */
public class VarDecl implements ASTNode {
    public final Type type;
    public final String varName;
    public int offset;

    public VarDecl(Type type, String varName) {
        this.type = type;
        this.varName = varName;
        if (type.size() == 0) {
            this.offset = 0;
        } else {
            this.offset = -1;
        }
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitVarDecl(this);
    }
}
