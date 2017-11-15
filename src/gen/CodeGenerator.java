package gen;

import ast.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

public class CodeGenerator implements ASTVisitor<Register> {

    /*
     * Simple register allocator.
     */

    private int strNum;    // string literal index number
    private int boNum;     // binary operatoin index number
    private int stmtNum;   // if-else and while statement index number
    private int offset;    // variable stack offset

    // contains all the free temporary registers
    private Stack<Register> freeRegs = new Stack<Register>();

    public CodeGenerator() {
        freeRegs.addAll(Register.tmpRegs);
        strNum = 0;
        boNum = 0;
        stmtNum = 0;
        offset = 0;
    }

    private class RegisterAllocationError extends Error {}

    private Register getRegister() {
        try {
            return freeRegs.pop();
        } catch (EmptyStackException ese) {
            throw new RegisterAllocationError(); // no more free registers, bad luck!
        }
    }

    private void freeRegister(Register reg) {
        freeRegs.push(reg);
    }





    private PrintWriter writer; // use this writer to output the assembly instructions


    public void emitProgram(Program program, File outputFile) throws FileNotFoundException {
        writer = new PrintWriter(outputFile);

        visitProgram(program);
        writer.close();
    }

    @Override
    public Register visitBaseType(BaseType bt) {
        return null;
    }

    @Override
    public Register visitStructTypeDecl(StructTypeDecl st) {
        return null;
    }

    @Override
    public Register visitBlock(Block b) {
        int oldOffset = offset;

        int totalParamSize = 0;
        for (VarDecl vd : b.varDecls) {
            int size = vd.type.size();
            totalParamSize += size;
            if (size == 1) {
                // char or char* or void*
                writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -1");
                offset = offset + 1;
                vd.offset = offset;
            } else if (size != 0) { // size >= 4 and size % 4 == 0
                // int or int* or struct related
                rectifyStackPointer();
                while (totalParamSize % 4 != 0) {
                    totalParamSize ++;
                }
                if (size == 4) {
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -" + size);
                    offset = offset + size;
                    vd.offset = offset;
                } else if (vd.type instanceof StructType) {
                    vd.offset = offset + Math.min(4, size);
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -" + size);
                    offset = offset + size;
                } else if (vd.type instanceof ArrayType) {
                    if (((ArrayType) vd.type).type == BaseType.CHAR) {
                        vd.offset = offset + 1;
                    } else {
                        vd.offset = offset + 4;
                    }
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -" + size);
                    offset = offset + size;
                }
            }
        }

        rectifyStackPointer();

        for (Stmt st : b.stmts) {
            st.accept(this);
        }

        offset = oldOffset;

        writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", " + totalParamSize);

