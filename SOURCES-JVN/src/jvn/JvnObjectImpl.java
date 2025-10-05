package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject{

    private static final long serialVersionUID = 1L;

	public enum LockState {NL, RC, WC, R, W, RWC};
	
	private LockState lockState = LockState.NL;
	
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
	
	@Override
	public void jvnLockRead() throws JvnException {
		// TODO Auto-generated method stub
		
		if (server == null) {
			throw new JvnException("Serveur local manquant dans JvnObjectImpl");
		}
        Serializable s = server.jvnLockRead(objectId);
        if (s != null) {
        	this.sharedObject = s;
        }
        this.lockState = LockState.R;
		
	}

	@Override
	public void jvnLockWrite() throws JvnException {
		// TODO Auto-generated method stub
		if (server == null) {
			throw new JvnException("Serveur local manquant dans JvnObjectImpl");
		}
        Serializable s = server.jvnLockWrite(objectId);
        if (s != null) {
        	this.sharedObject = s;
        }
        this.lockState = LockState.W;
		
	}

	@Override
	public void jvnUnLock() throws JvnException {
		// TODO Auto-generated method stub
		this.lockState = LockState.NL;	
	}

	@Override
	public int jvnGetObjectId() throws JvnException {
		// TODO Auto-generated method stub
		return objectId;
	}

	@Override
	public Serializable jvnGetSharedObject() throws JvnException {
		// TODO Auto-generated method stub
		return sharedObject;
	}

	@Override
	public void jvnInvalidateReader() throws JvnException {
		// TODO Auto-generated method stub
		
		this.sharedObject = null;
        this.lockState = LockState.NL;
		
	}

	@Override
	public Serializable jvnInvalidateWriter() throws JvnException {
		// TODO Auto-generated method stub
		
		 Serializable s = this.sharedObject;
	     this.sharedObject = null;
	     this.lockState = LockState.NL;
	     return s;
	}

	@Override
	public Serializable jvnInvalidateWriterForReader() throws JvnException {
		// TODO Auto-generated method stub
		Serializable s = this.sharedObject;
        this.lockState = LockState.R;
        return s;
	}

}