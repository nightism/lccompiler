package sem;

import java.util.ArrayList;

import ast.*;

public class NameAnalysisVisitor extends BaseSemanticVisitor<Void> {

    Scope scope;

    public NameAnalysisVisitor(Scope scope) {
        this.scope = scope;
        addBuildinFun();
    }

    public NameAnalysisVisitor() {
        this.scope = new Scope(null);
        addBuildinFun();
    }

    public Void addBuildinFun() {
        ArrayList<VarDecl> al1 = new ArrayList<VarDecl>();
        Block b1 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        al1.add(new VarDecl(new PointerType(BaseType.CHAR), "s"));
        this.scope.put(new FunSymbol(new FunDecl(BaseType.VOID, "print_s", al1, b1)));

        ArrayList<VarDecl> al2 = new ArrayList<VarDecl>();
        Block b2 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        al2.add(new VarDecl(BaseType.INT, "i"));
        this.scope.put(new FunSymbol(new FunDecl(BaseType.VOID, "print_i", al2, b2)));

        ArrayList<VarDecl> al3 = new ArrayList<VarDecl>();
        Block b3 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        al3.add(new VarDecl(BaseType.CHAR, "c"));
        this.scope.put(new FunSymbol(new FunDecl(BaseType.VOID, "print_c", al3, b3)));

        ArrayList<VarDecl> al4 = new ArrayList<VarDecl>();
        Block b4 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        this.scope.put(new FunSymbol(new FunDecl(BaseType.CHAR, "read_c", al4, b4)));

        ArrayList<VarDecl> al5 = new ArrayList<VarDecl>();
        Block b5 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        this.scope.put(new FunSymbol(new FunDecl(BaseType.INT, "read_i", al5, b5)));

        ArrayList<VarDecl> al6 = new ArrayList<VarDecl>();
        Block b6 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        al6.add(new VarDecl(BaseType.INT, "size"));
        this.scope.put(new FunSymbol(new FunDecl(new PointerType(BaseType.VOID), "mcmalloc", al6, b6)));

        return null;
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
        Symbol sym = this.scope.lookupCurrent(p.name);

        if (sym != null) {
            error("function " + p.name + " has already been declared.");
        } else {
            // error("test");
            this.scope.put(new FunSymbol(p));
        }

        Scope old = this.scope;
        this.scope = new Scope(old); // set outer scope

        for (VarDecl vd : p.params) {
            vd.accept(this);
        }

        for (VarDecl vd : p.block.varDecls) {
            vd.accept(this);
        }

        for (Stmt st : p.block.stmts) {
            st.accept(this);
        }

        this.scope = old;
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
            error("variable " + vd.varName + " has already been declared in current scope.");
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
        } else if (!sym.isVar()) {
            error(v.name + " is not variable name.");
        } else {
            // error("test");
            v.decl = ((VarSymbol) sym).vd;
        }

        return null;
    }

    /** more visitor methods */

    @Override
    public Void visitArrayAccessExpr(ArrayAccessExpr aae) {
        aae.base.accept(this);
        aae.index.accept(this);
        return null;
    }

    @Override
    public Void visitArrayType(ArrayType at) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitAssign(Assign a) {
        a.assignee.accept(this);
        a.assigner.accept(this);
        return null;
    }

    @Override
    public Void visitBinOp(BinOp bo) {
        bo.operandOne.accept(this);
        bo.operandTwo.accept(this);
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
        // should we check whether the field is defined
        return null;
    }

    @Override
    public Void visitFunCallExpr(FunCallExpr fce) {
        Symbol sym = this.scope.lookup(fce.name);

        if (sym == null) {
            error("function " + fce.name + " is not declared before calling.");
        } else if (!sym.isFun()) {
            error(fce.name + " is not a valid function name.");
        } else {
            // error("test");
            fce.decl = ((FunSymbol) sym).fd;
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
    public Void visitPointerType(PointerType pt) {
        // nothing to check
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
        // nothing to check
        return null;
    }

    @Override
    public Void visitStrLiteral(StrLiteral sl) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitStructType(StructType st) {
        // nothing to check
        return null;
    }

    @Override
    public Void visitTypecastExpr(TypecastExpr tce) {
        tce.exp.accept(this);
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
