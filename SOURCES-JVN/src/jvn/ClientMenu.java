package client;

import jvn.*;
import proxy.JvnProxyFactory;

import java.lang.reflect.Method;
import java.rmi.Naming;
import java.util.*;

public class ClientMenu {


    private static final String PRESET_COUNTER_CLASS  = "jvn.Counter";
    private static final String PRESET_SENTENCE_CLASS = "irc.Sentence";
    private static final String PRESET_COUNTER_IFACE  = "jvn.CounterItf";
    private static final String PRESET_SENTENCE_IFACE = "jvn.SentenceItf";

    private final Map<String, JvnObject> localObjects = new HashMap<>();
    private final Map<String, Object>    proxies      = new HashMap<>();
    private final JvnServerImpl server;

    public ClientMenu(JvnServerImpl server) {
        this.server = server;
    }

    public static void main(String[] args) {
        // Démarrer/attacher le coordinateur
        try { Naming.lookup("rmi://localhost:1099/JvnCoord"); }
        catch (Exception _e) { try { new JvnCoordImpl(); } catch (Exception ignored) {} }

        try {
            JvnServerImpl srv = JvnServerImpl.jvnGetServer();
            new ClientMenu(srv).loop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loop() {
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                printMenu();
                System.out.print("Choix: ");
                String c = sc.nextLine().trim();
                switch (c) {
                    case "1" -> creerObjet(sc);
                    case "2" -> afficherCache();
                    case "3" -> ecrireObjet(sc);
                    case "4" -> flushCache(sc);
                    case "0" -> { System.out.println("Au revoir !"); return; }
                    default -> System.out.println("Choix inconnu.");
                }
                System.out.println();
            }
        }
    }

    private void printMenu() {
        System.out.println("=======================================");
        System.out.println("       Client Javanaise (Menu)");
        System.out.println("=======================================");
        System.out.println(" 1) Créer un objet");
        System.out.println(" 2) Afficher le cache local");
        System.out.println(" 3) Écrire dans un objet");
        System.out.println(" 4) Flush cache (oublier localement)");
        System.out.println(" 0) Quitter");
        System.out.println("=======================================");
    }


    private void creerObjet(Scanner sc) {
        try {
            System.out.print("Nom de l'objet à créer: ");
            String name = sc.nextLine().trim();
            if (name.isEmpty()) { System.out.println("Nom requis."); return; }
            if (localObjects.containsKey(name)) {
                System.out.println("Un objet local nommé '" + name + "' existe déjà.");
                return;
            }

            System.out.println("Type d'objet:");
            System.out.println("  1) Counter (" + PRESET_COUNTER_CLASS + ")");
            System.out.println("  2) Sentence (" + PRESET_SENTENCE_CLASS + ")");

            System.out.print("Choix: ");
            String t = sc.nextLine().trim();

            String fqcn;
            switch (t) {
                case "1" -> fqcn = PRESET_COUNTER_CLASS;
                case "2" -> fqcn = PRESET_SENTENCE_CLASS;
                case "3" -> {
                    System.out.print("Entrer le FQCN de l'implémentation: ");
                    fqcn = sc.nextLine().trim();
                }
                default -> { System.out.println("Type inconnu."); return; }
            }

            Object initial = Class.forName(fqcn).getDeclaredConstructor().newInstance();
            JvnObject jo = server.jvnCreateObject((java.io.Serializable) initial);
            server.jvnRegisterObject(name, jo);
            localObjects.put(name, jo);

            Object px = buildProxyFor(name, jo, fqcn);
            if (px != null) proxies.put(name, px);

            int id = ((JvnObjectImpl) jo).jvnGetObjectId();
            System.out.println("Créé '" + name + "' (id=" + id + ").");

        } catch (Throwable e) {
            System.err.println("Échec création: " + e);
        }
    }

