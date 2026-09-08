# Taskomatic Android

Application Android native de gestion de tâches sans échéance, développée en Kotlin et Jetpack Compose. Dépôt indépendant de [Taskomatic iOS](https://github.com/pierre-teodoresco/taskomatic-ios).

La base de développement est en place ; les fonctionnalités sont construites sur une branche dédiée avec tests comportementaux et revues indépendantes.

Le stockage est local uniquement : aucun compte, service réseau ou cloud. Les sauvegardes automatiques du système sont désactivées. iCloud reste une fonctionnalité exclusivement iOS.

## Développement

JDK 17, SDK Android 36, Android 8 minimum. Ouvrir ce dossier dans Android Studio, ou consulter [le guide de développement](docs/development.md) pour la compilation et les tests en ligne de commande.

Le module `core` contient les règles métier indépendantes d’Android ; `app` contient l’interface et les intégrations système. Voir [l’architecture](docs/architecture.md) et [le workflow GitHub](docs/git-workflow.md).

## Licence

[MIT No Attribution (MIT-0)](LICENSE), comme la version iOS.
