package gen;

import ast.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EmptyStackException;
import java.util.Stack;

public class CodeGenerator implements ASTVisitor<Register> {

    /*
     * Simple register allocator.
     */

    private int strNum;    // string literal index number
    private int boNum;     // binary operatoin index number
    private int offset;    // variable stack offset

    // contains all the free temporary registers
    private Stack<Register> freeRegs = new Stack<Register>();

    public CodeGenerator() {
        freeRegs.addAll(Register.tmpRegs);
        strNum = 0;
        boNum = 0;
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
        for (VarDecl vd : b.varDecls) {
            // less than 4 variables
            int size = vd.type.size();
            if (size == 1) {
                // char or char* or void*
                writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -1");
                vd.offset = offset;
                offset = offset + 1;
            } else if (size == 4) {
                // int or int*
                rectifyStackPointer();
                writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -4");
                vd.offset = offset;
                offset = offset + 4;
            } else { // size > 4
                // TODO
            }
        }

        rectifyStackPointer();

        for (Stmt st : b.stmts) {
            st.accept(this);
        }

        return null;
    }

    @Override
    public Register visitFunDecl(FunDecl p) {
        // HashMap<String, Integer> offsetMap = new HashMap<String, Integer>();
        // TODO to be finished

        // label the function with its name
        writer.println("    .text");
        writer.println(p.name + ":");

        // change $fp to $sp 
        writer.println("    add  " + Register.fp.toString()+ ", " + Register.sp.toString() +", $zero");

        // save all parameters on stack
        int numOfParam = p.params.size();
        int paramIndex = 0;
        for (VarDecl vd : p.params) {
            if (paramIndex < 4) {
                // less than 4 variables
                int size = vd.type.size();
                if (size == 1) {
                    // char or char* or void*
                    writer.println("    sb   $a" + paramIndex + ", 0(" + Register.sp.toString() + ")");
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -1");
                    vd.offset = offset;
                    offset = offset + 1;
                } else { // size == 4
                    // int or int*
                    rectifyStackPointer();

                    writer.println("    sw   $a" + paramIndex + ", 0(" + Register.sp.toString() + ")");
                    writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -4");
                    vd.offset = offset;
                    offset = offset + 4;
                }
            } else {
                // TODO retrieve data from stack
                // TODO retrieve struct variables
            }
            paramIndex ++;
        }

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
                for (VarDecl v : t.sd.varDecls) {
                    int size = ((v.type.size() - 1) / 4 + 1) * 4;
                    // name : varName_structName_fieldName
                    writer.print(varName + "_" + structName + "_" + v.varName);
                    writer.println(":  .space  " + size);
                }
            } else {
                // declare normal variables
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


        writer.flush();
        return null;
    }

    @Override
    public Register visitVarDecl(VarDecl vd) {
        // TODO: to complete
        return null;
    }

    @Override
    public Register visitVarExpr(VarExpr v) {
        // TODO: to complete
        return null;
    }


    /**
        more visitor methods
    */


    @Override
    public Register visitArrayAccessExpr(ArrayAccessExpr aae) {
        return null;
    }

    @Override
    public Register visitArrayType(ArrayType at) {
        return null;
    }

    @Override
    public Register visitAssign(Assign a) {
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
        return null;
    }

    @Override
    public Register visitFunCallExpr(FunCallExpr fce) {
        int numOfParam = 0;

        // save $fp and $ra on the stack
        writer.println("    sw   " + Register.fp.toString()+ ", 0(" + Register.sp.toString() +")");
        writer.println("    sw   " + Register.ra.toString() + ", -4(" + Register.sp.toString() +")");
        writer.println("    addi " + Register.sp.toString() +", " + Register.sp.toString() +", -8");
        offset -= 8;

        // TODO passing parameters
        for (Expr p : fce.params) {
            Register r = p.accept(this);

            if (r == null) {
                break;
            }

            // store in a0-3 or store in the stack
            // TODO stuct in the first 4 arg
            if (numOfParam < 4) {
                writer.println("    add  $a" + numOfParam + ", $zero, " + r.toString());
                freeRegister(r);
            } else {
                // TODO to be finished this part
                writer.println("    # more than 4 args");
            }
            numOfParam ++;
        }

        // resest offset
        int oldStackOffset = offset;
        offset = 0;

        // jump back to the caller function
        writer.println("    jal  " + fce.name);

        // restore $fp and $ra
        writer.println("    lw   " + Register.fp.toString()+ ", 8(" + Register.sp.toString() +")");
        writer.println("    lw   " + Register.ra.toString() + ", 4(" + Register.sp.toString() +")");
        writer.println("    addi " + Register.sp.toString() +", " + Register.sp.toString() +", 8");
        offset += 8;

        // TODO store return value -> struct more than 4 bytes
        Register result = getRegister();
        writer.println("    add  " + result.toString() + ", $zero, $v0");

        // restore offset
        offset = oldStackOffset;

        return result;
    }

    @Override
    public Register visitIf(If i) {
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
            Register reg = e.accept(this);
            if (reg != null) {
                writer.println("    add  $v0, $zero, " + reg.toString());
                freeRegister(reg);
            }
        }
        return null;
    }

    @Override
    public Register visitSizeOfExpr(SizeOfExpr soe) {
        int size = soe.type.size();
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
        writer.println("str" + strNum + ":  .asciiz  \"" + str + "\"");
        // back to text section and store the string in register
        writer.println("    .text");
        writer.println("    la " + result.toString() + ", str" + strNum);
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
        return null;
    }

    @Override
    public Register visitWhile(While w) {
        return null;
    }


    /**
        util methods
    */

    public void rectifyStackPointer() {
        int rectifier = 0;
        while (offset % 4 != 0) {
            rectifier ++;
            offset ++;
        }
        if (rectifier != 0) {
            writer.println("    addi " + Register.sp.toString() + ", " + Register.sp.toString() + ", -" + rectifier);
        }
    }


    /**
        build-in functions
    */

    public void generatePrintI() {
        writer.println("    .text");
        writer.println("print_i:");
        writer.println("    li   $v0, 1");
        writer.println("    sw   $t0, 0(" + Register.sp.toString() +")");
        writer.println("    add  $t0, $a0, $zero");
        writer.println("    syscall");
        writer.println("    lw   $t0, 0(" + Register.sp.toString() +")");
        writer.println("    jr   " + Register.ra.toString() + "");
        writer.println();
    }

    public void generatePrintC() {
        writer.println("    .text");
        writer.println("print_c:");
        writer.println("    li   $v0, 4");
        writer.println("    sw   $t0, 0(" + Register.sp.toString() +")");
        writer.println("    add  $t0, $a0, $zero");
        writer.println("    syscall");
        writer.println("    lw   $t0, 0(" + Register.sp.toString() +")");
        writer.println("    jr   " + Register.ra.toString() + "");
        writer.println();
    }

    public void generatePrintS() {
        writer.println("    .text");
        writer.println("print_s:");
        writer.println("    li   $v0, 4");
        writer.println("    sw   $t0, 0(" + Register.sp.toString() +")");
        writer.println("    add  $t0, $a0, $zero");
        writer.println("    syscall");
        writer.println("    lw   $t0, 0(" + Register.sp.toString() +")");
        writer.println("    jr   " + Register.ra.toString() + "");
        writer.println();
    }
}
