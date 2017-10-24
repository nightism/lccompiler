package sem;

import java.util.ArrayList;

import ast.*;

public class SemanticAnalyzer {

    public int analyze(ast.Program prog) {
        Scope scope = buildInScope();
        // List of visitors
        ArrayList<SemanticVisitor> visitors = new ArrayList<SemanticVisitor>();
        visitors.add(new NameAnalysisVisitor(scope));
        visitors.add(new StructTypeCheckVisitor());
        visitors.add(new TypeCheckVisitor(scope));

        // Error accumulator
        int errors = 0;

        // Apply each visitor to the AST
        for (SemanticVisitor v : visitors) {
            prog.accept(v);
            errors += v.getErrorCount();
        }

        // Return the number of errors.
        return errors;
    }

    private Scope buildInScope() {
        Scope scope = new Scope(null);

        ArrayList<VarDecl> al1 = new ArrayList<VarDecl>();
        Block b1 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        al1.add(new VarDecl(new PointerType(BaseType.CHAR), "s"));
        scope.put(new FunSymbol(new FunDecl(BaseType.VOID, "print_s", al1, b1)));

        ArrayList<VarDecl> al2 = new ArrayList<VarDecl>();
        Block b2 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        al2.add(new VarDecl(BaseType.INT, "i"));
        scope.put(new FunSymbol(new FunDecl(BaseType.VOID, "print_i", al2, b2)));

        ArrayList<VarDecl> al3 = new ArrayList<VarDecl>();
        Block b3 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        al3.add(new VarDecl(BaseType.CHAR, "c"));
        scope.put(new FunSymbol(new FunDecl(BaseType.VOID, "print_c", al3, b3)));

        ArrayList<VarDecl> al4 = new ArrayList<VarDecl>();
        Block b4 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        scope.put(new FunSymbol(new FunDecl(BaseType.CHAR, "read_c", al4, b4)));

        ArrayList<VarDecl> al5 = new ArrayList<VarDecl>();
        Block b5 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        scope.put(new FunSymbol(new FunDecl(BaseType.INT, "read_i", al5, b5)));

        ArrayList<VarDecl> al6 = new ArrayList<VarDecl>();
        Block b6 = new Block(new ArrayList<VarDecl>(), new ArrayList<Stmt>());
        al6.add(new VarDecl(BaseType.INT, "size"));
        scope.put(new FunSymbol(new FunDecl(new PointerType(BaseType.VOID), "mcmalloc", al6, b6)));

        return scope;
    }
}