        return null;
    }

    @Override
    public Register visitFunDecl(FunDecl p) {
        int oldOffset = offset;
        offset = 0;

        // label the function with its name
        writer.println("    .text");
        writer.println(p.name + ":");

        // change $fp to $sp
        writer.println("    add  " + Register.fp.toString()+ ", " + Register.sp.toString() +", $zero");

        // save all parameters on stack
        int stackedParamSize = totalStackedParamSize(p.params);
        int paramIndex = 0;
        for (VarDecl vd : p.params) {
            if (vd.type instanceof StructType) {
                // retreive struct var from the stack
                rectifyStackPointer();
                int structSize = vd.type.size();
                vd.offset = offset + 4;
                while (structSize > 0) {
                    stackedParamSize = stackedParamSize - 4;
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -4");
                    writer.println("    lw   $a0, " + stackedParamSize + "(" + Register.fp.toString() + ")");
                    writer.println("    sw   $a0, 0(" + Register.sp.toString() + ")    #" + vd.varName);
                    offset = offset + 4;
                    structSize = structSize - 4;
                }
            } else if (paramIndex < 4) {
                // less than 4 variables
                // normal variable type, not struct type
                int size = vd.type.size();
                if (size == 1) {
                    // char or char* or void*
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -1");
                    writer.println("    sb   $a" + paramIndex + ", 0(" + Register.sp.toString() + ")    #" + vd.varName);
                    offset = offset + 1;
                    vd.offset = offset;
                } else { // size == 4
                    // int or int*
                    rectifyStackPointer();
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -4");
                    writer.println("    sw   $a" + paramIndex + ", 0(" + Register.sp.toString() + ")    #" + vd.varName);
                    offset = offset + 4;
                    vd.offset = offset;
                }
            } else {
                // retreive more variables from the stack
                int size = vd.type.size();
                if (size >= 4) {
                    rectifyStackPointer();
                    while (stackedParamSize % 4 != 0) {
                        stackedParamSize --;
                    }
                    // int locator = (stackedParamSize + offset); -> related to $sp
                    stackedParamSize -= 4;
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -4");
                    writer.println("    lw   $a0, " + stackedParamSize + "(" + Register.fp.toString() + ")");
                    writer.println("    sw   $a0, 0(" + Register.sp.toString() + ")    #" + vd.varName);
                    offset = offset + 4;
                    vd.offset = offset;
                } else {
                    // int locator = (stackedParamSize + offset);
                    stackedParamSize -= 1;
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -1");
                    writer.println("    lb   $a0, " + stackedParamSize + "(" + Register.fp.toString() + ")");
                    writer.println("    sb   $a0, 0(" + Register.sp.toString() + ")    #" + vd.varName);
                    offset = offset + 1;
                    vd.offset = offset;
                }
            }
            paramIndex ++;
        }

        // recitfy stack pointer after passing the parameters
        rectifyStackPointer();

        p.block.accept(this);

        // restore stack pointer
        writer.println("    add  " + Register.sp.toString() + ", " + Register.fp.toString() + ", $zero");

        // return to the caller function
        writer.println("    jr   " + Register.ra.toString() + "");
        writer.println();

        return null;
    }

    @Override
    public Register visitProgram(Program p) {
        writer.println(".data");
        for (VarDecl vd : p.varDecls) {
            // save variable name
            String varName = vd.varName;

            if (vd.type instanceof StructType) {
                // declare global struct variables
                StructType t = (StructType) vd.type;
                String structName = t.name;
                List<VarDecl> vdList = new ArrayList<VarDecl>(t.sd.varDecls);
                Collections.reverse(vdList); // store the gobal struct downward
                for (VarDecl v : vdList) {
                    int size = ((v.type.size() - 1) / 4 + 1) * 4;
                    // name : varName_structName_fieldName
                    writer.print(varName + "_" + structName + "_" + v.varName);
                    writer.println(":  .space  " + size);
                }
            } else if (vd.type.size() <= 4) {
                // declare normal variables
                writer.println(varName + ":  .word  0");
            } else { // arrays
                writer.println(varName + ":  .space  " + vd.type.size());
            }
        }
        writer.println();

        writer.println(".text");
        writer.println("    jal  main");
        writer.println("    li   $v0, 10");
        writer.println("    syscall");
        writer.println();

        for (FunDecl fd : p.funDecls) {
            fd.accept(this);
        }

        generatePrintI();
        generatePrintC();
        generatePrintS();
        generateReadI();
        generateReadC();
        generateMcmalloc();


        writer.flush();
        return null;
    }

    @Override
    public Register visitVarDecl(VarDecl vd) {
        return null;
    }

    @Override
    public Register visitVarExpr(VarExpr v) {
        int size = v.decl.type.size();
        Register result = getRegister();

        if (v.decl.offset == -1 && v.decl.type.size() != 0) {
            if (v.decl.type instanceof StructType) {
                StructType st  = ((StructType) v.decl.type);
                String firstVarName = st.sd.varDecls.get(0).varName;
                // varName_structName_fieldName
                writer.println("    la   " + result.toString() + ", " + v.name + "_" + st.name + "_" + firstVarName);
            } else {
                writer.println("    la   " + result.toString() + ", " + v.name);
            }
        } else {
            int thisOffset = offset - v.decl.offset;
            writer.println("    addi " + result.toString() + ", " + Register.sp.toString() + ", " + thisOffset);
        }

        if (size == 1) {
            writer.println("    lb   " + result.toString() + ", (" + result.toString() + ")");
        } else if (v.decl.type instanceof ArrayType || v.decl.type instanceof StructType) {
            return result;
        } else {
            writer.println("    lw   " + result.toString() + ", (" + result.toString() + ")");
        }
        return result;
    }


    /**
        more visitor methods
    */


    @Override
    public Register visitArrayAccessExpr(ArrayAccessExpr aae) {
        Expr vd = aae.base;
        ArrayType at = (ArrayType) vd.type;
        int elemSize = at.type.size();

        Register base = vd.accept(this);
        if (base == null) {
            return null;
        }
        Register index = aae.index.accept(this);
        if (index == null) {
            freeRegister(base);
            return null;
        }

        Register result = getRegister();

        int storageDirection = -1 * elemSize;
        if (vd instanceof VarExpr) {
             storageDirection = (((VarExpr) vd).decl.offset == -1) ? (elemSize) : (-1 * elemSize);
        }
        writer.println("    li   " + result.toString() + ", " + storageDirection);
        writer.println("    mult " + result.toString() + ", " + index.toString());
        writer.println("    mflo " + result.toString());
        writer.println("    add  " + result.toString() + ", " + base.toString() + ", " + result.toString());
        // now result stores the address of the target element

        if (at.type instanceof StructType) {
            // return the starting address of struct
            if (storageDirection < 0) {
                storageDirection = -storageDirection - 4;
                writer.println("    addi " + result.toString() + ", " + result.toString() + ", " + storageDirection);
            }
        } else if (elemSize == 1) {
            writer.println("    lb   " + result.toString() + ", (" + result.toString() + ")");
        } else if (elemSize == 4) {
            writer.println("    lw   " + result.toString() + ", (" + result.toString() + ")");
        }

        freeRegister(base);
        freeRegister(index);
        return result;
    }

    @Override
    public Register visitArrayType(ArrayType at) {
        return null;
    }

    @Override
    public Register visitAssign(Assign a) {
        Expr assignee = a.assignee;
        Expr assigner = a.assigner;

        Register result = assigner.accept(this);
        if (result == null) {
            return null;
        }

        if (assignee.type.size() == 0) {
            freeRegister(result);
            return null;
        } else if (assignee.type instanceof StructType) {
            Register target = assignee.accept(this);
            if (target == null) {
                freeRegister(result);
                return null;
            }
            // copy struct content
            Register helper = getRegister();
            int size = assignee.type.size();
            for (int i = 0; i < size; i = i + 4) {
                int add = -i;
                writer.println("    lw   " + helper.toString() + ", " + add + "(" + result.toString() + ")");
                writer.println("    sw   " + helper.toString() + ", " + add + "(" + target.toString() + ")");
            }
            freeRegister(helper);
            freeRegister(target);
            return null;
        } else if (assignee instanceof VarExpr) {
            VarExpr v = (VarExpr) assignee;
            if (v.decl.type.size() == 0) {
                freeRegister(result);
                return null;
            }

            Register address;
            if (v.decl.offset == -1) {
                address = getRegister();
                writer.println("    la   " + address.toString() + ", " + v.name);
            } else {
                address = getVarAddress(v);
                if (address == null) {
                    freeRegister(result);
                    return null;
                }
            }

            if (v.decl.type.size() == 1) {
                writer.println("    sb   " + result.toString() + ", (" + address.toString() + ")");
            } else {
                writer.println("    sw   " + result.toString() + ", (" + address.toString() + ")");
            }

            freeRegister(address);
        } else if (assignee instanceof FieldAccessExpr) {
            FieldAccessExpr faexp = (FieldAccessExpr) assignee;
            Register address = getFieldAddress(faexp);
            if (address == null) {
                freeRegister(result);
                return null;
            }

            writer.println("    sw   " + result.toString() + ", (" + address.toString() + ")");
            freeRegister(address);
        } else if (assignee instanceof ArrayAccessExpr) {
            ArrayAccessExpr aae = (ArrayAccessExpr) assignee;
            Register address = getArrayAccessAddress(aae);
            if (address == null) {
                freeRegister(result);
                return null;
            }

            int elemSize = assignee.type.size();
            if (elemSize == 1) {
                writer.println("    sb   " + result.toString() + ", (" + address.toString() + ")");
            } else {
                writer.println("    sw   " + result.toString() + ", (" + address.toString() + ")");
            }
        } else if (assignee instanceof ValueAtExpr) {
            ValueAtExpr vae = (ValueAtExpr) assignee;
            Register address = vae.exp.accept(this);
            if (address == null) {
                return null;
            } else {
                PointerType t = (PointerType) vae.exp.type;
                int size = t.type.size();
                if (size == 1) {
                    writer.println("    sb   " + result.toString() + ", (" + address.toString() + ")");
                } else {
                    writer.println("    sw   " + result.toString() + ", (" + address.toString() + ")");
                }
                freeRegister(address);
            }
        }

        freeRegister(result);
        return null;
    }

    @Override
    public Register visitBinOp(BinOp bo) {
        Register operandOne = bo.operandOne.accept(this);
        Register operandTwo = bo.operandTwo.accept(this);
        Register result = getRegister();

        if (operandOne != null && operandTwo != null) {
            switch(bo.operator) {
                case ADD:
                    writer.println("    add  " + result.toString() + ", " + operandOne.toString() + ", " + operandTwo.toString());
                    break;
                case SUB:
                    writer.println("    sub  " + result.toString() + ", " + operandOne.toString() + ", " + operandTwo.toString());
                    break;
                case MUL:
                    writer.println("    mult " + operandOne.toString() + ", " + operandTwo.toString());
                    writer.println("    mflo " + result.toString());
                    break;
                case DIV:
                    writer.println("    div  " + operandOne.toString() + ", " + operandTwo.toString());
                    writer.println("    mflo " + result.toString());
                    break;
                case MOD:
                    writer.println("    div  " + operandOne.toString() + ", " + operandTwo.toString());
                    writer.println("    mfhi " + result.toString());
                    break;
                case GT:
                    writer.println("    bgt  " + operandOne.toString() + ", " + operandTwo.toString() + ", BINOP" + boNum);
                    writer.println("    li   " + result.toString() + ", 0");
                    writer.println("    j    " + "BINOPJUMP" + boNum);
                    writer.println("BINOP" + boNum + ":  li   " + result.toString() + ", 1");
                    writer.println("BINOPJUMP" + boNum + ":");
                    boNum ++;
                    break;
                case LT:
                    writer.println("    blt  " + operandOne.toString() + ", " + operandTwo.toString() + ", BINOP" + boNum);
                    writer.println("    li   " + result.toString() + ", 0");
                    writer.println("    j    " + "BINOPJUMP" + boNum);
                    writer.println("BINOP" + boNum + ":  li   " + result.toString() + ", 1");
                    writer.println("BINOPJUMP" + boNum + ":");
                    boNum ++;
                    break;
                case GE:
                    writer.println("    bge  " + operandOne.toString() + ", " + operandTwo.toString() + ", BINOP" + boNum);
                    writer.println("    li   " + result.toString() + ", 0");
                    writer.println("    j    " + "BINOPJUMP" + boNum);
                    writer.println("BINOP" + boNum + ":  li   " + result.toString() + ", 1");
                    writer.println("BINOPJUMP" + boNum + ":");
                    boNum ++;
                    break;
                case LE:
                    writer.println("    ble  " + operandOne.toString() + ", " + operandTwo.toString() + ", BINOP" + boNum);
                    writer.println("    li   " + result.toString() + ", 0");
                    writer.println("    j    " + "BINOPJUMP" + boNum);
                    writer.println("BINOP" + boNum + ":  li   " + result.toString() + ", 1");
                    writer.println("BINOPJUMP" + boNum + ":");
                    boNum ++;
                    break;
                case NE:
                    writer.println("    bne  " + operandOne.toString() + ", " + operandTwo.toString() + ", BINOP" + boNum);
                    writer.println("    li   " + result.toString() + ", 0");
                    writer.println("    j    " + "BINOPJUMP" + boNum);
                    writer.println("BINOP" + boNum + ":  li   " + result.toString() + ", 1");
                    writer.println("BINOPJUMP" + boNum + ":");
                    boNum ++;
                    break;
                case EQ:
                    writer.println("    beq  " + operandOne.toString() + ", " + operandTwo.toString() + ", BINOP" + boNum);
                    writer.println("    li   " + result.toString() + ", 0");
                    writer.println("    j    " + "BINOPJUMP" + boNum);
                    writer.println("BINOP" + boNum + ":  li   " + result.toString() + ", 1");
                    writer.println("BINOPJUMP" + boNum + ":");
                    boNum ++;
                    break;
                case OR:
                    writer.println("    bne  " + operandOne.toString() + ", $zero, BINOP" + boNum);
                    writer.println("    bne  " + operandTwo.toString() + ", $zero, BINOP" + boNum);
                    writer.println("    li   " + result.toString() + ", 0");
                    writer.println("    j    " + "BINOPJUMP" + boNum);
                    writer.println("BINOP" + boNum + ":  li   " + result.toString() + ", 1");
                    writer.println("BINOPJUMP" + boNum + ":");
                    boNum ++;
                    break;
                case AND:
                    writer.println("    beq  " + operandOne.toString() + ", $zero, BINOP" + boNum);
                    writer.println("    beq  " + operandTwo.toString() + ", $zero, BINOP" + boNum);
                    writer.println("    li   " + result.toString() + ", 1");
                    writer.println("    j    " + "BINOPJUMP" + boNum);
                    writer.println("BINOP" + boNum + ":  li   " + result.toString() + ", 0");
                    writer.println("BINOPJUMP" + boNum + ":");
                    boNum ++;
                    break;
            }
        }

        if (operandOne != null) {
            freeRegister(operandOne);
        }
        if (operandTwo != null) {
            freeRegister(operandTwo);
        }
        return result;
    }

    @Override
    public Register visitChrLiteral(ChrLiteral cl) {
        Register result = getRegister();
        writer.println("    addi " + result.toString() + ", $zero, " + ((int) cl.character));
        return result;
    }

    @Override
    public Register visitExprStmt(ExprStmt es) {
        Register result = es.exp.accept(this);
        if (result != null) {
            freeRegister(result);
        }
        return null;
    }

    @Override
    public Register visitFieldAccessExpr(FieldAccessExpr faexp) {
        Expr baseExp = faexp.base;
        StructType st = (StructType) baseExp.type;
        String field = faexp.field;
        Register result = null;

        Type elemType = BaseType.VOID;
        for (int i = 0; i < st.sd.varDecls.size(); i ++) {
            VarDecl vd = st.sd.varDecls.get(i);
            if (vd.varName.equals(field)) {
                elemType = vd.type;
                break;
            }
        }

        int thisOffset = 0;
        if (baseExp instanceof VarExpr) {
            thisOffset = ((VarExpr) baseExp).decl.offset;
        }
        if (thisOffset == -1) {
            result = getRegister();
            writer.println("    la   " + result.toString() + ", " + ((VarExpr) baseExp).name + "_" + st.name + "_" + field);

            if (elemType instanceof ArrayType) {
                int totalSize = elemType.size();
                int targetAddress = totalSize - ((ArrayType) elemType).type.size();
                writer.println("    addi  " + result.toString() + ", " + result.toString() + ", -" + targetAddress);
            } else if (elemType instanceof StructType) {
                int totalSize = elemType.size();
                // Type firstType = ((StructType) elemType).sd.varDecls.get(0).type;
                int targetAddress = totalSize - 4; //firstType.size();
                writer.println("    addi  " + result.toString() + ", " + result.toString() + ", -" + targetAddress);
            }
        } else {
            result = baseExp.accept(this);
            if (result == null) {
                return null;
            }

            int targetAddress = 0;
            for (int i = 0; i < st.sd.varDecls.size(); i ++) {
                VarDecl vd = st.sd.varDecls.get(i);
                if (vd.type.size() > 1) {
                    while(targetAddress % 4 != 0) {
                        targetAddress ++;
                    }
                }
                if (vd.varName.equals(field)) {
                    break;
                }
                targetAddress += vd.type.size();
            }

            writer.println("    add  " + result.toString() + ", " + result.toString() + ", -" + targetAddress);
        }
        if (elemType instanceof ArrayType || elemType instanceof StructType) {
            return result;
        }
        writer.println("    lw   " + result.toString() + ", (" + result.toString() + ")");
        return result;
    }

    @Override
    public Register visitFunCallExpr(FunCallExpr fce) {
        // save $fp and $ra on the stack
        writer.println("    addi " + Register.sp.toString() +", " + Register.sp.toString() +", -8");
        writer.println("    sw   " + Register.fp.toString()+ ", 4(" + Register.sp.toString() +")");
        writer.println("    sw   " + Register.ra.toString() + ", 0(" + Register.sp.toString() +")");
        offset += 8;

        int stackedSize = 0;
        int numOfParam = 0;
        // passing parameters
        for (int i = 0; i < fce.params.size(); i ++) {
        // for (Expr p : fce.params) {
            Expr p = fce.params.get(i);
            VarDecl v = fce.decl.params.get(i);
            Type t = v.type;

            Register r = p.accept(this);

            if (r == null) {
                break;
            }

            if (t instanceof StructType) {
                // stacked struct parameters
                rectifyStackPointer();
                while (stackedSize %  4 != 0) {
                    stackedSize ++;
                }

                int structSize = t.size();
                int size = 0; // size has been stacked
                while (structSize > 0) {
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -4");
                    writer.println("    lw   $a0, " + size + "(" + r.toString() + ")");
                    writer.println("    sw   $a0, 0(" + Register.sp.toString() + ")    #" + v.varName);
                    offset = offset + 4;
                    structSize = structSize - 4;
                    size = size - 4;
                    stackedSize = stackedSize + 4;
                }
            } else if (numOfParam < 4) {
                // store the first 4 in a0-3
                writer.println("    add  $a" + numOfParam + ", $zero, " + r.toString() + "    #" + v.varName);
            } else {
                // stacked more parameters (more than 4)
                int size = t.size();
                if (size >= 4) {
                    rectifyStackPointer();
                    while (stackedSize %  4 != 0) {
                        stackedSize ++;
                    }
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -4");
                    writer.println("    sw   " + r.toString() + ", 0(" + Register.sp.toString() + ")    #" + v.varName);
                    offset += 4;
                    stackedSize += 4;
                } else {
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -1");
                    writer.println("    sb   " + r.toString() + ", 0(" + Register.sp.toString() + ")    #" + v.varName);
                    offset += 1;
                    stackedSize += 1;
                }
            }
            freeRegister(r);
            numOfParam ++;
        }

        // recitfy stack pointer after passing the parameters
        rectifyStackPointer();
        while (stackedSize %  4 != 0) {
            stackedSize ++;
        }

        // jump back to the caller function
        writer.println("    jal  " + fce.name);

        // clear all stacked parameters
        writer.println("    addi " + Register.sp.toString() +", " + Register.sp.toString() +", " + stackedSize);
        offset -= stackedSize;

        // restore $fp and $ra
        writer.println("    addi " + Register.sp.toString() +", " + Register.sp.toString() +", 8");
        writer.println("    lw   " + Register.fp.toString()+ ", -4(" + Register.sp.toString() +")");
        writer.println("    lw   " + Register.ra.toString() + ", -8(" + Register.sp.toString() +")");
        offset -= 8;

        Register result = getRegister();
        writer.println("    add  " + result.toString() + ", $zero, $v0");

        return result;
    }

    @Override
    public Register visitIf(If i) {
        int num = stmtNum;
        stmtNum ++;
        Expr cond =  i.cond;
        Stmt ifStmt = i.ifStmt;
        Stmt elseStmt = i.elseStmt;

        Register condResult = cond.accept(this);
        if (condResult == null) {
            return null;
        }

        writer.println("    bne  " + condResult.toString() + ", $zero, IFSTATEMENT" + num);
        freeRegister(condResult);
        writer.println("    j    ELSESTATEMENT" + num);
        writer.println("IFSTATEMENT" + num + ": ");
        ifStmt.accept(this);
        writer.println("    j    ENDIFELSE" + num);
        writer.println("ELSESTATEMENT" + num + ": ");
        if (elseStmt != null) {
            elseStmt.accept(this);
        }
        writer.println("ENDIFELSE" + num + ": ");
        return null;
    }

    @Override
    public Register visitIntLiteral(IntLiteral il) {
        Register result = getRegister();
        writer.println("    addi " + result.toString() + ", $zero, " + il.number);
        return result;
    }

    @Override
    public Register visitOp(Op o) {
        return null;
    }

    @Override
    public Register visitPointerType(PointerType pt) {
        return null;
    }

    @Override
    public Register visitReturn(Return r) {
        Expr e = r.exp;
        if (e != null) {

            // save struct as return value
            if (e.type instanceof StructType) {
                if (e.type.size() != 0) {
                    StructType st = (StructType) e.type;

                    Register address = e.accept(this);
                    if (address == null) {
                        return null;
                    }
                    Register result = getRegister();
                    int size = e.type.size();
                    for (int stacked = size; stacked > 0; stacked = stacked - 4) {
                        int stackIndex = size - stacked + 4;
                        writer.println("    lw   " + result.toString() + ", (" + address.toString() + ")");
                        writer.println("    sw   " + result.toString() + ", -" + stackIndex + "(" + Register.sp.toString() + ")");
                        writer.println("    addi " + address.toString() + ", " + address.toString() + ", -4");
                    }
                    writer.println("    addi $v0, " + Register.sp.toString() + ", 4");
                    freeRegister(result);
                    freeRegister(address);

                }
            } else {
                Register reg = e.accept(this);
                if (reg != null) {
                    writer.println("    add  $v0, $zero, " + reg.toString());
                    freeRegister(reg);
                }
            }

            writer.println("    add  $sp, $fp, $zero");
            writer.println("    jr   $ra");

        }
        return null;
    }

    @Override
    public Register visitSizeOfExpr(SizeOfExpr soe) {
        Type t = soe.type;
        int size = 0;
        while (t instanceof PointerType) {
            t = ((PointerType) t).type;
        }
        size = t.size();
        Register result = getRegister();
        writer.println("    li   " + result.toString() + ", " + size);
        return result;
    }

    @Override
    public Register visitStrLiteral(StrLiteral sl) {
        strNum ++;
        Register result = getRegister();
        // define String literal in data section
        writer.println("    .data");
        String str = sl.str;
        writer.println("STRING" + strNum + ":  .asciiz  \"" + str + "\"");
        // back to text section and store the string in register
        writer.println("    .text");
        writer.println("    la " + result.toString() + ", STRING" + strNum);
        return result;
    }

    @Override
    public Register visitStructType(StructType st) {
        return null;
    }

    @Override
    public Register visitTypecastExpr(TypecastExpr tce) {
        Register r = tce.exp.accept(this);
        return r;
    }

    @Override
    public Register visitValueAtExpr(ValueAtExpr vae) {
        Register r = vae.exp.accept(this);
        if (r == null) {
            return null;
        } else {
            Register result = getRegister();
            PointerType t = (PointerType) vae.exp.type;
            int size = t.type.size();
            if (vae.type instanceof StructType) {
                size = size - 4;
                writer.println("    addi " + result.toString() + ", " + r.toString() + ", " + size);
            } else if (size == 1) {
                writer.println("    lb   " + result.toString() + ", (" + r.toString() + ")");
            } else if (size == 4) {
                writer.println("    lw   " + result.toString() + ", (" + r.toString() + ")");
            } else {
                // save the address
                writer.println("    add  " + result.toString() + ", $zero, " + r.toString());
            }
            freeRegister(r);
            return result;
        }
    }

    @Override
    public Register visitWhile(While w) {
        int num = stmtNum;
        stmtNum ++;
        Expr cond = w.cond;
        Stmt s = w.stmt;

        writer.println("STARTWHILECOND" + num + ": ");
        Register r = cond.accept(this);
        if (r == null) {
            return null;
        }
        writer.println("    bne  " + r.toString() + ", $zero, WHILESTATEMENT" + num);
        writer.println("    j    ENDWHILE" + num);
        writer.println("WHILESTATEMENT" + num + ": ");
        s.accept(this);
        writer.println("    j    STARTWHILECOND" + num);
        writer.println("ENDWHILE" + num + ": ");
        freeRegister(r);

        return null;
    }



    /**
        util methods
    */

    private void rectifyStackPointer() {
        int rectifier = 0;
        // if (offset % 4 != 0) {
        //     rectifier += 4;
        // }
        while (offset % 4 != 0) {
            rectifier ++;
            offset ++;
        }
        if (rectifier != 0) {
            writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -" + rectifier);
        }
    }

    private int totalStackedParamSize(List<VarDecl> varDecls) {
        int index = 0;
        int result = 0;
        for (VarDecl vd : varDecls) {
            if (index >= 4 || vd.type.size() > 4) {
                if (vd.type.size() >= 4) {
                    while (result % 4 != 0) {
                        result ++;
                    }
                }
                result += vd.type.size();
            }
            index ++ ;
        }
        while (result % 4 != 0) {
            result ++;
        }
        return result;
    }



    /**
        assignment functions
    */

    private Register getVarAddress(VarExpr v) {
        int size = v.decl.type.size();
        Register result = getRegister();
        if (v.decl.offset == -1 && v.decl.type.size() != 0) {
            if (v.decl.type instanceof StructType) {
                StructType st  = ((StructType) v.decl.type);
                String firstVarName = st.sd.varDecls.get(0).varName;
                // varName_structName_fieldName
                writer.println("    la   " + result.toString() + ", " + v.name + "_" + st.name + "_" + firstVarName);
            } else {
                writer.println("    la   " + result.toString() + ", " + v.name);
            }
        } else {
            int thisOffset = offset - v.decl.offset;
            writer.println("    addi " + result.toString() + ", " + Register.sp.toString() + ", " + thisOffset);
        }
        return result;
    }

    private Register getFieldAddress(FieldAccessExpr faexp) {

        Expr baseExp = faexp.base;
        StructType st = (StructType) baseExp.type;
        String field = faexp.field;

        if (baseExp instanceof VarExpr) {
            int thisOffset = ((VarExpr) baseExp).decl.offset;
            if (thisOffset == -1) {
                Register result = getRegister();
                writer.println("    la   " + result.toString() + ", " + ((VarExpr) baseExp).name + "_" + st.name + "_" + field);
                return result;
            }
        }

        Register result = baseExp.accept(this);
        if (result == null) {
            return null;
        }

        int i = 0;
        for (i = 0; i < st.sd.varDecls.size(); i ++) {
            VarDecl vd = st.sd.varDecls.get(i);
            if (vd.varName.equals(field)) {
                break;
            }
        }
        i = i * -4;

        writer.println("    add  " + result.toString() + ", " + result.toString() + ", " + i);

        return result;
    }

    private Register getArrayAccessAddress(ArrayAccessExpr aae) {
        Expr vd = aae.base;
        ArrayType at = (ArrayType) vd.type;
        int elemSize = at.type.size();

        Register base = vd.accept(this);
        if (base == null) {
            return null;
        }
        Register index = aae.index.accept(this);
        if (index == null) {
            freeRegister(base);
            return null;
        }

        Register result = getRegister();

        int storageDirection = -1 * elemSize;
        if (vd instanceof VarExpr) {
            storageDirection = (((VarExpr) vd).decl.offset == -1) ? (elemSize) : (-1 * elemSize);
        }
        writer.println("    li   " + result.toString() + ", " + storageDirection);
        writer.println("    mult " + result.toString() + ", " + index.toString());
        writer.println("    mflo " + result.toString());
        writer.println("    add  " + result.toString() + ", " + base.toString() + ", " + result.toString());
        // now result stores the address of the target element

        freeRegister(base);
        freeRegister(index);
        return result;
    }

    /**
        build-in functions
    */

    public void generatePrintI() {
        writer.println("    .text");
        writer.println("print_i:");
        writer.println("    li   $v0, 1");
        writer.println("    sw   $t0, -4(" + Register.sp.toString() +")");
        writer.println("    add  $t0, $a0, $zero");
        writer.println("    syscall");
        writer.println("    lw   $t0, -4(" + Register.sp.toString() +")");
        writer.println("    jr   " + Register.ra.toString() + "");
        writer.println();
    }

    public void generatePrintC() {
        writer.println("    .text");
        writer.println("print_c:");
        writer.println("    li   $v0, 11");
        writer.println("    sw   $t0, -4(" + Register.sp.toString() +")");
        writer.println("    add  $t0, $a0, $zero");
        writer.println("    syscall");
        writer.println("    lw   $t0, -4(" + Register.sp.toString() +")");
        writer.println("    jr   " + Register.ra.toString() + "");
        writer.println();
    }

    public void generatePrintS() {
        writer.println("    .text");
        writer.println("print_s:");
        writer.println("    li   $v0, 4");
        writer.println("    sw   $t0, -4(" + Register.sp.toString() +")");
        writer.println("    add  $t0, $a0, $zero");
        writer.println("    syscall");
        writer.println("    lw   $t0, -4(" + Register.sp.toString() +")");
        writer.println("    jr   " + Register.ra.toString() + "");
        writer.println();
    }

    public void generateMcmalloc() {
        writer.println("    .text");
        writer.println("mcmalloc:");
        writer.println("    li   $v0, 9");
        writer.println("    sw   $t0, -4(" + Register.sp.toString() +")");
        writer.println("    add  $t0, $a0, $zero");
        writer.println("    syscall");
        writer.println("    lw   $t0, -4(" + Register.sp.toString() +")");
        writer.println("    jr   " + Register.ra.toString() + "");
        writer.println();
    }

    public void generateReadI() {
        writer.println("    .text");
        writer.println("read_i:");
        writer.println("    li   $v0, 5");
        writer.println("    syscall");
        writer.println("    jr   " + Register.ra.toString() + "");
        writer.println();
    }

    public void generateReadC() {
        writer.println("    .text");
        writer.println("read_c:");
        writer.println("    li   $v0, 12");
        writer.println("    syscall");
        writer.println("    jr   " + Register.ra.toString() + "");
        writer.println();
    }
}
