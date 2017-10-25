package sem;

import ast.*;

public class TypeCheckVisitor extends BaseSemanticVisitor<Type> {

    Scope scope;

    public TypeCheckVisitor(Scope scope) {
        this.scope = scope;
    }

    @Override
    public Type visitBaseType(BaseType bt) {
        // nothing to check
        return null;
    }

    @Override
    public Type visitStructTypeDecl(StructTypeDecl st) {
        // nothing to check
        return null;
    }

    @Override
    public Type visitBlock(Block b) {
        for (VarDecl vd : b.varDecls) {
            vd.accept(this);
        }
        for (Stmt s : b.stmts) {
            s.accept(this);
        }
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
        if (v.decl != null) {
            v.type = v.decl.type;
            return v.type;
        } else {
            return null;
        }
    }

    /** 
        more visitor methods 
    */

    @Override
    public Type visitArrayAccessExpr(ArrayAccessExpr aae) {
        Type t1 = aae.base.accept(this);
        Type t2 = aae.index.accept(this);
        if (t1 instanceof PointerType && t2 == BaseType.INT) {
            aae.type = ((PointerType) t1).type;
            // TODO check array length
            return aae.type;
        } else if (t1 instanceof ArrayType && t2 == BaseType.INT) {
            aae.type = ((ArrayType) t1).type;
            // TODO check array length
            return aae.type;
        } else {
            error("type check fails when accessing array");
        }
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
        Type t1 = bo.operandOne.accept(this);
        Type t2 = bo.operandTwo.accept(this);
        if (bo.operator != Op.NE && bo.operator != Op.EQ) {
            if (t1 == BaseType.INT && t2 == BaseType.INT) {
                bo.type = BaseType.INT;
                return bo.type;
            } else {
                error("Wrong type(s) of operands encountered in Binary Operation.");
            }
        } else {
            if (t1 == BaseType.VOID || t2 == BaseType.VOID
                || t1 == null || t2 == null
                || t1 instanceof StructType || t2 instanceof StructType
                || t1 instanceof ArrayType || t2 instanceof ArrayType) {
                error("Wrong type(s) of operands encountered in Binary Operation.");
            } else if (t1.getClass().equals(t2.getClass())) {
                bo.type = BaseType.INT;
                return bo.type;
            }
        }
        return null;
    }

    @Override
    public Type visitChrLiteral(ChrLiteral cl) {
        return BaseType.CHAR;
    }

    @Override
    public Type visitExprStmt(ExprStmt es) {
        return es.exp.accept(this);
    }

    @Override
    public Type visitFieldAccessExpr(FieldAccessExpr faexp) {
        return null;
    }

    @Override
    public Type visitFunCallExpr(FunCallExpr fce) {
        if (fce.decl != null) {
            FunDecl fd = fce.decl;
            if (fce.params.size() != fd.params.size()) {
                error("wrong number of parameters when calling " + fce.name);
            } else {
                int counter = 0;
                for (Expr e : fce.params) {
                    Type t = e.accept(this);
                    Type expected = fd.params.get(counter).type;
                    if (!t.getClass().equals(expected.getClass())) {
                        error("wrong type of the " + (counter + 1) + "th paramter passed when calling " + fce.name);
                    }
                    counter ++;
                }
            }
            fce.type = fd.type;
            return fce.type;
        }
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
        soe.type = BaseType.INT;
        return soe.type;
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
        Type t = tce.exp.accept(this);
        if (t == BaseType.CHAR && tce.targetType == BaseType.INT) {
            tce.type = BaseType.INT;
            return tce.type;
        } else if (t instanceof PointerType && tce.targetType instanceof PointerType) {
            tce.type = tce.targetType;
            return tce.type;
        } else if (t instanceof ArrayType && tce.targetType instanceof PointerType) {
            tce.type = tce.targetType;
            return tce.type;
        }
        return null;
    }

    @Override
    public Type visitValueAtExpr(ValueAtExpr vae) {
        Type t = vae.exp.accept(this);
        if (t instanceof PointerType) {
            vae.type = ((PointerType) t).type;
            return vae.type;
        } else {
            error("Type check fails when accessing pointer value");
        }
        return null;
    }

    @Override
    public Type visitWhile(While w) {
        return null;
    }


}
