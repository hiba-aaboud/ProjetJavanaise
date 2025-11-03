package test;

import jvn.JvnObject;
import jvn.JvnServerImpl;
import jvn.MySharedObject;

public class TestWriteThenReadCache {

    public static void main(String[] args) throws Exception {
        
    	JvnServerImpl js = JvnServerImpl.jvnGetServer();

        JvnObject jvnO = js.jvnLookupObject("MyObject");
        
        if (jvnO == null) {
            System.out.println("[JVM3] objet pas trouvé");
            return;
        }

        jvnO.jvnLockWrite();
        
        MySharedObject o = (MySharedObject) jvnO.jvnGetSharedObject();
        System.out.println("got lock read " + o.getValue());
        if (o == null) {
            
            o = new MySharedObject("Modifie par JVM3");
        } else {
            o.setValue("Modifie par JVM3");
        }
        System.out.println("[JVM3] jvnLockWrite obtenu, new value = " + o.getValue());

//        Thread.sleep(120_000);
        jvnO.jvnUnLock();
        System.out.println("[JVM3] write terminé, jvnUnLock()");
        System.out.println("[JVM3] etat lock ");

    }
}
