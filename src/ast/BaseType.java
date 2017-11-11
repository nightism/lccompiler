package ast;

public enum BaseType implements Type {
    INT, CHAR, VOID;

    public <T> T accept(ASTVisitor<T> v) {
        return v.visitBaseType(this);
    }

    public int size() {
        int s;
        if (this == INT) {
            return 4;
        } else if (this == CHAR) {
            return 1;
        } else {
            return 1;
        }
    }
}
