# Framework Sarobidy

Un framework PHP moderne et léger pour le développement d'applications web.

## Prérequis

- PHP 8.0 ou supérieur
- Composer
- Serveur web (Apache/Nginx)
- MySQL/PostgreSQL (optionnel, selon la base de données utilisée)

## Installation

1. Cloner le dépôt :
   ```bash
   git clone https://github.com/herisarobidy/framework_sarobidy.git
   cd framework_sarobidy
   ```

2. Installer les dépendances :
   ```bash
   composer install
   ```

3. Configurer l'environnement :
   ```bash
   cp .env.example .env
   php artisan key:generate
   ```

4. Configurer la base de données dans le fichier `.env`

5. Lancer l'application :
   ```bash
   php -S localhost:8000 -t public
   ```

## Structure du projet

```
framework_sarobidy/
├── app/           # Code de l'application
├── config/        # Fichiers de configuration
├── public/        # Point d'entrée public
├── resources/     # Vues et assets
├── routes/        # Définition des routes
└── tests/         # Tests automatisés
```

## Contribution

1. Créer une branche pour votre fonctionnalité :
   ```bash
   git checkout -b ma-nouvelle-fonctionnalite
   ```

2. Faire un commit de vos modifications
3. Pousser vers la branche :
   ```bash
   git push origin ma-nouvelle-fonctionnalite
   ```
4. Créer une Pull Request

## Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.