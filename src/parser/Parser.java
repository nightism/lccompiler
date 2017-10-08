package parser;

import lexer.Token;
import lexer.Tokeniser;
import lexer.Token.TokenClass;

import java.util.LinkedList;
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

    public void parse() {
        // get the first token
        nextToken();

        parseProgram();
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
        return accept(TokenClass.INT, TokenClass.VOID, TokenClass.CHAR, TokenClass.STRUCT);
    }

    private boolean matchType(Token target) {
        return match(target, TokenClass.INT, TokenClass.VOID, TokenClass.CHAR, TokenClass.STRUCT);
    }

    /**
    * Starts parsing
    */
    private void parseProgram() {
        parseIncludes();
        parseStructDecls();
        parseVarDecls();
        parseFunDecls();
        expect(TokenClass.EOF);
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

    private void parseStructDecls() {
        if (accept(TokenClass.STRUCT)
            && match(lookAhead(1), TokenClass.IDENTIFIER)
            && match(lookAhead(2), TokenClass.LBRA)) {

            parseStructType();
            expect(TokenClass.LBRA);
            parseVarDecls();
            expect(TokenClass.RBRA);
            expect(TokenClass.SC);

            parseStructDecls();
        }
    }

    private void parseVarDecls() {
        if (acceptType()
            && match(lookAhead(1), TokenClass.IDENTIFIER)
            && match(lookAhead(2), TokenClass.SC)) {

            parseType();
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.SC);

            parseVarDecls();
        } else if (acceptType()
            && match(lookAhead(1), TokenClass.IDENTIFIER)
            && match(lookAhead(2), TokenClass.LSBR)) {

            parseType();
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LSBR);
            expect(TokenClass.INT_LITERAL);
            expect(TokenClass.RSBR);
            expect(TokenClass.SC);

            parseVarDecls();
        } else if (accept(TokenClass.STRUCT)
            && match(lookAhead(1), TokenClass.IDENTIFIER)
            && match(lookAhead(2), TokenClass.IDENTIFIER)) {

            parseType();
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.SC);

            parseVarDecls();
        }
    }

    private void parseFunDecls() {
        if (acceptType()
            && match(lookAhead(1), TokenClass.IDENTIFIER)
            && match(lookAhead(2), TokenClass.LPAR)) {

            parseType();
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LPAR);

            if (!accept(TokenClass.RPAR)) {
                parseParamLst();
            }

            expect(TokenClass.RPAR);
            parseBlk();

            parseFunDecls();
        }
    }

    /******************************************/
    /********* PARSE INSIDE STRUCTURE *********/
    /******************************************/

    private void parseBlk() {
        expect(TokenClass.LBRA);

        parseVarDecls();

        while (!accept(TokenClass.RBRA) && getErrorCount() == 0) {
            parseStmt();
        }

        expect(TokenClass.RBRA);
    }

    private void parseStmt() {
        if (accept(TokenClass.LBRA)) {
            parseBlk();
        } else if (accept(TokenClass.WHILE)) {
            parseWhileStat();
        } else if (accept(TokenClass.IF)) {
            parseIfStat();
        } else if (accept(TokenClass.RETURN)) {
            parseReturnStat();
        } else { // encounter expression
            parseExp();
            if (accept(TokenClass.ASSIGN)) {
                expect(TokenClass.ASSIGN);
                parseExp();
            }
            expect(TokenClass.SC);
        }
    }

    private void parseWhileStat() {
        // while condition
        expect(TokenClass.WHILE);
        expect(TokenClass.LPAR);
        parseExp();
        expect(TokenClass.RPAR);
        // statement
        parseStmt();
    }

    private void parseIfStat() {
        // if condition
        expect(TokenClass.IF);
        expect(TokenClass.LPAR);
        parseExp();
        expect(TokenClass.RPAR);
        // statement
        parseStmt();

        // else statement
        if (accept(TokenClass.ELSE)) {
            expect(TokenClass.ELSE);
            parseStmt();
        }
    }

    private void parseReturnStat() {
        expect(TokenClass.RETURN);

        // return expression
        if (!accept(TokenClass.SC)) {
            parseExp();
        }

        expect(TokenClass.SC);
    }

    private void parseExp() {
        parseSecondaryLogicalTerm();
    }


    private void parseSecondaryLogicalTerm() {
        parsePrimaryLogicalTerm();
        if (accept(TokenClass.OR)) {
            expect(TokenClass.OR);
            parseSecondaryLogicalTerm();
        }
    }

    private void parsePrimaryLogicalTerm() {
        parseSecondaryRelationalTerm();
        if (accept(TokenClass.AND)) {
            expect(TokenClass.AND);
            parsePrimaryLogicalTerm();
        }
    }

    private void parseSecondaryRelationalTerm() {
        parsePrimaryRelationalTerm();
        if (accept(TokenClass.EQ, TokenClass.NE)) {
            expect(TokenClass.EQ, TokenClass.NE);
            parseSecondaryRelationalTerm();
        }
    }

    private void parsePrimaryRelationalTerm() {
        parseSecondaryArithmeticTerm();
        if (accept(TokenClass.LT, TokenClass.GT, TokenClass.LE, TokenClass.GE)) {
            expect(TokenClass.LT, TokenClass.GT, TokenClass.LE, TokenClass.GE);
            parsePrimaryRelationalTerm();
        }
    }

    private void parseSecondaryArithmeticTerm() {
        parsePrimaryArithmeticTerm();
        if (accept(TokenClass.PLUS, TokenClass.MINUS)) {
            expect(TokenClass.PLUS, TokenClass.MINUS);
            parseSecondaryArithmeticTerm();
        }
    }


    private void parsePrimaryArithmeticTerm() {
        parseSecondaryFactor();
        if (accept(TokenClass.ASTERIX, TokenClass.DIV, TokenClass.REM)) {
            expect(TokenClass.ASTERIX, TokenClass.DIV, TokenClass.REM);
            parsePrimaryArithmeticTerm();
        }
    }

    private void parseSecondaryFactor() {
        if (accept(TokenClass.MINUS)) {
            // Unary Minus
            expect(TokenClass.MINUS);
            parseSecondaryFactor();
        } else if (accept(TokenClass.ASTERIX)) {
            // Pointer indirection
            expect(TokenClass.ASTERIX);
            parseSecondaryFactor();
        } else if (accept(TokenClass.LPAR)
                   && matchType(lookAhead(1))) {
            // Type cast
            expect(TokenClass.LPAR);
            parseType();
            expect(TokenClass.RPAR);
            parseSecondaryFactor();
        } else {
            parsePrimaryFactor();
        }
    }

    private void parsePrimaryFactor() {
        if (!accept(TokenClass.DOT) && !accept(TokenClass.LSBR)) {
            parseBaseFactor();
            parsePrimaryFactorOperator();
        }
    }

    private void parsePrimaryFactorOperator() {
        if (accept(TokenClass.DOT)) {
            // parse fieldaccess
            expect(TokenClass.DOT);
            expect(TokenClass.IDENTIFIER);
            parsePrimaryFactorOperator();
        } else if (accept(TokenClass.LSBR)) {
            // parse arrayaccess
            expect(TokenClass.LSBR);
            parseExp();
            expect(TokenClass.RSBR);
            parsePrimaryFactorOperator();
        }
    }

    private void parseBaseFactor() {
        if (accept(TokenClass.LPAR)) {
            // parse expression inside parenthesis
            expect(TokenClass.LPAR);
            parseExp();
            expect(TokenClass.RPAR);
        } else if (accept(TokenClass.INT_LITERAL, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL)) {
            // parse literal factors
            expect(TokenClass.INT_LITERAL, TokenClass.CHAR_LITERAL, TokenClass.STRING_LITERAL);
        } else if (accept(TokenClass.SIZEOF)) {
            // parse sizeof(type)
            // sizeof ::= "sizeof" "(" type ")"
            expect(TokenClass.SIZEOF);
            expect(TokenClass.LPAR);
            parseType();
            expect(TokenClass.RPAR);
        } else { // accept(TokenClass.IDENTIFIER)
            // parse identifier
            expect(TokenClass.IDENTIFIER);
            if (accept(TokenClass.LPAR)) {
                // parse function call
                // funcall ::= IDENT "(" [ exp ("," exp)* ] ")"
                expect(TokenClass.LPAR);
                if (!accept(TokenClass.RPAR)) {
                    parseFuncallParamLst();
                }
                expect(TokenClass.RPAR);
            }
        }
    }

    private void parseFuncallParamLst() {
        parseExp();
        if (accept(TokenClass.COMMA)) {
            expect(TokenClass.COMMA);
            parseFuncallParamLst();
        }
    }

    private void parseParamLst() {
        parseType();
        expect(TokenClass.IDENTIFIER);
        if (accept(TokenClass.COMMA)) {
            expect(TokenClass.COMMA);
            parseParamLst();
        }
    }

    private void parseType() {
        if(accept(TokenClass.STRUCT)) {
            parseStructType();
        } else {
            expect(TokenClass.INT, TokenClass.VOID, TokenClass.CHAR);
        }

        // if encountering pointer declaration
        if (accept(TokenClass.ASTERIX)) {
            expect(TokenClass.ASTERIX);
        }
    }

    private void parseStructType() {
        expect(TokenClass.STRUCT);
        expect(TokenClass.IDENTIFIER);
    }
}
