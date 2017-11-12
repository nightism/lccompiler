package ast;

/**
 * @author myiking
 */
public class StructType implements Type {

    public final String name;
    public StructTypeDecl sd;

    public StructType (String n) {
        this.name = n;
        sd = null;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructType(this);
    }

    public int size() {
        int result = 0;
        if (sd == null)
            return 0;
        for (VarDecl vd : sd.varDecls) {
            int s = vd.type.size() - 1;
            s = s / 4 + 1;
            result += s;
        }
        return result;
    }
}
