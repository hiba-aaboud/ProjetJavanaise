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
Le projet contient plusieurs tests automatiques :

####  **BurstRunner**
Teste la cohérence sous forte charge concurrente (multi-threads / multi-serveurs) :
```bash
java test.BurstRunner
```
Output :
```
Committed writes: 22282
Final counter:    22282
 OK — cohérence préservée
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
