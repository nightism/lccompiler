package ast;

public class BinOp extends Expr {

    public enum Op {
        ADD, SUB, MUL, DIV, MOD, GT, LT, GE, LE, NE, EQ, OR, AND;
    }

    public final Expr operandOne;
    public final Expr operandTwo;
    public final Op oprator;

    public BinOp(Expr operandOne, Expr operandTwo, Op oprator) {
        this.operandOne = operandOne;
        this.operandTwo = operandTwo;
        this.operator = operator;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitBinOp(this);
    }
}
