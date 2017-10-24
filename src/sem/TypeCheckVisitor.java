package sem;

import ast.*;

public class TypeCheckVisitor extends BaseSemanticVisitor<Type> {

    Scope scope;

    public TypeCheckVisitor(Scope scope) {
        this.scope = scope;
    }

    @Override
    public Type visitBaseType(BaseType bt) {
        // To be completed...
        return null;
    }

    @Override
    public Type visitStructTypeDecl(StructTypeDecl st) {
        // To be completed...
        return null;
    }

    @Override
    public Type visitBlock(Block b) {
        // To be completed...
        return null;
    }

    @Override
    public Type visitFunDecl(FunDecl p) {
        for (VarDecl vd : p.params) {
            vd.accept(this);
        }
        p.block.accept(this);
       return null;
    }

    @Override
    public Type visitProgram(Program p) {
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
    public Type visitVarDecl(VarDecl vd) {
        if (vd.type == BaseType.VOID) {
            error("the type of variable " + vd.varName + " cannot be VOID.");
        }
       return null;
    }

    @Override
    public Type visitVarExpr(VarExpr v) {
       v.type = v.vd.type;
       return v.type;
    }

    /** 
        more visitor methods 
    */

    @Override
    public Type visitArrayAccessExpr(ArrayAccessExpr aae) {
        return null;
    }

    @Override
    public Type visitArrayType(ArrayType at) {
        return null;
    }

    @Override
    public Type visitAssign(Assign a) {
        return null;
    }

    @Override
    public Type visitBinOp(BinOp bo) {
        return null;
    }

    @Override
    public Type visitChrLiteral(ChrLiteral cl) {
        return BaseType.CHAR;
    }

    @Override
    public Type visitExprStmt(ExprStmt es) {
        return null;
    }

    @Override
    public Type visitFieldAccessExpr(FieldAccessExpr faexp) {
        return null;
    }

    @Override
    public Type visitFunCallExpr(FunCallExpr fce) {
        return null;
    }

    @Override
    public Type visitIf(If i) {
        return null;
    }

    @Override
    public Type visitIntLiteral(IntLiteral il) {
        return BaseType.INT;
    }

    @Override
    public Type visitOp(Op o) {
        return null;
    }

    @Override
    public Type visitPointerType(PointerType t) {
        return null;
    }

    @Override
    public Type visitReturn(Return r) {
        return null;
    }

    @Override
    public Type visitSizeOfExpr(SizeOfExpr soe) {
        return null;
    }

    @Override
    public Type visitStrLiteral(StrLiteral sl) {
        return new ArrayType(BaseType.CHAR, sl.str.length() + 1);
    }

    @Override
    public Type visitStructType(StructType st) {
        return null;
    }

    @Override
    public Type visitTypecastExpr(TypecastExpr tce) {
        return null;
    }

    @Override
    public Type visitValueAtExpr(ValueAtExpr vae) {
        return null;
    }

    @Override
    public Type visitWhile(While w) {
        return null;
    }


}
