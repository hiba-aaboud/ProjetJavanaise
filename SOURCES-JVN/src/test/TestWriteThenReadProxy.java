package test;

import jvn.*;

public class TestWriteThenReadProxy {
    public static void main(String[] args) throws Exception {
        System.out.println("[JVM3] start");
        JvnServerImpl js = JvnServerImpl.jvnGetServer();
        if (js == null) { System.err.println("js null"); return; }

        MySharedInterface proxy = js.jvnLookupProxy("MyObject", MySharedInterface.class);
        if (proxy == null) {
            System.out.println("[JVM3] objet pas trouvé");
            return;
        }

        proxy.setValue("updated-by-JVM3");
        System.out.println("[JVM3] wrote new value via proxy.");

  
        Thread.sleep(60_000);
    }
}
