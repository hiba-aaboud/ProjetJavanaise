package jvn;

import java.io.Serializable;

import irc.Sentence;

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
		Serializable s = null;

		if (server == null) {
			throw new JvnException("Serveur local manquant dans JvnObjectImpl");
		}
		if (lockState == LockState.RC){
			this.lockState = LockState.R;
	
		}
		if (lockState == LockState.NL) {
			s = server.jvnLockRead(objectId);	
		}

		if(lockState == LockState.WC) {
			this.lockState = LockState.RWC;
		}
        
        if (s != null) {
        	this.sharedObject = s;
        }

       
		
	}
	@Override
	public void jvnLockWrite() throws JvnException {
		// TODO Auto-generated method stub
		Serializable s = null;
		if (server == null) {
			throw new JvnException("Serveur local manquant dans JvnObjectImpl");
		}
		if (lockState == LockState.NL || lockState==LockState.RC){
			s = server.jvnLockWrite(objectId);
		}
		if(lockState==LockState.WC || lockState==LockState.RWC){
			this.lockState= LockState.W;
		}
        if (s != null) {
        	this.sharedObject = s;
        }
		
	}

	@Override
	public void jvnUnLock() throws JvnException {
		// TODO Auto-generated method stub				
		if(lockState == LockState.R){
	    	lockState = LockState.RC;
	    }else if(lockState == LockState.W || lockState == LockState.RWC)
	    {
	    	lockState = LockState.WC; 
	    }

		notifyAll();
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
		
		while(lockState == LockState.R ) {
			try {
				wait();
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
		}
		lockState = LockState.NL;

	if(lockState == LockState.RC){
		lockState = LockState.NL;
	}
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