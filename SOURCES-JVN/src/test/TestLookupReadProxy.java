package test;

import jvn.*;

public class TestLookupReadProxy {
    public static void main(String[] args) throws Exception {
        System.out.println("[JVM2] start");
        JvnServerImpl js = JvnServerImpl.jvnGetServer();
        if (js == null) { System.err.println("js null"); return; }

        // lookup proxy for "MyObject"
        MySharedInterface proxy = js.jvnLookupProxy("MyObject", MySharedInterface.class);
        if (proxy == null) {
            System.out.println("[JVM2] objet pas trouvé");
            return;
        }

        String v = proxy.getValue();
        System.out.println("[JVM2] read value via proxy: " + v);

        Thread.sleep(120_000);
    }
}
