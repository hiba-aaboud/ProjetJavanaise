package test;

import jvn.JvnObject;
import jvn.JvnServerImpl;
import jvn.MySharedObject;

public class TestLookupRead {
		
	public static void main(String[] args) throws Exception {
        JvnServerImpl js = JvnServerImpl.jvnGetServer();
        JvnObject jvnO = js.jvnLookupObject("MyObject");
        if (jvnO == null) {
            System.out.println("[JVM2] objet pas trouvé");
            return;
        }
        jvnO.jvnLockRead();
        MySharedObject o = (MySharedObject) jvnO.jvnGetSharedObject();
        System.out.println("[JVM2] lecture objet: " + o.getValue());
        jvnO.jvnUnLock();
        Thread.sleep(60_000);
    }
}
