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

    /************************************/
    /********** Kleene closure **********/
    /************************************/

    private void parseStructDecls() {
        while (accept(TokenClass.STRUCT)) {
            nextToken();
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LBRA);
            parseVarDecls();
            expect(TokenClass.RBRA);
            expect(TokenClass.SC);
        }
    }

    private void parseVarDecls() {
        while (acceptType()) {
            if (match(lookAhead(1), TokenClass.IDENTIFIER) && match(lookAhead(2), TokenClass.SC)) {
                parseType();
                expect(TokenClass.IDENTIFIER);

                if (accept(TokenClass.SC)) {
                // normal variables declaration
                    expect(TokenClass.SC);
                } else if (accept(TokenClass.LSBR)) {
                // arrays declaration
                    expect(TokenClass.LSBR);
                    expect(TokenClass.INT_LITERAL);
                    expect(TokenClass.RSBR);
                    expect(TokenClass.SC);
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private void parseFunDecls() {
        while (acceptType()) {
            if (match(lookAhead(1), TokenClass.IDENTIFIER) && match(lookAhead(2), TokenClass.LPAR)) {
                parseType();
                expect(TokenClass.IDENTIFIER);
                expect(TokenClass.LPAR);
                parseParamLst();
                expect(TokenClass.RPAR);
                parseBlk();
            } else {
                break;
            }
        }
    }

    /************************************/
    /********* Positive closure *********/
    /************************************/

    private void parseBlk() {
        expect(TokenClass.LBRA);
        parseVarDecls();

        while (!accept(TokenClass.RBRA)) {
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
            // TODO bugs here
            parseExp();
            // System.out.println("test1 " + token.data);
            if (accept(TokenClass.ASSIGN)) {
                expect(TokenClass.ASSIGN);
                parseExp();
            }
            // System.out.println("test3 " + token.data);
            expect(TokenClass.SC);
        }
    }

    private void parseExp() {
        // TODO
        if (accept(TokenClass.LPAR)) {
            expect(TokenClass.LPAR);
            if (acceptType()) {
            // parse typecase

                parseType();
                expect(TokenClass.RPAR);
                parseExp();
            } else {
            // parse expression inside parenthesis

                parseExp();
                expect(TokenClass.RPAR);
            }
        } else if (accept(TokenClass.IDENTIFIER) && match(lookAhead(1), TokenClass.LPAR)) {
        // funcall ::= IDENT "(" [ exp ("," exp)* ] ")"

            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.LPAR);
            parseExp();

            while (accept(TokenClass.COMMA)) {
                expect(TokenClass.COMMA);
                parseExp();
            }

            expect(TokenClass.RPAR);
        } else if (accept(TokenClass.MINUS)) {
        // parse negative expression

            expect(TokenClass.MINUS);
            expect(TokenClass.IDENTIFIER, TokenClass.INT_LITERAL);
        } else if (accept(TokenClass.IDENTIFIER, TokenClass.INT_LITERAL)) {
        // parse positive expression

            expect(TokenClass.IDENTIFIER, TokenClass.INT_LITERAL);
        } else if (accept(TokenClass.CHAR_LITERAL)) {
        // parse character expression
            // TODO to be merged and revised
            expect(TokenClass.CHAR_LITERAL);
        } else if (accept(TokenClass.STRING_LITERAL)) {
        // parse String expression
            // TODO to be merged and revised
            expect(TokenClass.STRING_LITERAL);
        } else if (accept(TokenClass.ASTERIX)) {
        // valueat ::= "*" exp    #### Value at operator (pointer indirection)
            // TODO to be merged and revised
            expect(TokenClass.ASTERIX);
            parseExp();
        } else if (accept(TokenClass.SIZEOF)) {
        // sizeof ::= "sizeof" "(" type ")"
            // TODO to be merged and revised
            expect(TokenClass.SIZEOF);
            expect(TokenClass.LPAR);
            parseType();
            expect(TokenClass.RPAR);
        } else {
        // encounter expressions

            // TODO to be refined
            // refer to the lecture notes
            if (accept(TokenClass.DOT)) {
                expect(TokenClass.DOT);
                expect(TokenClass.IDENTIFIER);
            } else {
                expect(TokenClass.AND, TokenClass.OR, TokenClass.EQ, TokenClass.NE, TokenClass.LT,
                       TokenClass.GT, TokenClass.LE, TokenClass.GE, TokenClass.PLUS, TokenClass.MINUS,
                       TokenClass.ASTERIX, TokenClass.DIV, TokenClass.REM);
                parseExp();
            }
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
                   && match(lookAhead(1), TokenClass.IDENTIFIER)
                   && match(lookAhead(2), TokenClass.RPAR) ) {
            // Type cast
            expect(TokenClass.LPAR);
            expect(TokenClass.IDENTIFIER);
            expect(TokenClass.RPAR);
            parseSecondaryFactor();
        } else {
            parsePrimaryFactor();
        }
    }

    private void parsePrimaryFactor() {
        parseBaseFactor();
        parsePrimaryFactorOperator();
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
        } else if (accept(TokenClass.IDENTIFIER)) {
            // parse identifier
            expect(TokenClass.IDENTIFIER);
            if (accept(TokenClass.LBRA)) {
                // parse function call
                // funcall ::= IDENT "(" [ exp ("," exp)* ] ")"
                expect(TokenClass.LBRA);
                if (!accept(TokenClass.RPAR)) {
                    parseFuncallParamLst();
                }
                expect(TokenClass.RBRA);
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
