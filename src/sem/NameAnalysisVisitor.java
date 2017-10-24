package sem;

import ast.*;

public class NameAnalysisVisitor extends BaseSemanticVisitor<Void> {

    Scope scope;

    public NameAnalysisVisitor(Scope scope) {
        this.scope = scope;
    }

    public NameAnalysisVisitor() {
        this.scope = new Scope(null);
    }

    @Override
    public Void visitBaseType(BaseType bt) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitStructTypeDecl(StructTypeDecl sts) {
        Scope old = this.scope;
        this.scope = new Scope(old); // set outer scope

        for (VarDecl vd : sts.varDecls) {
            vd.accept(this);
        }
        this.scope = old;
        return null;
    }

    @Override
    public Void visitBlock(Block b) {
        Scope old = this.scope;
        this.scope = new Scope(old); // set outer scope

        for (VarDecl vd : b.varDecls) {
            vd.accept(this);
        }

        for (Stmt st : b.stmts) {
            st.accept(this);
        }
        this.scope = old;
        return null;
    }

    @Override
    public Void visitFunDecl(FunDecl p) {
        // To be completed...
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
        Symbol sym = this.scope.lookupCurrent(vd.varName);

        if (sym != null) {
            error("variable " + vd.varName + " has already been declared.");
        } else {
            // error("test");
            this.scope.put(new VarSymbol(vd));
        }

        return null;
    }

    @Override
    public Void visitVarExpr(VarExpr v) {
        Symbol sym = this.scope.lookup(v.name);

        if (sym == null) {
            error("variable " + v.name + " is not declared before usage.");
        } else {
            // error("test");
            v.decl = ((VarSymbol) sym).vd;
        }

        return null;
    }







        // To be completed...

    @Override
    public Void visitArrayAccessExpr(ArrayAccessExpr aae) {
        return null;
    }

    @Override
    public Void visitArrayType(ArrayType at) {
        return null;
    }

    @Override
    public Void visitAssign(Assign a) {
        return null;
    }

    @Override
    public Void visitBinOp(BinOp bo) {
        return null;
    }

    @Override
    public Void visitChrLiteral(ChrLiteral cl) {
        return null;
    }

    @Override
    public Void visitExprStmt(ExprStmt es) {
        return null;
    }

    @Override
    public Void visitFieldAccessExpr(FieldAccessExpr faexp) {
        return null;
    }

    @Override
    public Void visitFunCallExpr(FunCallExpr fce) {
        return null;
    }

    @Override
    public Void visitIf(If i) {
        return null;
    }

    @Override
    public Void visitIntLiteral(IntLiteral il) {
        return null;
    }

    @Override
    public Void visitOp(Op o) {
        return null;
    }

    @Override
    public Void visitPointerType(PointerType pt) {
        // To be completed...
        return null;
    }

    @Override
    public Void visitReturn(Return r) {
        return null;
    }

    @Override
    public Void visitSizeOfExpr(SizeOfExpr soe) {
        return null;
    }

    @Override
    public Void visitStrLiteral(StrLiteral sl) {
        return null;
    }

    @Override
    public Void visitStructType(StructType st) {
        return null;
    }

    @Override
    public Void visitTypecastExpr(TypecastExpr tce) {
        return null;
    }

    @Override
    public Void visitValueAtExpr(ValueAtExpr vae) {
        return null;
    }

    @Override
    public Void visitWhile(While w) {
        return null;
    }

}
