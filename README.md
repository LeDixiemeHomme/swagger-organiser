# Swagger Organiser

## Build et exécution

```bash
./gradlew shadowJar
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar -sf src/main/resources/swagger-cobaye.yml -toRm post:/cadh/v1/operations -pf -d
```

## Analyse d'amélioration (mise a jour)

Cette section recense les axes d'amelioration techniques identifies dans le package process.

Important: le point "Suppression d'endpoint potentiellement trop large" n'apparait plus ici, car il a deja ete corrige dans le code et valide par tests unitaires.

### Priorite haute

- Robustesse de la navigation JSON sans NullPointerException de controle de flux.
Dans la resolution d'un endpoint, la logique repose encore sur un try/catch de NullPointerException.
Amelioration: verifier explicitement la presence de paths, path, method puis lever une exception metier claire et explicite.

- Coherence du modele SwaggerNode (record mutable).
Le type est un record mais plusieurs methodes mutent le JsonNode interne et retournent this.
Amelioration: choisir un contrat clair:
immutabilite stricte (copie + nouvel objet) ou mutabilite assumee (classe classique).

- addPathFileReferences a valider sur des cas reels OpenAPI.
Le parcours actuel traite les cles de premier niveau sous paths et peut manquer des references si la structure differe.
Amelioration: couvrir les structures reelles avec tests de non-regression supplementaires et ajuster le parcours si besoin.

### Priorite moyenne

- Parsing EndPoint.fromString fragile.
Le split sur : ne valide pas le format d'entree.
Amelioration: utiliser split(":", 2), verifier method/path non vides, et produire un message utilisateur exploitable en CLI.

- equals redefini dans un record EndPoint.
Le record fournit deja equals/hashCode.
Amelioration: supprimer la redefinition manuelle pour eviter toute incoherence future.

- Gestion defensive des refs dans DollarRef.
Certaines resolutions de reference supposent un format toujours valide.
Amelioration: valider le format de ref et lever une exception metier en cas d'invalidite au lieu de laisser remonter des erreurs techniques.

- Risque de collision dans decomposeComponent.
L'aplatissement des composants peut melanger des elements homonymes issus de sections differentes.
Amelioration: conserver la section d'origine (schemas, responses, etc.) dans la structure de decomposition.

### Priorite basse

- Messages d'exception et logs a sanitiser.
Eviter les extraits de payload brut dans les exceptions si des donnees sensibles peuvent exister.

- Extension.getSwaggerFileExtension sensible a la casse.
Amelioration: normaliser l'extension en lower case avant comparaison.

- ValidationUtils a clarifier.
L'API retourne des violations mais leve deja une exception quand elles existent.
Amelioration: choisir une seule strategie (retour des violations ou exception), et clarifier le contrat.

## Tests recommandes (prochaines etapes)

- Cas d'erreur sur endpoint introuvable avec message metier stable.
- Cas d'entree CLI invalide pour EndPoint.fromString.
- Cas de refs mal formees dans DollarRef.
- Cas de collisions de noms lors de la decomposition des components.