    private Object buildProxyFor(String name, JvnObject jo, String implFqcn) {
        try {
            if (implFqcn.equals(PRESET_COUNTER_CLASS)) {
                try {
                    Class<?> itf = Class.forName(PRESET_COUNTER_IFACE);
                    return JvnProxyFactory.createProxy((JvnObjectImpl) jo, itf);
                } catch (ClassNotFoundException e) {
                    return JvnProxyFactory.createProxyForObject((JvnObjectImpl) jo);
                }
            } else if (implFqcn.equals(PRESET_SENTENCE_CLASS)) {
                try {
                    Class<?> itf = Class.forName(PRESET_SENTENCE_IFACE);
                    return JvnProxyFactory.createProxy((JvnObjectImpl) jo, itf);
                } catch (ClassNotFoundException e) {
                    return JvnProxyFactory.createProxyForObject((JvnObjectImpl) jo);
                }
            } else {
                // Demander éventuellement l'interface
                return JvnProxyFactory.createProxyForObject((JvnObjectImpl) jo);
            }
        } catch (Throwable e) {
            System.err.println("Proxy échoué pour '" + name + "': " + e);
            return null;
        }
    }


    private void afficherCache() {
        if (localObjects.isEmpty()) {
            System.out.println("Cache local vide.");
            return;
        }
        System.out.println("Cache local (objets connus par CE client) :");
        for (var entry : localObjects.entrySet()) {
            String name = entry.getKey();
            JvnObject jo = entry.getValue();
            Object px = proxies.get(name);
            try {
                int id = ((JvnObjectImpl) jo).jvnGetObjectId();
                // Essayer de lire la valeur actuelle sous lock R (si possible)
                String valueStr;
                try {
                    ((JvnObjectImpl) jo).jvnLockRead();
                    try {
                        Object real = ((JvnObjectImpl) jo).jvnGetSharedObject();
                        valueStr = String.valueOf(real);
                    } finally {
                        ((JvnObjectImpl) jo).jvnUnLock();
                    }
                } catch (Exception e) {
                    valueStr = "(lecture échouée: " + e.getMessage() + ")";
                }

                System.out.println(" - " + name +
                        " | id=" + id +
                        " | proxy=" + (px == null ? "aucun" : px.getClass().getInterfaces().length == 0
                        ? px.getClass().getSimpleName() : px.getClass().getInterfaces()[0].getSimpleName()) +
                        " | valeur=" + valueStr);
            } catch (Exception e) {
                System.out.println(" - " + name + " (erreur: " + e + ")");
            }
        }
    }


    private void ecrireObjet(Scanner sc) {
        String name = askName(sc);
        if (name == null) return;

        JvnObject jo = localObjects.get(name);
        Object px = proxies.get(name);

        System.out.println("Choisir l'action d'écriture :");
        System.out.println("  1) Counter.inc()");
        System.out.println("  2) Counter.set(int)");
        System.out.println("  3) Sentence.write(String)");
        System.out.print("Choix: ");
        String ch = sc.nextLine().trim();

        try {
            switch (ch) {
                case "1" -> {
                    if (px != null) {
                        try {
                            Method inc = px.getClass().getMethod("inc");
                            inc.invoke(px);
                        } catch (NoSuchMethodException noProxyMethod) {
                            // fallback: lock write direct + reflect
                            writeCounterIncDirect(jo);
                        }
                    } else {
                        writeCounterIncDirect(jo);
                    }
                    // feedback lecture
                    readAndPrintValue(jo, "Après inc()");
                }
                case "2" -> {
                    System.out.print("Valeur (int) : ");
                    int v = Integer.parseInt(sc.nextLine().trim());
                    if (px != null) {
                        try {
                            Method set = px.getClass().getMethod("set", int.class);
                            set.invoke(px, v);
                        } catch (NoSuchMethodException noProxyMethod) {
                            writeCounterSetDirect(jo, v);
                        }
                    } else {
                        writeCounterSetDirect(jo, v);
                    }
                    readAndPrintValue(jo, "Après set(" + v + ")");
                }
                case "3" -> {
                    System.out.print("Texte : ");
                    String s = sc.nextLine();
                    if (px != null) {
                        try {
                            Method w = px.getClass().getMethod("write", String.class);
                            w.invoke(px, s);
                        } catch (NoSuchMethodException noProxyMethod) {
                            writeSentenceDirect(jo, s);
                        }
                    } else {
                        writeSentenceDirect(jo, s);
                    }
                    readAndPrintValue(jo, "Après write(\"" + s + "\")");
                }
                default -> System.out.println("Choix inconnu.");
            }
        } catch (Throwable e) {
            System.err.println("Écriture échouée: " + e);
        }
    }

