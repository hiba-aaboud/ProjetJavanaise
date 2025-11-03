package jvn;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ObjectMeta {
    private final int id;
    private Serializable state;
    private JvnRemoteServer writer;          // the ONLY process allowed to have WC/W
    private final Set<JvnRemoteServer> readers = new HashSet<>();

    public ObjectMeta(int id, Serializable init) {
        this.id = id;
        this.state = init;
    }

    public int getId() { return id; }
    public Serializable getState() { return state; }
    public void setState(Serializable s) { this.state = s; }

    public JvnRemoteServer getWriter() { return writer; }
    public void setWriter(JvnRemoteServer w) { this.writer = w; }

    public Set<JvnRemoteServer> getReaders() { return readers; }
}
