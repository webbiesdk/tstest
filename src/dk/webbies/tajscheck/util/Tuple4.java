package dk.webbies.tajscheck.util;

/**
 * Created by erik1 on 09-01-2017.
 */
public class Tuple4<A, B, C, D> {
    private final A a;
    private final B b;
    private final C c;
    private final D d;

    public Tuple4(A a, B b, C c, D d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public C getC() {
        return c;
    }

    public D getD() {
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple4<?, ?, ?, ?> tuple4 = (Tuple4<?, ?, ?, ?>) o;

        if (a != null ? !a.equals(tuple4.a) : tuple4.a != null) return false;
        if (b != null ? !b.equals(tuple4.b) : tuple4.b != null) return false;
        if (c != null ? !c.equals(tuple4.c) : tuple4.c != null) return false;
        return d != null ? d.equals(tuple4.d) : tuple4.d == null;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + (c != null ? c.hashCode() : 0);
        result = 31 * result + (d != null ? d.hashCode() : 0);
        return result;
    }
}
