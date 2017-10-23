package ast;

import java.util.List;

public class Block extends Stmt {

    public List<VarDecl> varDecls;
    public List<Stmt> stmts;

    public Block (List<VarDecl> varDecls, List<Stmt> stmts) {
        this.varDecls = varDecls;
        this.stmts = stmts;
    }

    public <T> T accept(ASTVisitor<T> v) {
       return v.visitBlock(this);
    }
}
