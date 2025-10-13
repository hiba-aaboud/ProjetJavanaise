/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.io.*;



public class JvnServerImpl 	
              extends UnicastRemoteObject 
							implements JvnLocalServer, JvnRemoteServer{ 
	
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton  
	private static JvnServerImpl js = null;
    private final JvnRemoteCoord coord;
    private final ConcurrentMap<Integer, JvnObjectImpl> localObjects = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> localNames = new ConcurrentHashMap<>();


  /**
  * Default constructor
  * @throws JvnException
  **/
	private JvnServerImpl() throws Exception {
		super();
        coord = (JvnRemoteCoord) Naming.lookup("rmi://localhost:1099/JvnCoord");
        System.out.println("[JvnServer] Connected to JvnCoord");
	}
	
  /**
    * Static method allowing an application to get a reference to 
    * a JVN server instance
    * @throws JvnException
    **/
	public static JvnServerImpl jvnGetServer() {
		if (js == null){
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return js;
	}
	
	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	public  void jvnTerminate()
	throws jvn.JvnException {
		 try {
	            if (coord != null) {
	            	coord.jvnTerminate(this);
	            }
	            System.out.println("[JvnServer] Terminated");
	        } catch (Exception e) {
	            throw new JvnException("jvnTerminate: " + e.getMessage());
	        }
	} 
	
	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public  JvnObject jvnCreateObject(Serializable o)
	throws jvn.JvnException { 
		// to be completed 
		try {
            int id = coord.jvnGetObjectId();
            JvnObjectImpl jo = new JvnObjectImpl(id, o, this);
            //jo.setLocalLockState(JvnObjectImpl.LockState.WC);
            localObjects.put(id, jo);
            return jo;
        } catch (Exception e) {
            throw new JvnException("jvnCreateObject: " + e.getMessage());
        }
	}
	
	
	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
	public  void jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.JvnException {
		try {
            coord.jvnRegisterObject(jon, jo, this);
            int id = jo.jvnGetObjectId();
            System.out.print("id object = " + id);
            localNames.put(jon, id);
            System.out.print("name object = " + jon);
        } catch (Exception e) {
            throw new JvnException("jvnRegisterObject: " + e.getMessage());
        }
	}
	
	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	public  JvnObject jvnLookupObject(String jon)
	throws jvn.JvnException {
    // to be completed 
		try {
            JvnObject remoteJo = coord.jvnLookupObject(jon, this);
            if (remoteJo == null) return null;
            int id = remoteJo.jvnGetObjectId();
            Serializable state = remoteJo.jvnGetSharedObject();
            JvnObjectImpl localJo = new JvnObjectImpl(id, state, this);
            //localJo.setLocalLockState(JvnObjectImpl.LockState.RC);
            localObjects.put(id, localJo);
            return localJo;
        } catch (Exception e) {
            throw new JvnException("jvnLookupObject: " + e.getMessage());
        }
	}	
	
	/**
	* Get a Read lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockRead(int joi)
	 throws JvnException {
		// to be completed
	   try {
           System.out.println("valeur du local hello :");

           Serializable state = coord.jvnLockRead(joi, this);
           System.out.println("valeur du local state :"+ state);
           JvnObjectImpl local = localObjects.get(joi);
           System.out.println("valeur du local reader :"+ local);
           if (state != null) {
        	   local.overwriteSharedObject(state);
           }
           return state;
       } catch (Exception e) {
           throw new JvnException("jvnLockRead: " + e.getMessage());
       }

	}

	/**
	* Get a Write lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockWrite(int joi)
	 throws JvnException {
		// to be completed 
	   try {
           Serializable state = coord.jvnLockWrite(joi, this);
           JvnObjectImpl local = localObjects.get(joi);
           System.out.println("valeur du local :"+ local);
           if (state != null) local.overwriteSharedObject(state);
           return state;
       } catch (Exception e) {
           throw new JvnException("jvnLockWrite: " + e.getMessage());
       }
	}	

	
  /**
	* Invalidate the Read lock of the JVN object identified by id 
	* called by the JvnCoord
	* @param joi : the JVN object id
	* @return void
	* @throws java.rmi.RemoteException,JvnException
	**/
  public void jvnInvalidateReader(int joi)
	throws java.rmi.RemoteException,jvn.JvnException {
	  JvnObjectImpl local = localObjects.get(joi);
      if (local != null) {
          local.jvnInvalidateReader();
      }
	};
	    
	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
  public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.JvnException { 
		// to be completed 
	  JvnObjectImpl local = localObjects.get(joi);
      if (local != null) {
          return local.jvnInvalidateWriter();
      } else {
          return null;
      }
	};
	
	/**
	* Reduce the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
   public Serializable jvnInvalidateWriterForReader(int joi)
	 throws java.rmi.RemoteException,jvn.JvnException { 
		// to be completed 
	   JvnObjectImpl local = localObjects.get(joi);
       System.out.println("valeur du local :"+ local);
       if (local != null) {
           return local.jvnInvalidateWriterForReader();
       } else {
           return null;
       }
	 };

}

 
