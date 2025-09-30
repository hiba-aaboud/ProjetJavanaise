package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject{

	public enum LockState {NL, RC, WC, R, W, RWC};
	
	private LockState lock = LockState.NL;
	
	private Serializable object;
	
	@Override
	public void jvnLockRead() throws JvnException {
		// TODO Auto-generated method stub
		
		if(lock == LockState.NL || lock == LockState.RC) {
			lock = LockState.R;
		}
		else if(lock==LockState.WC || lock==LockState.RWC) {
			lock = LockState.RWC;
		}
		
	}

	@Override
	public void jvnLockWrite() throws JvnException {
		// TODO Auto-generated method stub
		
		if(lock == LockState.NL || lock == LockState.RC || lock == LockState.WC ) {
			lock = LockState.W;
		}
		
	}

	@Override
	public void jvnUnLock() throws JvnException {
		// TODO Auto-generated method stub
		
		if(lock == LockState.R) {
			lock = LockState.RC;
		}
		else if(lock == LockState.W) {
			lock = LockState.WC;
		}
		
	}

	@Override
	public int jvnGetObjectId() throws JvnException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Serializable jvnGetSharedObject() throws JvnException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void jvnInvalidateReader() throws JvnException {
		// TODO Auto-generated method stub
		
		while(lock == LockState.R || lock == LockState.RWC) {
			try {
				wait();
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
		}
		
		if(lock == LockState.RC) {
			lock = LockState.NL;
		}
		
	}

	@Override
	public Serializable jvnInvalidateWriter() throws JvnException {
		// TODO Auto-generated method stub
		
		while(lock == LockState.W) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Serializable ser = null;
		
		if(lock == LockState.WC) {
			ser = object;
			lock = LockState.NL;
		}
		return ser;
	}

	@Override
	public Serializable jvnInvalidateWriterForReader() throws JvnException {
		// TODO Auto-generated method stub
		
		while(lock == LockState.W) {
		   try {
			wait();
		   } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		   }
		}
		Serializable ser = null;
		
		if(lock == LockState.WC ||  lock == LockState.RWC) {
			ser = object;
			lock = LockState.RC;
		}
		
		return ser;
	}

}