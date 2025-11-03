package jvn;

import java.io.Serializable;

public class Counter implements CounterItf, Serializable {
    private static final long serialVersionUID = 1L;
    private int value = 0;

    @Override public void inc() { value++; }
    @Override public int  get() { return value; }
    public  void set(int v) { value = v; }

    @Override public String toString() { return "Counter(" + value + ")"; }
}
