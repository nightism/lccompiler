package parser;

import ast.*;

import ast.BaseType;
import ast.PointerType;

import lexer.Token;
import lexer.Tokeniser;
import lexer.Token.TokenClass;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * @author cdubach
 * @author myiking
 */
public class Parser {

    private Token token;

    // use for backtracking (useful for distinguishing decls from procs when parsing a program for instance)
    private Queue<Token> buffer = new LinkedList<>();

    private final Tokeniser tokeniser;



    public Parser(Tokeniser tokeniser) {
        this.tokeniser = tokeniser;
    }

    public Program parse() {
        // get the first token
        nextToken();

        return parseProgram();
    }

    public int getErrorCount() {
        return error;
    }

    private int error = 0;
    private Token lastErrorToken;

    private void error(TokenClass... expected) {

        if (lastErrorToken == token) {
            // skip this error, same token causing trouble
            return;
        }

        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (TokenClass e : expected) {
            sb.append(sep);
            sb.append(e);
            sep = "|";
        }
        System.out.println("Parsing error: expected ("+sb+") found ("+token+") at "+token.position);

        error++;
        lastErrorToken = token;
    }

    /*
     * Look ahead the i^th element from the stream of token.
     * i should be >= 1
     */
    private Token lookAhead(int i) {
        // ensures the buffer has the element we want to look ahead
        while (buffer.size() < i)
            buffer.add(tokeniser.nextToken());
        assert buffer.size() >= i;

        int cnt=1;
        for (Token t : buffer) {
            if (cnt == i)
                return t;
            cnt++;
        }

        assert false; // should never reach this
        return null;
    }

    /*
     * Consumes the next token from the tokeniser or the buffer if not empty.
     */
    private void nextToken() {
        if (!buffer.isEmpty())
            token = buffer.remove();
        else
            token = tokeniser.nextToken();
    }

    /*
     * If the current token is equals to the expected one, then skip it, otherwise report an error.
     * Returns the expected token or null if an error occurred.
     */
    private Token expect(TokenClass... expected) {
        for (TokenClass e : expected) {
            if (e == token.tokenClass) {
                Token cur = token;
                nextToken();
                return cur;
            }
        }

        error(expected);
        return null;
    }

    /*
    * Returns true if the current token is equals to any of the expected ones.
    */
    private boolean accept(TokenClass... expected) {
        boolean result = false;
        for (TokenClass e : expected)
            result |= (e == token.tokenClass);
        return result;
    }

    private boolean match(Token target, TokenClass... expected) {
        boolean result = false;
        for (TokenClass e : expected)
            result |= (e == target.tokenClass);
        return result;
    }

    private boolean acceptType() {
        return acceptNormalType() || acceptStruct();
    }

    // private boolean acceptDeclType() {
        // return accept(TokenClass.INT, TokenClass.VOID, TokenClass.CHAR) &&
    // }

    private boolean acceptNormalType() {
        return accept(TokenClass.INT, TokenClass.VOID, TokenClass.CHAR);
    }

    private boolean acceptStruct() {
      return accept(TokenClass.STRUCT) && match(lookAhead(1), TokenClass.IDENTIFIER);
    }

    private boolean matchType(Token target) {
        return match(target, TokenClass.INT, TokenClass.VOID, TokenClass.CHAR, TokenClass.STRUCT);
    }

    /*
    * Start parsing
    #*/

    private Program parseProgram() {
        parseIncludes();
        // parseStructDecls();
        // parseVarDecls();
        // parseFunDecls();
        List<StructTypeDecl> stds = parseStructDecls();
        List<VarDecl> vds = parseVarDecls();
        List<FunDecl> fds = parseFunDecls();
        expect(TokenClass.EOF);
        return new Program(stds, vds, fds);
        // expect(TokenClass.EOF);
        // return null;
    }

    // includes are ignored, so does not need to return an AST node
    private void parseIncludes() {
        if (accept(TokenClass.INCLUDE)) {
            nextToken();
            expect(TokenClass.STRING_LITERAL);
            parseIncludes();
        }
    }

    /***************************************************/
    /********** PARSE BASIC PROGRAM STRUCTURE **********/
    /***************************************************/

    private List<StructTypeDecl> parseStructDecls() {
        List<StructTypeDecl> results = new ArrayList<StructTypeDecl>();

        if (accept(TokenClass.STRUCT)
            && match(lookAhead(1), TokenClass.IDENTIFIER)
            && match(lookAhead(2), TokenClass.LBRA)) {

            StructType type = parseStructType();

            expect(TokenClass.LBRA);
            List<VarDecl> varDecls = new ArrayList<VarDecl>();
            varDecls.addAll(parseVarDecls());
            expect(TokenClass.RBRA);

            expect(TokenClass.SC);

            if (type != null) {
                results.add(new StructTypeDecl(type, varDecls));
            }

            results.addAll(parseStructDecls());
        }
        return results;
    }

