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
        while (accept(TokenClass.INT, TokenClass.VOID, TokenClass.CHAR, TokenClass.STRUCT)) {
            if (match(lookAhead(1), TokenClass.IDENTIFIER) && match(lookAhead(2), TokenClass.SC)) {
                parseType();
                expect(TokenClass.IDENTIFIER);
                expect(TokenClass.SC);
            } else {
                break;
            }
        }
    }

    private void parseFunDecls() {
        while (accept(TokenClass.INT, TokenClass.VOID, TokenClass.CHAR, TokenClass.STRUCT)) {
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

    private void parseParamLst() {
        while (accept(TokenClass.INT, TokenClass.VOID, TokenClass.CHAR)) {
            //TODO
        }
    }

    private void parseBlk() {
        // TODO
    }

    private boolean parseType() {
        if(accept(TokenClass.STRUCT)) {
            parseStructType();
        } else {
            expect(TokenClass.INT, TokenClass.VOID, TokenClass.CHAR);
        }

        if (accept(TokenClass.ASTERIX)) {
            expect(TokenClass.ASTERIX);
        }
    }

    private boolean parseStructType() {
        expect(TokenClass.STRUCT);
        expect(TokenClass.IDENTIFIER);
    }
}
