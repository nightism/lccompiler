package lexer;

import lexer.Token.TokenClass;

import java.io.EOFException;
import java.io.IOException;

/**
 * @author cdubach
 */
public class Tokeniser {

    private class EoLException extends Exception {
    }

    private Scanner scanner;

    private int error = 0;
    public int getErrorCount() {
        return this.error;
    }

    public Tokeniser(Scanner scanner) {
        this.scanner = scanner;
    }

    private void error(char c, int line, int col) {
        System.out.println("Lexing error: unrecognised character ("+c+") at "+line+":"+col);
        error++;
    }


    public Token nextToken() {
        Token result;
        try {
             result = next();
        } catch (EOFException eof) {
            // end of file, nothing to worry about, just return EOF token
            return new Token(TokenClass.EOF, scanner.getLine(), scanner.getColumn());
        } catch (IOException ioe) {
            ioe.printStackTrace();
            // something went horribly wrong, abort
            System.exit(-1);
            return null;
        }
        return result;
    }

    private Token next() throws IOException {

        int line = scanner.getLine();
        int column = scanner.getColumn();

        // get the next character
        char c = scanner.next();

        // skip white spaces, carriage return (\r) and line feed (\n)
        if (Character.isWhitespace(c)) {
            return next();
        }

        /****** skip the comments ******/
        if (c == '/') {
            if (scanner.peek() == '/') {
                skipLine();
                return next();
            } else if (scanner.peek() == '*') {
                try{
                    char nextOne = scanner.next();
                    while (!(nextOne == '*' && scanner.peek() == '/')) {
                        nextOne = scanner.next();
                    }
                    scanner.next();
                    return next();
                } catch (EOFException eof) {
                    error(c, line, column);
                    return new Token(TokenClass.INVALID, line, column);
                }
            }
        }

        /****** recognises the header files ******/
        // TODO need to be refined
        if (c == '#') {
            int thisCol = column;
            String includeToken = Character.toString(c);
            char nextOne = scanner.next();
            while (!Character.isWhitespace(nextOne)) {
                includeToken = includeToken + Character. toString(nextOne);
                nextOne = scanner.next();
            }
            if (includeToken.equals("#include")) {
                // TODO need to check whether the following is a valid string
                return new Token(TokenClass.INCLUDE, line, thisCol);
            }
        }

        /****** recognises the delimiters ******/
        if (c == '{') {
            return new Token(TokenClass.LBRA, line, column);
        }

        if (c == '}') {
            return new Token(TokenClass.RBRA, line, column);
        }

        if (c == '(') {
            return new Token(TokenClass.LPAR, line, column);
        }

        if (c == ')') {
            return new Token(TokenClass.RPAR, line, column);
        }

        if (c == '[') {
            return new Token(TokenClass.LSBR, line, column);
        }

        if (c == ']') {
            return new Token(TokenClass.RSBR, line, column);
        }

        if (c == ';') {
            return new Token(TokenClass.SC, line, column);
        }

        if (c == ',') {
            return new Token(TokenClass.COMMA, line, column);
        }

        /****** recognises the arithmetic operator ******/
        if (c == '+') {
            return new Token(TokenClass.PLUS, line, column);
        }

        if (c == '-') {
            return new Token(TokenClass.MINUS, line, column);
        }

        if (c == '*') {
            return new Token(TokenClass.ASTERIX, line, column);
        }

        if (c == '/' && scanner.peek() != '/' && scanner.peek() != '*') {
            return new Token(TokenClass.DIV, line, column);
        }

        if (c == '%') {
            return new Token(TokenClass.REM, line, column);
        }

        /****** recognises the logic operators ******/
        if (c == '&' && scanner.peek() == '&') {
            scanner.next();
            return new Token(TokenClass.AND, line, column);
        }

        if (c == '|' && scanner.peek() == '|') {
            scanner.next();
            return new Token(TokenClass.OR, line, column);
        }

        /****** recognises the assignment operator and comparison operators ******/
        if (c == '=') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.EQ, line, column);
            } else {
                return new Token(TokenClass.EQ, line, column);
            }
        }

        if (c == '!' && scanner.peek() == '=') {
            return new Token(TokenClass.NE, line, column);
        }

        if (c == '<') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.LE, line, column);
            } else {
                return new Token(TokenClass.LT, line, column);
            }
        }

        if (c == '>') {
            if (scanner.peek() == '=') {
                scanner.next();
                return new Token(TokenClass.GE, line, column);
            } else {
                return new Token(TokenClass.GT, line, column);
            }
        }

        /****** recognises the struct member access symbol ******/
        if (c == '.') {
            return new Token(TokenClass.DOT, line, column);
        }

        /****** recognises string ******/
        if (c == '\"') {
            // TODO to be finished
            String stringData;
            try {
                stringData = readString();
                return new Token(TokenClass.STRING_LITERAL, stringData, line, column);
            } catch (EoLException eol) {
                error(c, line, column);
                return new Token(TokenClass.INVALID, line, column);
            }
        }

        // if we reach this point, it means we did not recognise a valid token
        error(c, line, column);
        return new Token(TokenClass.INVALID, line, column);
    }


    ///////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Utility functions //////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////

    private void skipLine() throws IOException {
        char nextOne = scanner.next();
        while (!(nextOne == '\n' || nextOne == '\r')) {
            nextOne = scanner.next();
        }
    }

    /**
    *   @desc this function will read and accumulate the characters into String 
    *         until \" is met in one single line. 
    */
    private String readString() throws EoLException, IOException {
        try {
            char thisOne = scanner.next();
            String resultString = "";

            while (thisOne != '\"') {
                if (thisOne == '\n' || thisOne == '\r') {
                    throw new EoLException();
                }

                resultString = resultString + Character.toString(thisOne);

                if (thisOne == '\\') {
                    // encounter escape char
                    resultString = resultString + Character.toString(scanner.next());
                }

                thisOne = scanner.next();
            }

            return resultString;
        } catch (EOFException eof) {
            throw new EoLException();
        }
    }

}



























