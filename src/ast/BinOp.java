package ast;

public class BinOp extends Expr {

    public final Expr operandOne;
    public final Expr operandTwo;
    public final Op operator;

    public BinOp(Expr operandOne, Op operator, Expr operandTwo) {
        this.operandOne = operandOne;
        this.operandTwo = operandTwo;
        this.operator = operator;
    }

    public <T> T accept(ASTVisitor<T> v) {
        return null;
        // return v.visitBinOp(this);
    }
}
