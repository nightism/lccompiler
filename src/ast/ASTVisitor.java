package ast;

public interface ASTVisitor<T> {
    public T visitBaseType(BaseType bt);
    public T visitStructTypeDecl(StructTypeDecl st);
    public T visitBlock(Block b);
    public T visitFunDecl(FunDecl p);
    public T visitProgram(Program p);
    public T visitVarDecl(VarDecl vd);
    public T visitVarExpr(VarExpr v);

    // to complete ... (should have one visit method for each concrete AST node class)
    public T visitPointerType(PointerType pt);

    public T visitArrayAccessExpr(ArrayAccessExpr aae);
    public T visitArrayType(ArrayType at);
    public T visitAssign(Assign a);
    public T visitBinOp(BinOp bo);
    public T visitChrLiteral(ChrLiteral cl);
    public T visitExprStmt(ExprStmt es);
    public T visitFieldAccessExpr(FieldAccessExpr faexp);
    public T visitFunCallExpr(FunCallExpr fce);
    public T visitIf(If i);
    public T visitIntLiteral(IntLiteral il);
    public T visitOp(Op o);
    public T visitReturn(Return r);
    public T visitSizeOfExpr(SizeOfExpr soe);
    public T visitStrLiteral(StrLiteral sl);
    public T visitStructType(StructType st);
    public T visitTypecastExpr(TypecastExpr tce);
    public T visitValueAtExpr(ValueAtExpr vae);
    public T visitWhile(While w);

}
