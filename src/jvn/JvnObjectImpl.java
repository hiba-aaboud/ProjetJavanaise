package jvn;

import java.io.Serializable;

import irc.Sentence;

public class JvnObjectImpl implements JvnObject{

    private static final long serialVersionUID = 1L;
    public enum LockState {NL, RC, WC, R, W, RWC};
    private LockState lockState;
    private Serializable sharedObject;
    private transient JvnLocalServer server;
    private final int objectId;


    public JvnObjectImpl(int objectId, Serializable sharedObject, JvnLocalServer server) {
        this.objectId = objectId;
        this.sharedObject = sharedObject;
        this.server = server;
        this.lockState = LockState.NL;
    }

    public JvnObjectImpl(int objectId, Serializable sharedObject) {
        this(objectId, sharedObject, null);
    }

    public void setServer(JvnLocalServer server) {
        this.server = server;
    }
    void setLocalLockState(LockState s) {
        this.lockState = s;
    }
    public synchronized void overwriteSharedObject(Serializable s) {
        this.sharedObject = s;
    }

    @Override
    public synchronized void jvnLockRead() throws JvnException {
        // TODO Auto-generated method stub
        Serializable s = null;
        System.out.println("etat lock inital :" + this.lockState);
        System.out.println("id de notre objet read" + this.jvnGetObjectId());
        switch (this.lockState) {
            case NL:
                s = server.jvnLockRead(this.jvnGetObjectId());
                if (s != null) sharedObject = s;
                this.lockState = LockState.R;
                break;
            case RC:
                this.lockState = LockState.R;
                break;
            case WC:
                this.lockState = LockState.RWC;
                break;
            case R:
                //rien a faire
                break;
            case W:
                //deja en ecriture
                break;
            case RWC:
                //same
                break;
        }
        System.out.println("etat lock final read :" + this.lockState);
    }

    @Override
    public synchronized void jvnLockWrite() throws JvnException {
        // TODO Auto-generated method stub
        System.out.println("etat lock write :" + this.lockState);
        Serializable s = null;
        System.out.println("id de notre objet" + this.jvnGetObjectId());
        switch (this.lockState) {
            case NL, RC:
                s = server.jvnLockWrite(this.jvnGetObjectId());
                if (s != null) sharedObject = s;
                this.lockState = LockState.W;
                break;
            case WC, RWC:
                this.lockState = LockState.W;
                break;
            case W:
                break;
            case R:
                s = server.jvnLockWrite(this.jvnGetObjectId());
                if (s != null) sharedObject = s;
                this.lockState = LockState.W;
                break;
        }
        System.out.println("etat lock write final :" + this.lockState);
    }

    @Override
    public synchronized void jvnUnLock() throws JvnException {
        // TODO Auto-generated method stub
        switch (this.lockState) {
            case R:
                this.lockState = LockState.RC;
                break;
            case W, RWC:
                this.lockState = LockState.WC;
                break;
            default:
                break;
        }
        notifyAll();
    }

    @Override
    public synchronized int jvnGetObjectId() throws JvnException {
        // TODO Auto-generated method stub
        return objectId;
    }

    @Override
    public synchronized Serializable jvnGetSharedObject() throws JvnException {
        // TODO Auto-generated method stub
        return sharedObject;
    }

    @Override
    public synchronized void jvnInvalidateReader() throws JvnException {

        if (lockState == LockState.RC) {
            lockState = LockState.NL;
            return;
        }

        if (lockState == LockState.R || lockState == LockState.RWC) {
            try {

                while (lockState == LockState.R || lockState == LockState.RWC) {
                    wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JvnException("interrupted", e);
            }
            lockState = LockState.NL;
            return;
        }

    }



    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {
        // TODO Auto-generated method stub
        Serializable s = this.sharedObject;
        //this.sharedObject = null;
        switch (this.lockState) {

            case RWC,W:
                while (this.lockState == LockState.W) {
                    try { this.wait(); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new JvnException("Interrupted", e);
                    }
                }
                this.lockState = LockState.NL;
                break;
            case WC:
                this.lockState = LockState.NL;
                break;

            default:
                throw new JvnException("error");
        }
        return s;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
        // TODO Auto-generated method stub
        System.out.println("etat :" + this.lockState);
        Serializable s = this.sharedObject;
        switch (this.lockState) {
            case RWC:
                this.lockState = LockState.RC;
                break;
            case W:
                while (this.lockState == LockState.W) {
                    try { this.wait(); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new JvnException("Interrupted", e);
                    }
                }
                System.out.println("etat W" + this.jvnGetObjectId());
                this.lockState = LockState.RC;
                break;
            case WC:
                System.out.println("etat WC" + this.jvnGetObjectId());
                this.lockState = LockState.RC;
                break;

            default:
                throw new JvnException("error");
        }
        return s;

    }}