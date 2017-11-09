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

    // contains all the free temporary registers
    private Stack<Register> freeRegs = new Stack<Register>();

    public CodeGenerator() {
        freeRegs.addAll(Register.tmpRegs);
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
        if (b.varDecls.size() != 0) {
            writer.println("    .data");
        }
        for (VarDecl vd : b.varDecls) {
            vd.accept(this);
        }

        writer.println("    .text");
        for (Stmt st : b.stmts) {
            st.accept(this);
        }

        writer.println();
        return null;
    }

    @Override
    public Register visitFunDecl(FunDecl p) {
        if (p.params.size() != 0) {
            writer.println("    .data");
        }
        for (VarDecl vd : p.params) {
            vd.accept(this);
        }

        writer.println("    .text");
        writer.println(p.name + ":");

        p.block.accept(this);

        // TODO tobe refined
        if (p.name.equals("main")) {
            writer.println("    li   $v0, 10");
            writer.println("    syscall");
        }


        return null;
    }

    @Override
    public Register visitProgram(Program p) {
        writer.println(".data");
        for (VarDecl vd : p.varDecls) {
            vd.accept(this);
        }

        writer.println(".text");
        writer.println("    j    main");
        writer.println();

        generatePrintI();

        for (FunDecl fd : p.funDecls) {
            fd.accept(this);
        }

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


    // TODO: to complete


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
        return null;
    }

    @Override
    public Register visitChrLiteral(ChrLiteral cl) {
        return null;
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
        for (Expr p : fce.params) {
            Register r = p.accept(this);
            // TODO to be finished and refined
            writer.println("    add  $a0, $zero, " + r.toString());
            freeRegister(r);
        }
        writer.println("    jal  " + fce.name);
        Register result = getRegister();
        // TODO to be refined
        writer.println("    add  " + result.toString() + ", $zero, $v0");
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
        return null;
    }

    @Override
    public Register visitSizeOfExpr(SizeOfExpr soe) {
        return null;
    }

    @Override
    public Register visitStrLiteral(StrLiteral sl) {
        return null;
    }

    @Override
    public Register visitStructType(StructType st) {
        return null;
    }

    @Override
    public Register visitTypecastExpr(TypecastExpr tce) {
        return null;
    }

    @Override
    public Register visitValueAtExpr(ValueAtExpr vae) {
        return null;
    }

    @Override
    public Register visitWhile(While w) {
        return null;
    }

    public void generatePrintI() {
      // to be refined with STACK oprations
        writer.println("    .text");
        writer.println("print_i:");
        writer.println("    li   $v0, 1");
        writer.println("    add  $t0, $a0, $zero");
        writer.println("    syscall");
        writer.println("    jr   $ra");
        writer.println();
    }
}
