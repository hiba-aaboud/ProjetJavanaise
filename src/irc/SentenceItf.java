package irc;

import jvn.JvnAnnotation;

public interface SentenceItf {

    @JvnAnnotation(operation ="write")
    public void write(String text);

    @JvnAnnotation(operation ="read")
    public String read();

    @JvnAnnotation(operation ="debug")
    public String debug();
}

