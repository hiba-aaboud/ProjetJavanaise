package jvn;

import java.io.Serializable;

public class JvnObjImpl implements JvnObject {

    private static final long serialVersionUID = 1L;

    public enum LockState { NL, RC, WC, R, W, RWC };

    // mutable shared fields guarded by synchronized(this)
    private LockState lockState;
    private Serializable sharedObject;
    private transient JvnLocalServer server;
    private int objectId = 0;

    public void JvnObjectImpl(int objectId, Serializable sharedObject, JvnLocalServer server) {
        this.objectId = objectId;
        this.sharedObject = sharedObject;
        this.server = server;
        this.lockState = LockState.NL;
    }

//    public void JvnObjectImpl(int objectId, Serializable sharedObject) {
//        this(objectId, sharedObject, null);
//    }

    public void setServer(JvnLocalServer server) {
        this.server = server;
    }

    void setLocalLockState(LockState s) {
        synchronized (this) { this.lockState = s; }
    }

    public synchronized void overwriteSharedObject(Serializable s) {
        this.sharedObject = s;
    }

    @Override
    public void jvnLockRead() throws JvnException {
        if (server == null) throw new JvnException("Local server missing in JvnObjectImpl");
        boolean needRemote = false;

        synchronized (this) {
            switch (lockState) {
                case NL:
                    needRemote = true;
                    break;
                case RC:
                    lockState = LockState.R;
                    return;
                case WC:
                    lockState = LockState.RWC;
                    return;
                case R:
                case W:
                case RWC:
                    return; // already OK for read
            }
        }

        if (needRemote) {
            // call remote outside synchronized to avoid deadlocks
            Serializable s;
            try {
                s = server.jvnLockRead(objectId);
            } catch (Exception e) {
                throw new JvnException("jvnLockRead remote error: " + e.getMessage());
            }
            synchronized (this) {
                if (s != null) this.sharedObject = s;
                this.lockState = LockState.R;
            }
        }
    }

    @Override
    public void jvnLockWrite() throws JvnException {
        if (server == null) throw new JvnException("Local server missing in JvnObjectImpl");
        boolean needRemote = false;
        synchronized (this) {
            switch (lockState) {
                case NL:
                case RC:
                case R:
                    needRemote = true;
                    break;
                case WC:
                case RWC:
                    lockState = LockState.W;
                    return;
                case W:
                    return;
            }
        }

        if (needRemote) {
            Serializable s;
            try {
                s = server.jvnLockWrite(objectId);
            } catch (Exception e) {
                throw new JvnException("jvnLockWrite remote error: " + e.getMessage());
            }
            synchronized (this) {
                if (s != null) this.sharedObject = s;
                this.lockState = LockState.W;
            }
        }
    }

    @Override
    public synchronized void jvnUnLock() throws JvnException {
        switch (lockState) {
            case R:
                lockState = LockState.RC;
                break;
            case W:
            case RWC:
                lockState = LockState.WC;
                break;
            default:
                break;
        }
        notifyAll();
    }

    @Override
    public synchronized int jvnGetObjectId() throws JvnException {
        return objectId;
    }

    @Override
    public synchronized Serializable jvnGetSharedObject() throws JvnException {
        return sharedObject;
    }

    @Override
    public void jvnInvalidateReader() throws JvnException {
        synchronized (this) {
            if (lockState == LockState.RC) {
                sharedObject = null;
                lockState = LockState.NL;
                return;
            }
            if (lockState == LockState.R || lockState == LockState.RWC) {
                // wait until active read finishes
                while (lockState == LockState.R || lockState == LockState.RWC) {
                    try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new JvnException("Interrupted", e); }
                }
                sharedObject = null;
                lockState = LockState.NL;
                return;
            }
            // for NL, WC, W etc - nothing to do (no reader to invalidate)
            sharedObject = null;
            lockState = LockState.NL;
        }
    }

    @Override
    public Serializable jvnInvalidateWriter() throws JvnException {
        // should return the current state and set NL
        Serializable s;
        synchronized (this) {
            s = this.sharedObject;
            // if writing active, wait until finished
            while (lockState == LockState.W) {
                try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new JvnException("Interrupted", e); }
            }
            // whatever the cache state is, we drop local copy and NL
            this.sharedObject = null;
            this.lockState = LockState.NL;
        }
        return s;
    }

    @Override
    public Serializable jvnInvalidateWriterForReader() throws JvnException {
        Serializable s;
        synchronized (this) {
            s = this.sharedObject;
            // if a write is active, wait
            while (lockState == LockState.W) {
                try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new JvnException("Interrupted", e); }
            }
            // reduce to read cached
            this.lockState = LockState.RC;
        }
        return s;
    }
}
