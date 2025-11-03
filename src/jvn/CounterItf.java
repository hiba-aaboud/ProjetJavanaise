package jvn;

import annotations.JvnRead;
import annotations.JvnWrite;

import java.io.Serializable;

public interface CounterItf extends Serializable {
    @JvnWrite void inc();
    @JvnRead  int  get();
    default void set(int x) {}
}
