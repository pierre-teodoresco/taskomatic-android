# Taskomatic Android

Application Android native de gestion de tâches sans échéance, développée en Kotlin et Jetpack Compose. Dépôt indépendant de [Taskomatic iOS](https://github.com/pierre-teodoresco/taskomatic-ios).

Première version native en développement, sur une branche dédiée avec tests comportementaux et revues indépendantes. Elle propose :

- Création rapide, notes, édition, validation, annulation d’une validation, restauration et suppression confirmée.
- Récurrences de 1 à 99 jours, semaines ou mois, sans échéances ni accumulation de cycles manqués.
- Listes des tâches actives, en attente de leur prochain cycle et terminées.
- Rappels locaux selon une heure et des jours choisis, notification de test et action « Terminer » protégée contre les anciens cycles.
- Thèmes système, clair et sombre ; interface française et anglaise sans traduction des contenus saisis.
- Import/export manuel JSON compatible iOS, avec aperçu, confirmation et import add-only : une tâche existante n’est jamais écrasée.

Le stockage est local uniquement : aucun compte, service réseau ou cloud. Les sauvegardes automatiques du système sont désactivées. iCloud reste une fonctionnalité exclusivement iOS.

Les fichiers sont choisis explicitement dans le sélecteur Android. Un fournisseur de documents peut proposer un emplacement distant ; Taskomatic ne réalise aucune sauvegarde automatique vers cet emplacement. Les exports contiennent les titres et notes en clair : conserver les fichiers dans un emplacement approprié.

Les rappels sont approximatifs et peuvent être retardés par Android, notamment pour économiser la batterie. L’app renouvelle la planification au démarrage, après redémarrage de l’appareil et lors des changements d’heure/fuseau. Après un arrêt forcé, rouvrir l’app. La désinstallation supprime les données locales.

## Développement

JDK 17, SDK Android 36, Android 8 minimum. Ouvrir ce dossier dans Android Studio, ou consulter [le guide de développement](docs/development.md) pour la compilation et les tests en ligne de commande.

Le module `core` contient les règles métier indépendantes d’Android ; `app` contient l’interface et les intégrations système. Voir [l’architecture](docs/architecture.md) et [le workflow GitHub](docs/git-workflow.md).

La variante debug est installée sous un identifiant distinct (`.debug`) pour isoler ses données. La version release est optimisée mais non signée : cette première livraison n’est pas une publication Google Play.

## Licence

[MIT No Attribution (MIT-0)](LICENSE), comme la version iOS.
