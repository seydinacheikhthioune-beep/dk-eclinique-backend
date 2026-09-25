cd# Déploiement sur Railway (Backend + MySQL)

Le projet est prêt pour Railway : `Dockerfile`, `railway.json`, et `application.yml`
qui lit automatiquement les variables du service MySQL de Railway.

## 1. Pousser le code sur GitHub

```bash
git add .
git commit -m "Passage à MySQL + configuration Railway"
git push origin master
```

## 2. Créer le projet Railway

1. Sur https://railway.com → **New Project** → **Deploy from GitHub repo** → choisir `e-clinique-backend`.
2. Dans le même projet : **+ New** → **Database** → **Add MySQL**.

## 3. Relier le backend à MySQL

Service backend → onglet **Variables** → **Raw Editor**, coller :

```
MYSQLHOST=${{MySQL.MYSQLHOST}}
MYSQLPORT=${{MySQL.MYSQLPORT}}
MYSQLDATABASE=${{MySQL.MYSQLDATABASE}}
MYSQLUSER=${{MySQL.MYSQLUSER}}
MYSQLPASSWORD=${{MySQL.MYSQLPASSWORD}}
JWT_SECRET=remplacez-par-une-longue-chaine-aleatoire-de-64-caracteres-minimum
```

(`MySQL` est le nom du service base de données ; adaptez-le s'il est différent.)

Générer un secret JWT : `openssl rand -base64 64 | tr -d '\n'`

## 4. Exposer l'API

Service backend → **Settings** → **Networking** → **Generate Domain**.
Railway fournit le port via `PORT`, l'application l'utilise automatiquement.

L'API est alors disponible sur :

- `https://<votre-domaine>.up.railway.app/api`
- Swagger : `https://<votre-domaine>.up.railway.app/api/swagger-ui.html`

Les tables sont créées automatiquement au premier démarrage (`ddl-auto: update`),
ainsi que le compte administrateur par défaut.

## Variables disponibles

| Variable | Rôle | Défaut |
|---|---|---|
| `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD` | Connexion MySQL | `localhost:3306/eclinique`, `root/root` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Surcharge complète de la connexion (JDBC) | — |
| `JWT_SECRET` | Clé de signature des tokens | valeur de dev (à changer !) |
| `DB_POOL_SIZE` | Taille du pool de connexions | `5` |
| `PORT` | Port HTTP (fourni par Railway) | `8282` |

## En local

- Avec Docker : `docker compose up -d` depuis la racine `e-clinique/` (MySQL sur le port hôte 3307).
- Sans base : `mvn spring-boot:run -Dspring-boot.run.profiles=h2` (H2 en mémoire).

## Frontend

Le frontend calcule actuellement l'URL de l'API avec `hostname:8282`, ce qui ne
fonctionne pas sur Railway (HTTPS sur le port 443). Il faudra y mettre l'URL du backend
Railway, par ex. `apiUrl: 'https://<votre-domaine>.up.railway.app/api'`.
