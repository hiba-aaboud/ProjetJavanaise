package proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import annotations.JvnRead;
import annotations.JvnWrite;
import jvn.JvnObjectImpl;

public class JvnInvocationHandler implements InvocationHandler {

    private final JvnObjectImpl jvnObject;

    public JvnInvocationHandler(JvnObjectImpl jvnObject) {
        this.jvnObject = jvnObject;
    }

    private boolean isRead(Method m) {
        if (m.isAnnotationPresent((Class<? extends Annotation>) JvnRead.class)) return true;
        if (m.isAnnotationPresent(JvnWrite.class)) return false;
        String name = m.getName();
        if (name.startsWith("get") || name.startsWith("is")) return true;
        return false; // conservateur -> write si indéterminé
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // TODO Auto-generated method stub
        boolean read = isRead(method);
        // Acquire lock (JvnObjectImpl gère sa propre synchronisation interne)
        if (read) {
            jvnObject.jvnLockRead();
        } else {
            jvnObject.jvnLockWrite();
        }

        try {
            Object real = jvnObject.jvnGetSharedObject();
            if (real == null) {
                // improbable dans un usage normal, mais on protège
                throw new IllegalStateException("Shared object is null for JVN object id " + jvnObject.jvnGetObjectId());
            }

            try {
                return method.invoke(real, args);
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        } finally {
            jvnObject.jvnUnLock();
        }
    }

}