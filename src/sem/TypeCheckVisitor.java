package sem;

import ast.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class TypeCheckVisitor extends BaseSemanticVisitor<Type> {

    Map<String, StructTypeDecl> structTypeList;
    Type returnType;

    public TypeCheckVisitor(Map<String, StructTypeDecl> structTypeList) {
        this.structTypeList = structTypeList;
        returnType = null;
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
        Type oldReturnType = this.returnType;
        this.returnType = p.type;
        for (VarDecl vd : p.params) {
            vd.accept(this);
        }
        p.block.accept(this);
        this.returnType = oldReturnType;
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
        // nothing to check
        return null;
    }

    @Override
    public Type visitAssign(Assign a) {
        if (!(a.assignee instanceof VarExpr
              || a.assignee instanceof FieldAccessExpr
              || a.assignee instanceof ArrayAccessExpr
              || a.assignee instanceof ValueAtExpr)) {
            error("the type of the left-hand side of the assignment statement is invalid.");
            return null;
        }

        Type t1 = a.assignee.accept(this);
        Type t2 = a.assigner.accept(this);

        if (t1 == null || t2 == null) {
            error("assignment type cannot be null.");
        } else if (!t1.getClass().equals(t2.getClass())) {
            error("expressions must be of the same type on the both sides of the assignment.");
        } else if (t1 == BaseType.VOID || t1 instanceof ArrayType) {
            error("invalid expression type for assignment.");
        }
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
        Type baseType = faexp.base.accept(this);
        if (!(baseType instanceof StructType)) {
            error("field access must be operated on a struct variable");
            return null;
        }
        String structName = ((StructType)baseType).name;
        StructTypeDecl sd = structTypeList.get(structName);
        if (sd != null) {
            List<VarDecl> vds = sd.varDecls;
            boolean fieldDefined = false;
            Type fieldType = null;
            for (VarDecl vd : vds) {
                if (vd.varName.equals(faexp.field)) {
                    fieldDefined = true;
                    faexp.type = vd.type;
                    return faexp.type;
                }
            }
            if (!fieldDefined) {
                error("field " + faexp.field + " is not defined in structure " + structName + ".");
            }
        }
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
        Type condType = i.cond.accept(this);
        if (condType != BaseType.INT) {
            error("type check fails on if condition.");
        }
        i.ifStmt.accept(this);
        if (i.elseStmt != null) {
            i.elseStmt.accept(this);
        }
        return null;
    }

    @Override
    public Type visitIntLiteral(IntLiteral il) {
        return BaseType.INT;
    }

    @Override
    public Type visitOp(Op o) {
        // nothing to check
        return null;
    }

    @Override
    public Type visitPointerType(PointerType t) {
        // nothing to check
        return null;
    }

    @Override
    public Type visitReturn(Return r) {
        if (r.exp != null) {
            Type t = r.exp.accept(this);
            if (t == null) {
                error("error occurs in the return statement.");
            } else if (!t.getClass().equals(this.returnType.getClass())) {
                error("the function return type is wrong.");
            } else if (t instanceof ArrayType) {
                if (((ArrayType)t).number.number != ((ArrayType)returnType).number.number) {
                    error("the legnth of function return type (ArrayType) is wrong.");
                } else {
                    // nothing to do, everything is fine
                }
            }
        } else if (this.returnType != BaseType.VOID) {
            error("the function return type is not void.");
        } else {
            // return type is null do nothing
        }
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
        // nothing to check
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
        } else {
            error("undefined typecast expression encounterd.");
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
            error("type check fails when accessing pointer value");
        }
        return null;
    }

    @Override
    public Type visitWhile(While w) {
        Type condType = w.cond.accept(this);
        if (condType != BaseType.INT) {
            error("type check fails on while condition.");
        }

        w.stmt.accept(this);
        return null;
    }


}
