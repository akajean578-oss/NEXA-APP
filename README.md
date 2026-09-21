# Nexa — nouvelle architecture (React + Java Spring Boot)

Ton application Nexa a été transformée de "une seule page HTML/CSS/JS" vers une
**vraie architecture web séparée**, comme Facebook :

```
nexa/
├── backend/    → API Java (Spring Boot) : comptes, publications, likes, commentaires
└── frontend/   → Interface React (Vite) : le même design orange que ton fichier original
```

## Pourquoi cette architecture ?

Facebook (et toute app moderne à cette échelle) sépare :
- **le frontend** (ce que l'utilisateur voit et avec quoi il interagit) → React
- **le backend** (les données, la sécurité, la logique métier) → Java (Spring Boot)
- **la base de données** (stockage permanent) → PostgreSQL en production (H2 en local pour tester sans rien installer)

Ton ancien fichier HTML gardait tout en mémoire dans le navigateur : dès qu'on
rafraîchissait la page, tout disparaissait. Maintenant, les comptes, les
publications, les likes et les commentaires sont **réellement enregistrés**
côté serveur et partagés entre tous les utilisateurs.

## Ce qui fonctionne déjà

- Inscription / connexion avec mot de passe chiffré (BCrypt) + jeton de session (JWT)
- Fil d'actualité chargé depuis l'API (persistant, partagé entre utilisateurs)
- Créer une publication, aimer/ne plus aimer, commenter — tout est sauvegardé en base
- **Temps réel (WebSocket/STOMP)** : nouveaux posts signalés en direct (bandeau "Actualiser" plutôt qu'insertion brutale, pour ne pas perturber la lecture), likes et commentaires mis à jour en direct, notifications privées instantanées
- **Messagerie privée en direct** : chat 1-à-1 avec historique persisté, envoi/réception instantanés via WebSocket, indicateur "en train d'écrire...", compteur de messages non lus par conversation
- **Système d'amis** : demandes d'ami envoyées depuis la recherche, notification live à la réception, acceptation/refus, liste d'amis
- **Upload de médias** : ajout d'une photo à une publication (validation stricte du type et de la taille côté serveur, nom de fichier régénéré pour éviter tout risque de sécurité)
- **Recherche côté serveur** : recherche de personnes et de publications interrogeant directement la base de données (avec anti-rebond côté frontend)
- **Sécurité renforcée** :
  - limitation de débit (10 tentatives/minute/IP) sur les routes de connexion/inscription pour bloquer le brute-force
  - gestionnaire d'erreurs global : aucune trace technique ou stack Java n'est jamais renvoyée au client
  - en-têtes de sécurité (HSTS, protections XSS/MIME)
  - validation stricte des fichiers uploadés (type MIME vérifié, taille limitée à 8 Mo, chemin de fichier contrôlé)
  - le WebSocket exige lui aussi un jeton JWT valide à la connexion (pas d'accès anonyme au temps réel)
- Le même thème visuel orange/crème que ton design original
- Sidebar de navigation (Accueil / Profil / Amis / Groupes / Marché)

## Ce qu'il reste à faire pour vraiment "tourner comme Facebook"

- Pages Profil / Groupes / Marché : encore des pages vides prêtes à être remplies (Amis et Messages sont maintenant fonctionnels)
- Stories, réactions multiples (❤️ 😂 😮...), partage de publications
- Répartition de charge (plusieurs instances backend derrière un load balancer) et files d'attente (RabbitMQ/Kafka) pour les tâches lourdes (notifications en masse, traitement de médias) : pas nécessaires tant que le trafic reste modeste, mais l'architecture actuelle (backend sans état, JWT, cache externalisé dans Redis) est déjà prête à être dupliquée sur plusieurs instances le jour où c'est utile

## Tests automatisés

**Backend** (`backend/src/test`) :
- `PostServiceTest` — tests unitaires (Mockito) de la logique métier : créer un post, aimer/ne plus aimer, compter les likes/commentaires
- `AuthControllerTest` — tests unitaires de l'inscription/connexion (email déjà pris, mauvais mot de passe...)
- `PostFlowIntegrationTest` — test d'intégration `@SpringBootTest` + `MockMvc` qui simule un vrai parcours HTTP (inscription → publication → like → commentaire) sur une base H2 isolée (`application-test.properties`)
- Dépendances Testcontainers déjà ajoutées au `pom.xml` pour, si besoin, rejouer les mêmes tests contre un vrai PostgreSQL

Lancer : `cd backend && mvn test`

**Frontend** (`frontend/src/test`) :
- `Composer.test.jsx`, `Post.test.jsx` — tests de composants avec Vitest + React Testing Library (ouverture du champ de saisie, publication, like, chargement des commentaires)
- `e2e/social-flow.spec.js` — test end-to-end Playwright qui pilote un vrai navigateur sur le parcours complet inscription → post → like → commentaire

Lancer :
```bash
cd frontend
npm run test          # tests de composants (rapides, pas besoin du backend)
npm run test:e2e      # end-to-end (nécessite backend + frontend démarrés)
```

## Déploiement & infrastructure

- **Docker** : `backend/Dockerfile` (build Maven multi-étapes, image finale légère Alpine) et `frontend/Dockerfile` (build Vite puis servi par Nginx, avec `frontend/nginx.conf` qui proxifie `/api`, `/uploads` et `/ws` vers le backend)
- **docker-compose.yml** (à la racine) orchestre backend + frontend + PostgreSQL + Redis en une commande :
  ```bash
  JWT_SECRET=une_valeur_longue_et_aleatoire DB_PASSWORD=un_mot_de_passe docker compose up --build
  ```
- **CI/CD** : `.github/workflows/ci-cd.yml` — à chaque push sur `main`, lance les tests backend et frontend, puis (si tout passe) construit et publie les images Docker sur GitHub Container Registry. L'étape de déploiement effectif (Railway/Render/AWS...) est indiquée en commentaire à la fin du fichier — à brancher sur ton hébergeur une fois choisi
- **Variables d'environnement** : `application-prod.properties` ne contient plus aucun secret en dur — tout vient de l'environnement (`NEXA_JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`...), comme le préconise `docker-compose.yml`
- **Nom de domaine + HTTPS** : à configurer côté hébergeur (Railway/Render/Vercel gèrent Let's Encrypt automatiquement) ; rien à coder de plus dans l'app

## Scalabilité

- **Cache Redis** : le fil d'actualité (`PostService.getRawFeed`) est annoté `@Cacheable("feedPosts")` — en profil `prod`, `spring.cache.type=redis` fait passer ce cache par Redis (partagé entre toutes les instances du backend) ; en local, Spring utilise un cache mémoire simple, donc pas besoin de lancer Redis pour développer. Le cache est invalidé automatiquement à chaque nouvelle publication (`@CacheEvict`)
- **CDN** : les images uploadées sont déjà servies via une route dédiée (`/uploads/**`) — il suffit de pointer un CDN (Cloudflare, CloudFront...) dessus, ou de migrer le stockage vers S3/Cloudinary directement (voir la section précédente) pour bénéficier d'une distribution mondiale sans changer le reste du code
- **Répartition de charge & files d'attente** : non implémentées (inutiles tant que le trafic est modeste), mais le backend est déjà sans état (JWT, pas de session serveur) donc prêt à être dupliqué derrière un load balancer ; l'ajout d'une file (RabbitMQ/Kafka) pour les tâches lourdes se brancherait naturellement à l'endroit où `RealtimeNotificationService` est actuellement appelé de façon synchrone
- Passage en PostgreSQL en production (déjà préparé dans `application.properties`, juste à décommenter)
- Déploiement : backend sur un serveur Java (Render, Railway, AWS...), frontend buildé (`npm run build`) servi en statique (Vercel, Netlify...), et un vrai stockage de fichiers (S3, Cloudinary...) plutôt que le disque local utilisé ici pour le développement

## Lancer le projet en local

### 1. Backend (nécessite Java 17+ et Maven)
```bash
cd backend
mvn spring-boot:run
```
L'API démarre sur `http://localhost:8080`. Elle utilise une base H2 en mémoire :
aucune installation de base de données n'est nécessaire pour tester.

### 2. Frontend (nécessite Node.js 18+)
```bash
cd frontend
npm install
npm run dev
```
L'application s'ouvre sur `http://localhost:5173` et communique automatiquement
avec l'API backend (proxy déjà configuré dans `vite.config.js`).

### 3. Utilisation
Ouvre `http://localhost:5173`, crée un compte, publie, aime, commente. Chaque
action est enregistrée par le backend Java.

## Structure des principaux fichiers

**Backend**
- `model/` : les entités (User, Post, Comment, Like)
- `repository/` : accès base de données (Spring Data JPA)
- `service/PostService.java` : la logique métier du fil d'actualité
- `controller/` : les routes de l'API (`/api/auth`, `/api/posts`)
- `config/` : sécurité, JWT

**Frontend**
- `src/pages/` : Login, Register, Home
- `src/components/` : Header, Sidebar, Composer, Post
- `src/context/AuthContext.jsx` : gestion de la session utilisateur
- `src/api/client.js` : appels à l'API (ajoute automatiquement le jeton JWT)
- `src/styles.css` : ton design original, conservé à l'identique
