package lexer;

import lexer.Token.TokenClass;

import java.io.EOFException;
import java.io.IOException;

/**
 * @author cdubach
 * @authot myiking
 */
public class Tokeniser {

    private class EOLException extends Exception {
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
                scanner.next();
                try{
                    char nextOne = scanner.next();
                    while (!(nextOne == '*' && scanner.peek() == '/')) {
                        nextOne = scanner.next();
                    }
                    scanner.next();
                } catch (EOFException eof) {
                    error(c, line, column);
                    return new Token(TokenClass.INVALID, line, column);
                }
                return next();
            } else {
                return new Token(TokenClass.DIV, line, column);
            }
        }   

        /****** recognises the header files ******/
        if (c == '#') {
            int thisCol = column;
            String includeToken = Character.toString(c);
            char nextOne = scanner.next();
            while (!Character.isWhitespace(nextOne)) {
                includeToken = includeToken + Character. toString(nextOne);
                nextOne = scanner.next();
            }
            if (includeToken.equals("#include")) {
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
                return new Token(TokenClass.ASSIGN, line, column);
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
            String stringData;
            try {
                stringData = readString();
                return new Token(TokenClass.STRING_LITERAL, stringData, line, column);
            } catch (EOLException eol) {
                error(c, line, column);
                return new Token(TokenClass.INVALID, line, column);
            }
        }

        /****** recognises characters ******/
        if (c == '\'') {
            char nextOne = scanner.peek();
            if (nextOne == '\n' || nextOne == '\r' || nextOne == -1) {
            // unqoted and undefined char
                error(c, line, column);
                return new Token(TokenClass.INVALID, line, column);
            } else if (nextOne == '\\') {
            // special character
                String target = Character.toString(nextOne);
                scanner.next();
                nextOne = scanner.next();

                if (nextOne == 't' || nextOne == 'b' || nextOne == 'n' || nextOne == 'r' || nextOne == 'f'
                    || nextOne == '\'' || nextOne == '\\' || nextOne == '\"' ) {
                // existing characters
                    if (!readUntilApostrophe()) {
                    // more than one characters encountered between apostrophe
                        error(c, line, column);
                        return new Token(TokenClass.INVALID, line, column);
                    } else {
                    // correct return with special character
                        return new Token(TokenClass.CHAR_LITERAL, target + nextOne, line, column);
                    }
                } else {
                // non-existing characters
                    readUntilApostrophe();
                    error(c, line, column);
                    return new Token(TokenClass.INVALID, line, column);
                }
            } else {
            // normal character
                String target = Character.toString(nextOne);
                scanner.next();

                if (!readUntilApostrophe()) {
                // more than one characters encountered between apostrophe
                    error(c, line, column);
                    return new Token(TokenClass.INVALID, line, column);
                } else {
                // correct return with normal character
                    return new Token(TokenClass.CHAR_LITERAL, target, line, column);
                }
            }

        }

        /****** recognises keywords and identifier ******/
        if (Character.isLetter(c) || c == '_') {
            String target = Character.toString(c) + readWord();

            if (target.equals("int")) {
                return new Token(TokenClass.INT, line, column);
            } else if (target.equals("char")) {
                return new Token(TokenClass.CHAR, line, column);
            } else if (target.equals("void")) {
                return new Token(TokenClass.VOID, line, column);
            } else if (target.equals("if")) {
                return new Token(TokenClass.IF, line, column);
            } else if (target.equals("else")) {
                return new Token(TokenClass.ELSE, line, column);
            } else if (target.equals("while")) {
                return new Token(TokenClass.WHILE, line, column);
            } else if (target.equals("return")) {
                return new Token(TokenClass.RETURN, line, column);
            } else if (target.equals("struct")) {
                return new Token(TokenClass.STRUCT, line, column);
            } else if (target.equals("sizeof")) {
                return new Token(TokenClass.SIZEOF, line, column);
            } else {
                return new Token(TokenClass.IDENTIFIER, target, line, column);
            }
        }

        /****** recognises numbers ******/
        if (Character.isDigit(c)) {
            String target = readNumber();
            if (target == null) {
                error(c, line, column);
                return new Token(TokenClass.INVALID, line, column);
            } else {
                target = c + target;
                return new Token(TokenClass.INT_LITERAL, line, column);
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

    private boolean checkEOL() throws IOException {
        char thisOne = scanner.next();
        while (thisOne != '\n' && thisOne != '\r') {
            if (!Character.isWhitespace(thisOne)) {
                return false;
            }
        }
        return true;
    }

    private boolean readUntilApostrophe() throws IOException {
        boolean result = true;
        while (scanner.peek() != '\'' && scanner.peek() != '\n' && scanner.peek() != '\r') {
            result = false;
            scanner.next();
        }
        scanner.next();
        return result;
    }

    private String readString() throws EOLException, IOException {
        try {
            char thisOne = scanner.next();
            String resultString = "";

            while (thisOne != '\"') {
                if (thisOne == '\n' || thisOne == '\r') {
                    throw new EOLException();
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
            throw new EOLException();
        }
    }

    private String readWord() throws IOException {
        String result = "";
        while (Character.isLetter(scanner.peek()) || Character.isDigit(scanner.peek()) || scanner.peek() == '_') {
            result = result + Character.toString(scanner.next());
        }
        return result;
    }

    private String readNumber() throws IOException {
        String result = "";
        boolean invalid = false;
        while (Character.isLetter(scanner.peek()) || Character.isDigit(scanner.peek()) || scanner.peek() == '_') {
            char thisOne = scanner.next();
            result = result + Character.toString(thisOne);
            if (!invalid && !Character.isDigit(thisOne)) {
                invalid = true;
            }
        }
        return (invalid ? null : result);
    }
}
