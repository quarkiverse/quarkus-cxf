
package io.quarkiverse.cxf.client.it.rtinit;

import java.util.Objects;

public class Operands {
    private int a;
    private int b;

    public Operands() {
    }

    public Operands(int a, int b) {
        super();
        this.a = a;
        this.b = b;
    }

    public int getA() {
        return a;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Operands other = (Operands) obj;
        return a == other.a && b == other.b;
    }

    public void setA(int a) {
        this.a = a;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }
}