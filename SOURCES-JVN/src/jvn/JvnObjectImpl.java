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
            //if on an inital state on le considere en lecture cache
            this.lockState = (sharedObject != null) ? LockState.RC : LockState.NL;
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
	@Override
	public synchronized void jvnLockRead() throws JvnException {
		// TODO Auto-generated method stub
		Serializable s = null;

//		if (server == null) {
//			throw new JvnException("Serveur local manquant dans JvnObjectImpl");
//		}
        switch (this.lockState) {
            case NL:
                s = server.jvnLockRead(objectId);
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
        }

	@Override
	public synchronized void jvnLockWrite() throws JvnException {
		// TODO Auto-generated method stub
        Serializable s = null;
        switch (this.lockState) {
            case NL, RC:
                s = server.jvnLockWrite(objectId);
                if (s != null) sharedObject = s;
                this.lockState = LockState.W;
                break;
            case WC, RWC:
                this.lockState = LockState.W;
                break;
            case W:
                break;
            case R:
                break;
        }
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
        if(this.lockState == LockState.RC){
            this.lockState = LockState.NL;
        } else if(this.lockState == LockState.R || this.lockState == LockState.RWC ){
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.lockState = LockState.NL;
        }
    }


	@Override
	public synchronized Serializable jvnInvalidateWriter() throws JvnException {
		// TODO Auto-generated method stub
		 Serializable s = this.sharedObject;
	     this.sharedObject = null;
        switch (this.lockState) {
            case RWC:
                this.lockState = LockState.NL;
                break;
            case W:
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.lockState = LockState.NL;
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
        Serializable s = this.sharedObject;
        switch (this.lockState) {
            case RWC:
                this.lockState = LockState.RC;
                break;
            case W:
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.lockState = LockState.RC;
            case WC:
                this.lockState = LockState.RC;
                break;

            default:
                throw new JvnException("error");
        }
        return s;

    }}