# Projet Javanaise

##  Présentation

Le projet **Javanaise** est une implémentation simplifiée d’un **système de cohérence d’objets distribués** en Java, basé sur RMI.  
Chaque objet partagé (ex: `Counter`, `Sentence`) peut être manipulé depuis plusieurs processus clients, avec une **gestion automatique des verrous** (`Read`, `Write`) coordonnée par un **serveur central** (*Coordinateur*).

Le système permet :
- la création d’objets partagés,
- la lecture et modification concurrente de leur état,
- le caching local avec invalidation,
- et la synchronisation des mises à jour via le *coordinateur Javanaise*.

---

##  Architecture du projet

```
ProjetJavanaise/
├── jvn/
    ├── ClientMenu.java         Interface console interactive (création, lecture, écriture, flush)
│   ├── JvnObject.java          Interface d’un objet partagé
│   ├── JvnObjectImpl.java      Implémentation concrète avec gestion des locks
│   ├── JvnServerImpl.java      Serveur local côté client
│   ├── JvnCoordImpl.java       Coordinateur central (RMI)
│   ├── JvnRemoteCoord.java     Interface RMI du coordinateur
│   ├── JvnRemoteServer.java    Interface RMI des serveurs
│   ├── ObjectMeta.java         Métadonnées côté coordinateur (état, lecteurs, writer)
│   ├── JvnException.java       Exception spécifique Javanaise
│   ├── Counter.java (optionnel) Exemple d’objet partagé (compteur)
│
├── proxy/
│   ├── JvnInvocationHandler.java  Gère automatiquement les locks via les annotations (@JvnRead/@JvnWrite)
│   ├── JvnProxyFactory.java       Crée les proxies dynamiques
│
├── test/
│   ├── BurstRunner.java         Test automatique multi-thread (validation de cohérence)
│
└── annotations/
    ├── JvnRead.java             Annotation pour les méthodes de lecture
    ├── JvnWrite.java            Annotation pour les méthodes d’écriture
```

---

##  Installation & Exécution

###  Prérequis
- **Java 17+** (ou supérieur)
- IntelliJ IDEA / VS Code / Eclipse  
- Aucun serveur externe requis (RMI créé automatiquement)

---

##  Étapes d’exécution

### 1️ Démarrer le coordinateur
Lancer simplement le coordinateur :
```bash
java jvn.JvnCoordImpl
```
 Le registre RMI sera créé automatiquement sur le port `1099`.

---

### 2 Lancer un ou plusieurs clients
Dans d’autres terminaux ou IDE :
```bash
java jvn.ClientMenu
```

Le menu interactif permet de :
```
1) Créer un objet
2) Afficher le cache local
3) Écrire dans un objet
4) Flush cache
0) Quitter
```

Chaque client peut créer, lire, et modifier les mêmes objets partagés simultanément.

---

### 3️ Exécuter les tests de cohérence
Le projet contient plusieurs tests voici un example de flow pour tester la version 1, on a aussi d'autre pour tester le proxy

