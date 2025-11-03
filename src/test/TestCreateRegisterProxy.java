package test;

import jvn.*;
import proxy.JvnProxyFactory;

import java.lang.Thread;

public class TestCreateRegisterProxy {
    public static void main(String[] args) throws Exception {
        System.out.println("[JVM1] start");
        JvnServerImpl js = JvnServerImpl.jvnGetServer();
        if (js == null) { System.err.println("Erreur: js null"); return; }

        // On crée et on enregistre en une seule méthode
       // MySharedInterface proxy = js.jvnCreateProxy(new MySharedObject("hello-from-JVM1"), MySharedInterface.class);
        JvnObject jvnO = js.jvnCreateObject(new MySharedObject("hello-from-JVM1"));
        js.jvnRegisterObject("MyObject", jvnO);
        MySharedInterface proxy = JvnProxyFactory.createProxy((JvnObjectImpl) jvnO, MySharedInterface.class);
        System.out.println("[JVM1] created proxy anD registered MyObject");
        Thread.sleep(120_000);
    }
}
