// IGNORE_K2
class X {
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals<caret>(Object o) {
        return super.equals(o);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

class Y extends Thread {
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