####  **TestCreateRegister**
Cree un registre RMI :
```bash
java test.TestCreateRegister
```
Output :
```
[JvnServer] Connected to JvnCoord
id object = 1name object = MyObject[JVM1] créé et enregistré MyObject
```
####  **TestLookupRead**
Output :
```
[JvnServer] Connected to JvnCoord
etat lock inital :NL
id de notre objet read1
valeur du local hello :
valeur du local state :Mon Objet Partagé(hello word-from-JVM1)
valeur du local reader :jvn.JvnObjectImpl@65e579dc
etat lock final read :R
got lock read
[JVM2] lecture objet: hello word-from-JVM1
```
####  **TestWriteThenReadCache**
Output :
```
[JvnServer] Connected to JvnCoord
etat lock write :NL
id de notre objet1
valeur du local :jvn.JvnObjectImpl@65e579dc
etat lock write final :W
got lock read hello word-from-JVM1
[JVM3] jvnLockWrite obtenu, new value = Modifie par JVM3
[JVM3] write terminé, jvnUnLock()
[JVM3] etat lock 
```
Pour tester le proxy:
####  **burstProxy**
Output:
```
[JvnServer] Connected to JvnCoord
id object = 3name object = counter-1762164968495LA VALEUR FINAL o1:jvn.JvnObjectImpl@27082746
LA VALEUR FINAL o2:jvn.JvnObjectImpl@270421f5
OBJ IDs: S1=3 S2=3
etat lock write :NL
id de notre objet3
valeur du local :jvn.JvnObjectImpl@270421f5
etat lock write final :W
etat lock inital :WC
etat lock inital :NL
id de notre objet read3
id de notre objet read3
valeur du local hello :
etat lock final read :RWC
etat lock inital :RWC
id de notre objet read3
etat lock final read :RWC
etat lock inital :RWC
id de notre objet read3
etat lock final read :RWC
etat lock inital :WC
id de notre objet read3
etat lock final read :RWC
etat lock inital :RWC
id de notre objet read3
etat lock final read :RWC
etat lock inital :WC
id de notre objet read3
etat lock final read :RWC
valeur du local state :Counter(0)
valeur du local reader :jvn.JvnObjectImpl@270421f5
etat lock final read :R
etat lock inital :RC
id de notre objet read3
etat lock final read :R
etat lock inital :RC
id de notre objet read3
etat lock final read :R
etat lock write :RC
id de notre objet3
valeur du local :jvn.JvnObjectImpl@270421f5
etat lock write final :W
etat lock inital :WC
id de notre objet read3
etat lock final read :RWC
etat lock write :WC
id de notre objet3
etat lock write final :W
LA VALEUR FINAL C2:Counter(1)
LA VALEUR commited commited:1
Committed writes: 1
Final counter:   1
 OK — cached version passed!

```
---

##  Exemple d’utilisation (Client CLI)

```
=======================================
       Client Javanaise (Menu)
=======================================
 1) Créer un objet
 2) Afficher le cache local
 3) Écrire dans un objet
 4) Flush cache
 0) Quitter
=======================================
Choix: 1
Nom de l'objet à créer: compteur
Type d'objet: 1) Counter
Créé 'compteur' (id=1)
```

Puis :
```
Choix: 3
Nom de l'objet: compteur
Action: 1) Counter.inc()
Après inc() -> 5
```

Et enfin :
```
Choix: 2
Cache local :
 - compteur | id=1 | valeur=Counter(5)
```

---

##  Mécanisme interne (résumé)

| Élément | Rôle |
|----------|------|
| **JvnCoordImpl** | Gère les verrous et synchronise les états (invalidate reader/writer) |
| **JvnServerImpl** | Fournit les opérations locales (lookup, create, lockRead/Write) |
| **JvnObjectImpl** | Contient l’état et le lock courant (RC, WC, R, W, RWC, NL) |
| **Proxy dynamique** | Intercepte les appels et applique automatiquement les verrous via les annotations |
| **Cache local** | Garde les objets récemment utilisés pour réduire les appels RMI |
| **Invalidations** | Le coord notifie les clients pour relâcher ou invalider leurs caches |

---

##  États possibles d’un objet

| État | Description |
|------|--------------|
| **NL** | Non locké |
| **RC** | Cache lecture valide |
| **WC** | Cache écriture valide |
| **R**  | Verrou lecture détenu |
| **W**  | Verrou écriture détenu |
| **RWC**| Cache combiné lecture/écriture |

---

##  Scénarios de test

| Test | Objectif | Résultat attendu |
|------|-----------|------------------|
| **BurstRunner** | Stress-test concurrent | Aucune perte d’incréments |
| **ClientMenu** | Interaction manuelle | Valeurs cohérentes entre clients |

---

## Flush & Reset

- `Flush cache` vide uniquement le cache local (les objets restent dans le système global).
- Pour redémarrer complètement le système, il suffit de relancer :
  ```bash
  killall java  # (ou stopper les processus)
  java jvn.JvnCoordImpl
  java client.ClientMenu
  ```

---

##  Auteurs & Maintenance

- Projet réalisé dans le cadre du cours **Systèmes et Applications Réparties (SAR)**.  
- Inspiré de l’architecture **Javanaise** (Université Grenoble INP – UGA).  
- Contributions : **Maroua Aaboud**, **Bahae ddine Moutaoukil**, **Jocelyn Kouayep Tankio**, 2025.   

---

## Exemple rapide

```bash
# Terminal 1
java jvn.JvnCoordImpl

# Terminal 2
java jvn.ClientMenu
#  créer un compteur et l’incrémenter

# Terminal 3
java jvn.ClientMenu
#  faire un lookup sur le même nom et lire la valeur (synchro assurée)
```
