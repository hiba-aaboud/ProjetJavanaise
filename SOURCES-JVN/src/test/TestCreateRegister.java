package test;

import jvn.JvnObject;
import jvn.JvnServerImpl;
import jvn.MySharedObject;

public class TestCreateRegister {
		
	public static void main(String[] args) throws Exception {
        JvnServerImpl js = JvnServerImpl.jvnGetServer();
        MySharedObject o =  new MySharedObject("hello word-from-JVM1");
        JvnObject jvnO = js.jvnCreateObject(o);
        js.jvnRegisterObject("MyObject", jvnO);
        System.out.println("[JVM1] créé et enregistré MyObject");
        
        Thread.sleep(120_000);
    }
}