    private List<VarDecl> parseVarDecls() {
        List<VarDecl> results = new ArrayList<VarDecl>();
        List<VarDecl> rests = new ArrayList<VarDecl>();

        Type type = null;
        Token iden = null;
        Token n = null;
        boolean arrayFlag = false;
        if (acceptNormalType()
            && (match(lookAhead(1), TokenClass.IDENTIFIER)
              && match(lookAhead(2), TokenClass.SC)
            || match(lookAhead(1), TokenClass.ASTERIX)
              && match(lookAhead(2), TokenClass.IDENTIFIER)
              && match(lookAhead(3), TokenClass.SC))) {
            // normal var declaration
            type = parseType();
            iden = expect(TokenClass.IDENTIFIER);
            expect(TokenClass.SC);

        } else if (acceptNormalType()
            && (match(lookAhead(1), TokenClass.IDENTIFIER)
              && match(lookAhead(2), TokenClass.LSBR)
            || match(lookAhead(1), TokenClass.ASTERIX)
              && match(lookAhead(2), TokenClass.IDENTIFIER)
              && match(lookAhead(3), TokenClass.LSBR))) {
            // normal var array declaration
            type = parseType();
            iden = expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LSBR);
            n = expect(TokenClass.INT_LITERAL);
            expect(TokenClass.RSBR);
            expect(TokenClass.SC);
            arrayFlag = true;

        } else if (acceptStruct()
            && (match(lookAhead(2), TokenClass.IDENTIFIER)
              && match(lookAhead(3), TokenClass.SC)
            || match(lookAhead(2), TokenClass.ASTERIX)
              && match(lookAhead(3), TokenClass.IDENTIFIER)
              && match(lookAhead(4), TokenClass.SC))) {
            // struct var declaration
            type = parseType();
            iden = expect(TokenClass.IDENTIFIER);
            expect(TokenClass.SC);

        } else if (acceptStruct()
            && (match(lookAhead(2), TokenClass.IDENTIFIER)
              && match(lookAhead(3), TokenClass.LSBR)
            || match(lookAhead(2), TokenClass.ASTERIX)
              && match(lookAhead(3), TokenClass.IDENTIFIER)
              && match(lookAhead(4), TokenClass.LSBR))) {
            // struct var array declaration
            type = parseType();
            iden = expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LSBR);
            n = expect(TokenClass.INT_LITERAL);
            expect(TokenClass.RSBR);
            expect(TokenClass.SC);
            arrayFlag = true;

        }

        if (type != null && iden != null) {
            if (arrayFlag) {
                if (n != null) {
                    type = new ArrayType(type, n.data);
                    results.add(new VarDecl(type, iden.data));
                    rests = parseVarDecls();
                }
            } else {
                results.add(new VarDecl(type, iden.data));
                rests = parseVarDecls();
            }
        }

        results.addAll(rests);
        return results;
    }

    private List<FunDecl> parseFunDecls() {
        List<FunDecl> results = new ArrayList<FunDecl>();
        if (acceptType()) {

            Type type = parseType();
            Token iden = expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LPAR);

            List<VarDecl> varDecls = new ArrayList<VarDecl>();
            if (!accept(TokenClass.RPAR)) {
                varDecls.addAll(parseParamLst());
            }

            expect(TokenClass.RPAR);
            Block blk= parseBlk();

            if (type != null && iden != null && blk != null) {
                results.add(new FunDecl(type, iden.data, varDecls, blk));
            }

            results.addAll(parseFunDecls());
        }
        return results;
    }

    /******************************************/
    /********* PARSE INSIDE STRUCTURE *********/
    /******************************************/

    private Block parseBlk() {
        expect(TokenClass.LBRA);

        List<VarDecl> varDecls = parseVarDecls();
        List<Stmt> stmts = new ArrayList<Stmt>();

        while (!accept(TokenClass.RBRA) && getErrorCount() == 0) {
            Stmt thisOne = parseStmt();
            if (thisOne != null) {
                stmts.add(thisOne);
            }
        }
        expect(TokenClass.RBRA);
        return new Block(varDecls, stmts);
    }

    private Stmt parseStmt() {
        if (accept(TokenClass.LBRA)) {
            return parseBlk();
        } else if (accept(TokenClass.WHILE)) {
            return parseWhileStat();
        } else if (accept(TokenClass.IF)) {
            return parseIfStat();
        } else if (accept(TokenClass.RETURN)) {
            return parseReturnStat();
        } else { // encounter expression
            Stmt resultStmt = null;
            Expr assignee = parseExp();
            if (assignee == null) {
                resultStmt = null;
            } else if (accept(TokenClass.ASSIGN)) {
                expect(TokenClass.ASSIGN);
                Expr assigner = parseExp();
                if (assigner != null) {
                    resultStmt = new Assign(assignee, assigner);
                } else {
                    resultStmt = null;
                }
            }
            expect(TokenClass.SC);
            return resultStmt;
        }
        // should never reach here
        // return null;
    }

    private While parseWhileStat() {
        // while condition
        expect(TokenClass.WHILE);
        expect(TokenClass.LPAR);
        Expr cond = parseExp();
        expect(TokenClass.RPAR);
        // statement
        Stmt stmt = parseStmt();

        if (cond != null && stmt != null) {
            return new While(cond, stmt);
        } else {
            return null;
        }
    }

    private If parseIfStat() {
        // if condition
        expect(TokenClass.IF);
        expect(TokenClass.LPAR);
        Expr cond = parseExp();
        expect(TokenClass.RPAR);

        // statement
        Stmt ifStmt = parseStmt();
        Stmt elseStmt = null;

        // else statement
        if (accept(TokenClass.ELSE)) {
            expect(TokenClass.ELSE);
            elseStmt = parseStmt();
        }

        if (ifStmt != null || cond != null) {
            return new If(cond, ifStmt, elseStmt);
        } else {
            return null;
        }
    }

    private Return parseReturnStat() {
        expect(TokenClass.RETURN);
        Return stmtReturn = null;

        // return expression
        if (!accept(TokenClass.SC)) {
            Expr exp = parseExp();
            stmtReturn = new Return(exp);
        } else {
            stmtReturn = new Return();
        }
        expect(TokenClass.SC);

        return stmtReturn;
    }

    private Expr parseExp() {
        return parseSecondaryLogicalTerm();
    }

    private Expr parseSecondaryLogicalTerm() {
        return parseSecondaryLogicalTerm(parsePrimaryLogicalTerm());
    }

    private Expr parseSecondaryLogicalTerm(Expr operandOne) {
        if (accept(TokenClass.OR) && operandOne != null) {
            expect(TokenClass.OR);
            Expr operandTwo = parsePrimaryLogicalTerm();
            if (operandTwo != null) {
                return parseSecondaryLogicalTerm(new BinOp(operandOne, Op.OR, operandTwo));
            } else {
                return null;
            }
        } else {
            return operandOne;
        }
    }

    private Expr parsePrimaryLogicalTerm() {
        return parsePrimaryLogicalTerm(parseSecondaryRelationalTerm());
    }

    private Expr parsePrimaryLogicalTerm(Expr operandOne) {
        if (accept(TokenClass.AND) && operandOne != null) {
            expect(TokenClass.AND);
            Expr operandTwo = parseSecondaryRelationalTerm();
            if (operandTwo != null) {
                return parsePrimaryLogicalTerm(new BinOp(operandOne, Op.AND, operandTwo));
            } else {
                return null;
            }
        } else {
            return operandOne;
        }
    }

    private Expr parseSecondaryRelationalTerm() {
        return parseSecondaryRelationalTerm(parsePrimaryRelationalTerm());
    }

    private Expr parseSecondaryRelationalTerm(Expr operandOne) {
        if (accept(TokenClass.EQ, TokenClass.NE) && operandOne != null) {
            Token op = expect(TokenClass.EQ, TokenClass.NE);
            Expr operandTwo = parsePrimaryRelationalTerm();
            if (operandTwo != null && op != null) {
                if (op.tokenClass == TokenClass.EQ) {
                    return parseSecondaryRelationalTerm(new BinOp(operandOne, Op.EQ, operandTwo));
                } else { // op.tokenClass == TokenClass.NE
                    return parseSecondaryRelationalTerm(new BinOp(operandOne, Op.NE, operandTwo));
                }
            } else {
                return null;
            }
        } else {
            return operandOne;
        }
    }

    private Expr parsePrimaryRelationalTerm() {
        return parsePrimaryRelationalTerm(parseSecondaryArithmeticTerm());
    }

    private Expr parsePrimaryRelationalTerm(Expr operandOne) {
        if (accept(TokenClass.LT, TokenClass.GT, TokenClass.LE, TokenClass.GE) && operandOne != null) {
            Token op = expect(TokenClass.LT, TokenClass.GT, TokenClass.LE, TokenClass.GE);
            Expr operandTwo = parseSecondaryArithmeticTerm();

            if (operandTwo != null && op != null) {
                if (op.tokenClass == TokenClass.LT) {
                    return parsePrimaryRelationalTerm(new BinOp(operandOne, Op.LT, operandTwo));
                } else if (op.tokenClass == TokenClass.GT) {
                    return parsePrimaryRelationalTerm(new BinOp(operandOne, Op.GT, operandTwo));
                } else if (op.tokenClass == TokenClass.LE) {
                    return parsePrimaryRelationalTerm(new BinOp(operandOne, Op.LE, operandTwo));
                } else { // op.tokenClass == TokenClass.GE
                    return parsePrimaryRelationalTerm(new BinOp(operandOne, Op.GE, operandTwo));
                }
            } else {
                return null;
            }
        } else {
            return operandOne;
        }
    }

    private Expr parseSecondaryArithmeticTerm() {
        return parseSecondaryArithmeticTerm(parsePrimaryArithmeticTerm());
    }

    private Expr parseSecondaryArithmeticTerm(Expr operandOne) {
        if (accept(TokenClass.PLUS, TokenClass.MINUS) && operandOne != null) {
            Token op = expect(TokenClass.PLUS, TokenClass.MINUS);
            Expr operandTwo = parsePrimaryArithmeticTerm();

            if (operandTwo != null && op != null) {
                if (op.tokenClass == TokenClass.PLUS) {
                    return parseSecondaryArithmeticTerm(new BinOp(operandOne, Op.ADD, operandTwo));
                } else { // op.tokenClass == TokenClass.MINUS
                    return parseSecondaryArithmeticTerm(new BinOp(operandOne, Op.SUB, operandTwo));
                }
            } else {
                return null;
            }
        } else {
            return operandOne;
        }
    }

    private Expr parsePrimaryArithmeticTerm() {
        return parsePrimaryArithmeticTerm(parseSecondaryFactor());
    }

    private Expr parsePrimaryArithmeticTerm(Expr operandOne) {
        if (accept(TokenClass.ASTERIX, TokenClass.DIV, TokenClass.REM) && operandOne != null) {
            Token op = expect(TokenClass.ASTERIX, TokenClass.DIV, TokenClass.REM);
            Expr operandTwo = parseSecondaryFactor();

            if (operandTwo != null && op != null) {
                if (op.tokenClass == TokenClass.ASTERIX) {
                    return parsePrimaryArithmeticTerm(new BinOp(operandOne, Op.MUL, operandTwo));
                } else if (op.tokenClass == TokenClass.DIV) {
                    return parsePrimaryArithmeticTerm(new BinOp(operandOne, Op.DIV, operandTwo));
                } else { // op.tokenClass == TokenClass.REM
                    return parsePrimaryArithmeticTerm(new BinOp(operandOne, Op.MOD, operandTwo));
                }
            } else {
                return null;
            }
        } else {
            return operandOne;
        }
    }

    private Expr parseSecondaryFactor() {
        if (accept(TokenClass.MINUS)) {
            // Unary Minus
            if (expect(TokenClass.MINUS) != null) {
                Expr exp = parseSecondaryFactor();
                if (exp != null) {
                    return new BinOp(new IntLiteral(0), Op.SUB, exp);
                }
            }
        } else if (accept(TokenClass.ASTERIX)) {
            // Pointer indirection
            if (expect(TokenClass.ASTERIX) != null) {
                Expr exp = parseSecondaryFactor();
                if (exp != null) {
                    return new ValueAtExpr(exp);
                }
            }
        } else if (accept(TokenClass.LPAR)
                   && matchType(lookAhead(1))) {
            // Type cast
            expect(TokenClass.LPAR);
            Type t = parseType();
            expect(TokenClass.RPAR);
            Expr exp = parseSecondaryFactor();
            if (exp != null && t != null) {
                return new TypecastExpr(t, exp);
            }
        } else {
            return parsePrimaryFactor();
        }

        return null;
    }

    private Expr parsePrimaryFactor() {
        if (!accept(TokenClass.DOT) && !accept(TokenClass.LSBR)) {
            Expr base = parseBaseFactor();
            if (base != null) {
                return parsePrimaryFactorOperator(base);
            }
        }
        return null;
    }

    private Expr parsePrimaryFactorOperator(Expr base) {
        if (accept(TokenClass.DOT)) {
            // parse fieldaccess
            expect(TokenClass.DOT);
            Token iden = expect(TokenClass.IDENTIFIER);
            if (iden != null) {
                return parsePrimaryFactorOperator(new FieldAccessExpr(base, iden.data));
            } else {
                return null;
            }
        } else if (accept(TokenClass.LSBR)) {
            // parse arrayaccess
            expect(TokenClass.LSBR);
            Expr index = parseExp();
            expect(TokenClass.RSBR);
            if (index != null) {
                return parsePrimaryFactorOperator(new ArrayAccessExpr(base, index));
            } else {
                return null;
            }
        } else {
            return base;
        }
    }

    private Expr parseBaseFactor() {
        if (accept(TokenClass.LPAR)) {
            // parse expression inside parenthesis
            expect(TokenClass.LPAR);
            Expr exp = parseExp();
            expect(TokenClass.RPAR);
            return exp; // could be null
        } else if (accept(TokenClass.INT_LITERAL, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL)) {
            // parse literal factors
            Token t = expect(TokenClass.INT_LITERAL, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL);

            if (t == null) {
                return null;
            } else if (t.tokenClass == TokenClass.INT_LITERAL) {
                return new IntLiteral(t.data);
            } else if (t.tokenClass == TokenClass.CHAR_LITERAL) {
                return new ChrLiteral(t.data);
            } else if (t.tokenClass == TokenClass.STRING_LITERAL) {
                return new StrLiteral(t.data);
            } else {
                // should never reach this point
                return null;
            }
        } else if (accept(TokenClass.SIZEOF)) {
            // parse sizeof(type)
            // sizeof ::= "sizeof" "(" type ")"
            expect(TokenClass.SIZEOF);

            expect(TokenClass.LPAR);
            Type t = parseType();
            Expr result = null;
            if (t != null) {
                result = new SizeOfExpr(t);
            }
            expect(TokenClass.RPAR);

            return result;
        } else { // accept(TokenClass.IDENTIFIER)
            // parse identifier
            Token token = expect(TokenClass.IDENTIFIER);

            if (accept(TokenClass.LPAR) && (token != null)) {
                // parse function call
                // funcall ::= IDENT "(" [ exp ("," exp)* ] ")"
                List<Expr> funcallParam = new ArrayList<Expr>();
                expect(TokenClass.LPAR);

                if (!accept(TokenClass.RPAR)) {
                    funcallParam = parseFuncallParamLst();
                }
                expect(TokenClass.RPAR);

                return new FunCallExpr(token.data, funcallParam);
            } else if (token != null) {
                return new VarExpr(token.data);
            } else {
                return null;
            }
        }
    }

    private List<Expr> parseFuncallParamLst() {
        List<Expr> results = new ArrayList<Expr>();
        Expr exp = parseExp();
        if (exp != null) {
            results.add(exp);
        }

        if (accept(TokenClass.COMMA)) {
            expect(TokenClass.COMMA);
            List<Expr> rest = parseFuncallParamLst();
            results.addAll(rest);
        }
        return results;
    }

    private List<VarDecl> parseParamLst() {
        List<VarDecl> results = new ArrayList<VarDecl>();

        Type t = parseType();
        Token iden = expect(TokenClass.IDENTIFIER);
        if (iden != null && t != null) {
            results.add(new VarDecl(t, iden.data));
        }

        if (accept(TokenClass.COMMA)) {
            expect(TokenClass.COMMA);
            List<VarDecl> rest = parseParamLst();
            results.addAll(rest);
        }

        return results; // could be empty list
    }

    private Type parseType() {
        Type t = null;
        if(accept(TokenClass.STRUCT)) {
            t = parseStructType();
        } else {
            Token token = expect(TokenClass.INT, TokenClass.VOID, TokenClass.CHAR);
            if (token != null) {
                if (token.tokenClass == TokenClass.INT) {
                    t = BaseType.INT;
                } else if (token.tokenClass == TokenClass.VOID) {
                    t = BaseType.VOID;
                } else if (token.tokenClass == TokenClass.CHAR) {
                    t = BaseType.CHAR;
                }
            }
        }

        // if encountering pointer declaration
        if (accept(TokenClass.ASTERIX) && (t != null)) {
            expect(TokenClass.ASTERIX);
            return new PointerType(t);
        }
        return t; // could be none
    }

    private StructType parseStructType() {
        expect(TokenClass.STRUCT);
        Token iden = expect(TokenClass.IDENTIFIER);
        if (iden != null) {
            return new StructType(iden.data);
        } else {
            return null;
        }
    }

    // private List<StructTypeDecl> parseStructDecls() {
    //     // to be completed ...
    //     return null;
    // }

}
