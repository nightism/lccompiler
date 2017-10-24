package sem;

public abstract class Symbol {
    public String name;

    public abstract boolean isVar();
    public abstract boolean isFun();
}
