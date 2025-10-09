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
    
    void setLocalLockState(LockState s) {
        this.lockState = s;
    }

    
    
    @Override
    public synchronized void jvnLockRead() throws jvn.JvnException {
        if (server == null) throw new JvnException("Serveur local manquant dans JvnObjectImpl");

        switch (lockState) {
            case NL: {
                // Pas de copie locale : demander au coord via le serveur local
                Serializable s = server.jvnLockRead(objectId);
                if (s != null) this.sharedObject = s;
                lockState = LockState.R;
                break;
            }
            case RC:
                lockState = LockState.R;
                break;
            case R:
                break;
            case WC:
                lockState = LockState.RWC;
                break;
            case W:
                break; // write active on ne peux pas lire
            case RWC:
                break;
            default:
                Serializable s2 = server.jvnLockRead(objectId);
                if (s2 != null) this.sharedObject = s2;
                lockState = LockState.R;
                break;
        }
    }


    @Override
    public synchronized void jvnLockWrite() throws jvn.JvnException {
        if (server == null) throw new JvnException("Serveur local manquant dans JvnObjectImpl");

        switch (lockState) {
            case NL: {
                Serializable s = server.jvnLockWrite(objectId);
                if (s != null) this.sharedObject = s;
                lockState = LockState.W;
                break;
            }
            case RC: {
             
                Serializable s2 = server.jvnLockWrite(objectId);
                if (s2 != null) this.sharedObject = s2;
                lockState = LockState.W;
                break;
            }
            case R: {
                // lecture active -> demander upgrade vers write
                Serializable s3 = server.jvnLockWrite(objectId);
                if (s3 != null) this.sharedObject = s3;
                lockState = LockState.W;
                break;
            }
            case WC:
                
                lockState = LockState.W;
                break;
            case W:
                
                break;
            case RWC:
                
                lockState = LockState.W;
                break;
            default: {
                Serializable s4 = server.jvnLockWrite(objectId);
                if (s4 != null) this.sharedObject = s4;
                lockState = LockState.W;
                break;
            }
        }
    }

    @Override
    public synchronized void jvnUnLock() throws jvn.JvnException {
        switch (lockState) {
            case R:
                lockState = LockState.RC;
                break;
            case W:
                lockState = LockState.WC;
                break;
            case RWC:
                lockState = LockState.WC;
                break;
            case RC:
            case WC:
            case NL:
            default:
                
                break;
        }
        // Réveiller les threads qui attendent la fin d'un accès actif (invalidate)
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
	public void jvnInvalidateReader() throws JvnException {
		// TODO Auto-generated method stub
		
		while(lockState == LockState.R || lockState == LockState.RWC) {
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
	this.sharedObject = null;
	}

	@Override
	public synchronized Serializable jvnInvalidateWriter() throws JvnException {
		// TODO Auto-generated method stub
		
		 while (lockState == LockState.W) {
	            try {
	                wait();
	            } catch (InterruptedException e) {
					
					e.printStackTrace();
				}
	        }
	        // On renvoie l'état courant 
	        Serializable s = this.sharedObject;
	        this.sharedObject = null;
	        this.lockState = LockState.NL;
	        return s;
		 
	}

	@Override
	public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
		// TODO Auto-generated method stub
		while (lockState == LockState.W) {
            try {
                wait();
            } catch (InterruptedException e) {
				
				e.printStackTrace();
			}
        }
        // Retourner l'état et réduire on passe a read
        Serializable s = this.sharedObject;
        this.lockState = LockState.R;
        return s;
    }

}

