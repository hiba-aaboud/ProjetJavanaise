package proxy;

import java.lang.reflect.Proxy;

import jvn.JvnObjectImpl;

import java.lang.reflect.*;
import jvn.*;

public class JvnProxyFactory {

	@SuppressWarnings("unchecked")
	public static <T> T createProxy(JvnObjectImpl jvnObject, Class<T> iface) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[] { iface },
                new JvnInvocationHandler(jvnObject)
        );
    }
	
	
	public static Object createProxyForObject(JvnObjectImpl jvnObject) {
        try {
            Object real = jvnObject.jvnGetSharedObject();
            if (real == null) return null;
            Class<?>[] interfaces = real.getClass().getInterfaces();
            if (interfaces.length == 0)
                throw new IllegalArgumentException("Target object has no interfaces; cannot create dynamic proxy");
            return Proxy.newProxyInstance(
                    real.getClass().getClassLoader(),
                    interfaces,
                    new JvnInvocationHandler(jvnObject)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
