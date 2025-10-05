/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */ 

package jvn;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.Serializable;


public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{
	

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    private final AtomicInteger nextObjectId = new AtomicInteger(1);
	private final ConcurrentMap<String,Integer> nameToId = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, ObjectMeta> idToMeta = new ConcurrentHashMap<>();

/**
  * Default constructor
  * @throws JvnException
  **/
	private JvnCoordImpl() throws Exception {
		// to be completed
		super();
		try {
            LocateRegistry.createRegistry(1099);
            System.out.println("[JvnCoord] Registre RMI créé sur le port 1099");
        } catch (Exception e) {
            System.out.println("[JvnCoord] Le registre RMI existe déjà ou n'a pas pu être créé: " + e.getMessage());
        }
        Naming.rebind("rmi://localhost:1099/JvnCoord", this);
        System.out.println("[JvnCoord] Lié 'JvnCoord'");
 
	}
	
	 public static void main(String[] args) {
	        try {
	            new JvnCoordImpl();
	            System.out.println("[JvnCoord] Prêt.");
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

  /**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  public int jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.JvnException {
    // to be completed 
	  return nextObjectId.getAndIncrement();
  }
  
  /**
  * Associate a symbolic name with a JVN object
  * @param jon : the JVN object name
  * @param jo  : the JVN object 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  //creer un lien symbilique en associant idObject a un nom, puis a ses meta donnees
  public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
    // to be completed 
	  
	  try {
          int id = jo.jvnGetObjectId();
          Serializable state = jo.jvnGetSharedObject();
          ObjectMeta meta = new ObjectMeta(id, state);
          idToMeta.put(id, meta);
          nameToId.put(jon, id);
          if (js != null) {
        	  meta.getReaders().add(js);
          }
          System.out.println("[JvnCoord] Enregistré name='" + jon + "' id=" + id);
      } catch (Exception e) {
          throw new JvnException("jvnRegisterObject: " + e.getMessage());
      }
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  
  //Récupération d'une référence sur un objet JVN à partir de son nom symbolique
  public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
    // to be completed 
	  
	  Integer id = nameToId.get(jon);
      if (id == null) return null;
      ObjectMeta meta = idToMeta.get(id);
      if (meta == null) {
    	  return null;
      }
      
      if (js != null) {
    	  meta.getReaders().add(js);
      }
      
      return new JvnObjectImpl(id, meta.getState());
 }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    // to be completed
	   ObjectMeta meta = idToMeta.get(joi);
       if (meta == null) throw new JvnException("id Objet  " + joi + " Pas trouvé");
       synchronized (meta) {
           if (meta.getWriter() != null && !meta.getWriter().equals(js)) {

               try {
                   Serializable s = meta.getWriter().jvnInvalidateWriterForReader(joi);
                   meta.setState(s);
                   meta.getReaders().add(meta.getWriter());
                   meta.setWriter(null);
               } catch (Exception e) {
                   
                   meta.setWriter(null);
               }
           }
           if (js != null) {
        	   meta.getReaders().add(js);
           }
           return meta.getState();
       }
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    // to be completed
	   ObjectMeta meta = idToMeta.get(joi);
       if (meta == null) throw new JvnException("id Objet " + joi + " pas trouvé");
       synchronized (meta) {
           
           for (JvnRemoteServer r : meta.getReaders()) {
               if (!r.equals(js)) {
                   try {
                       r.jvnInvalidateReader(joi);
                   } catch (Exception e) {
                       
                   }
               }
           }
           meta.getReaders().clear();
           
           if (meta.getWriter() != null && !meta.getWriter().equals(js)) {
               try {
                   Serializable s = meta.getWriter().jvnInvalidateWriter(joi);
                   meta.setState(s);
               } catch (Exception e) {
                   
               }
           }
           meta.setWriter(js);
           return meta.getState();
       }
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
	 // to be completed
    	
    	for (ObjectMeta meta : idToMeta.values()) {
            synchronized (meta) {
                meta.getReaders().remove(js);
                if (js.equals(meta.getWriter())) meta.setWriter(null);
            }
        }
        System.out.println("[JvnCoord] Nettoyage du serveur terminé.");
    }
  
}

 