    private void writeCounterIncDirect(JvnObject jo) throws Exception {
        ((JvnObjectImpl) jo).jvnLockWrite();
        try {
            Object real = ((JvnObjectImpl) jo).jvnGetSharedObject();
            Method inc = real.getClass().getMethod("inc");
            inc.invoke(real);
            ((JvnObjectImpl) jo).overwriteSharedObject((java.io.Serializable) real);
        } finally {
            ((JvnObjectImpl) jo).jvnUnLock();
        }
    }

    private void writeCounterSetDirect(JvnObject jo, int v) throws Exception {
        ((JvnObjectImpl) jo).jvnLockWrite();
        try {
            Object real = ((JvnObjectImpl) jo).jvnGetSharedObject();
            Method set = real.getClass().getMethod("set", int.class);
            set.invoke(real, v);
            ((JvnObjectImpl) jo).overwriteSharedObject((java.io.Serializable) real);
        } finally {
            ((JvnObjectImpl) jo).jvnUnLock();
        }
    }

    private void writeSentenceDirect(JvnObject jo, String s) throws Exception {
        ((JvnObjectImpl) jo).jvnLockWrite();
        try {
            Object real = ((JvnObjectImpl) jo).jvnGetSharedObject();
            Method write = real.getClass().getMethod("write", String.class);
            write.invoke(real, s);
            ((JvnObjectImpl) jo).overwriteSharedObject((java.io.Serializable) real);
        } finally {
            ((JvnObjectImpl) jo).jvnUnLock();
        }
    }

    private void readAndPrintValue(JvnObject jo, String prefix) {
        try {
            ((JvnObjectImpl) jo).jvnLockRead();
            try {
                Object real = ((JvnObjectImpl) jo).jvnGetSharedObject();
                // essayer get() ou read() si dispo
                String valStr;
                try {
                    Method get = real.getClass().getMethod("get");
                    valStr = String.valueOf(get.invoke(real));
                } catch (NoSuchMethodException e1) {
                    try {
                        Method read = real.getClass().getMethod("read");
                        valStr = String.valueOf(read.invoke(real));
                    } catch (NoSuchMethodException e2) {
                        valStr = String.valueOf(real);
                    }
                }
                System.out.println(prefix + " -> " + valStr);
            } finally {
                ((JvnObjectImpl) jo).jvnUnLock();
            }
        } catch (Exception e) {
            System.out.println(prefix + " (lecture échouée) : " + e);
        }
    }


    private void flushCache(Scanner sc) {
        System.out.println("Flush :");
        System.out.println("  1) Oublier un objet local");
        System.out.println("  2) Oublier TOUT le cache local");
        System.out.print("Choix: ");
        String ch = sc.nextLine().trim();

        switch (ch) {
            case "1" -> {
                String name = askNameOptional(sc);
                if (name == null) return;
                proxies.remove(name);
                localObjects.remove(name);
                System.out.println("→ Objet '" + name + "' oublié localement (il existe toujours globalement).");
            }
            case "2" -> {
                proxies.clear();
                localObjects.clear();
                System.out.println("→ Cache local vidé. (Les objets globaux restent enregistrés chez le coord.)");
            }
            default -> System.out.println("Choix inconnu.");
        }
    }



    private String askName(Scanner sc) {
        System.out.print("Nom de l'objet: ");
        String name = sc.nextLine().trim();
        if (!localObjects.containsKey(name)) {
            // tenter un lookup pour l'ajouter au cache
            try {
                JvnObject jo = server.jvnLookupObject(name);
                if (jo != null) {
                    localObjects.put(name, jo);
                    // essaie un proxy générique
                    Object px = JvnProxyFactory.createProxyForObject((JvnObjectImpl) jo);
                    if (px != null) proxies.put(name, px);
                    return name;
                }
            } catch (Exception ignored) {}
            System.out.println("Inconnu localement et lookup introuvable. Crée-le d'abord.");
            return null;
        }
        return name;
    }

    private String askNameOptional(Scanner sc) {
        System.out.print("Nom de l'objet: ");
        String name = sc.nextLine().trim();
        if (!localObjects.containsKey(name)) {
            System.out.println("Inconnu localement.");
            return null;
        }
        return name;
    }
}
