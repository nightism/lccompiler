package sem;

import java.util.HashMap;
import java.util.Map;

import ast.*;

public class StructTypeCheckVisitor extends BaseSemanticVisitor<Void> {

    Map<String, StructTypeDecl> structTypeList;
    // Scope scope;

    public StructTypeCheckVisitor (Map<String, StructTypeDecl> structTypeList) {
        this.structTypeList = structTypeList;
        // this.scope = new Scope();
    }

    @Override
    public Void visitBaseType(BaseType bt) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitStructTypeDecl(StructTypeDecl st) {
        StructTypeDecl thisST = structTypeList.get(st.name.name);
        if (thisST != null) {
            error("structure type " + st.name.name + " has already been declared.");
        } else {
            structTypeList.put(st.name.name, st);
        }
        return null;
    }

    @Override
    public Void visitBlock(Block b) {

        for (VarDecl vd : b.varDecls) {
            vd.accept(this);
        }
        for (Stmt s : b.stmts) {
            s.accept(this);
        }

        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl p) {
        for (VarDecl vd : p.params) {
            vd.accept(this);
        }
        p.block.accept(this);
        return null;
    }


    @Override
    public Void visitProgram(Program p) {
        for (StructTypeDecl std : p.structTypeDecls) {
            std.accept(this);
        }
        for (VarDecl vd : p.varDecls) {
            vd.accept(this);
        }
        for (FunDecl fd : p.funDecls) {
            fd.accept(this);
        }
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl vd) {
        vd.type.accept(this);
        return null;
    }

    @Override
    public Void visitVarExpr(VarExpr v) {
        // nothing to check
        return null;
    }

    /**
        more visitor methods
    */

    @Override
    public Void visitArrayAccessExpr(ArrayAccessExpr aae) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitArrayType(ArrayType at) {
        at.type.accept(this);
        return null;
    }

    @Override
    public Void visitAssign(Assign a) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitBinOp(BinOp bo) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitChrLiteral(ChrLiteral cl) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitExprStmt(ExprStmt es) {
        es.exp.accept(this);
        return null;
    }

    @Override
    public Void visitFieldAccessExpr(FieldAccessExpr faexp) {
        faexp.base.accept(this);
        return null;
    }

    @Override
    public Void visitFunCallExpr(FunCallExpr fce) {
        for (Expr e : fce.params) {
            e.accept(this);
        }
        return null;
    }

    @Override
    public Void visitIf(If i) {
        i.cond.accept(this);
        i.ifStmt.accept(this);
        if (i.elseStmt != null) {
            i.elseStmt.accept(this);
        }
        return null;
    }

    @Override
    public Void visitIntLiteral(IntLiteral il) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitOp(Op o) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitPointerType(PointerType t) {
        t.type.accept(this);
        return null;
    }

    @Override
    public Void visitReturn(Return r) {
        if (r.exp != null) {
            r.exp.accept(this);
        }
        return null;
    }

    @Override
    public Void visitSizeOfExpr(SizeOfExpr soe) {
        soe.target.accept(this);
        return null;
    }

    @Override
    public Void visitStrLiteral(StrLiteral sl) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitStructType(StructType st) {
        if (this.structTypeList.get(st.name) == null) {
            error("structure type " + st.name + " does not exist.");
        } else if (st.sd == null) {
            st.sd = this.structTypeList.get(st.name);
        }

        return null;
    }

    @Override
    public Void visitTypecastExpr(TypecastExpr tce) {
        tce.targetType.accept(this);
        return null;
    }

    @Override
    public Void visitValueAtExpr(ValueAtExpr vae) {
        vae.exp.accept(this);
        return null;
    }

    @Override
    public Void visitWhile(While w) {
        w.cond.accept(this);
        w.stmt.accept(this);
        return null;
    }


}
