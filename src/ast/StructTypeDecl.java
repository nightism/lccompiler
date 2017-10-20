package ast;

import java.util.List;

/**
 * @author myiking
 */
public class StructTypeDecl implements ASTNode {

    public StructType name;
    public List<VarDecl> varDecls;

    public StructTypeDecl (StructType name, List<VarDecl> varDecls) {
        this.name = name;
        this.varDecls = varDecls;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitStructTypeDecl(this);
    }

}
