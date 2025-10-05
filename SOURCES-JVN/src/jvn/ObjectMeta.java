package jvn;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectMeta {

    private final int id;
    private volatile Serializable state;
    private volatile JvnRemoteServer writer;
    private final Set<JvnRemoteServer> readers = Collections.newSetFromMap(new ConcurrentHashMap<JvnRemoteServer, Boolean>());

    public ObjectMeta(int id, Serializable state) {
        this.id = id;
        this.state = state;
        this.writer = null;
    }

    public int getId() { return id; }
    public Serializable getState() { return state; }
    public void setState(Serializable state) { this.state = state; }
    public JvnRemoteServer getWriter() { return writer; }
    public void setWriter(JvnRemoteServer writer) { this.writer = writer; }
    public Set<JvnRemoteServer> getReaders() { return readers; }

}
