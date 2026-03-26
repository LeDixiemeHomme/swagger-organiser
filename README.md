# Swagger Organiser

Outil de gestion et d'organisation de fichiers Swagger/OpenAPI.  
Le même JAR supporte deux modes d'utilisation : **CLI** et **serveur REST**.

## Build

```bash
./gradlew shadowJar
```

Le JAR produit : `build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar`

---

## Mode CLI

Le premier argument ne doit **pas** être `server` — les arguments sont transmis directement à picocli.

### Options

| Option | Obligatoire | Description |
|---|---|---|
| `-sf`, `--swaggerFilePath` | Oui | Chemin du fichier Swagger source |
| `-toRm`, `--endPointToRemove` | Non* | Endpoints à supprimer, format `method:path`, séparés par `,` |
| `-toKeep`, `--endPointToKeep` | Non* | Endpoints à conserver (tous les autres sont supprimés), prioritaire sur `-toRm` |
| `-d`, `--decomposeSwagger` | Non | Décompose le swagger en plusieurs fichiers |
| `-pf`, `--persistFile` | Non | Persiste le résultat dans des fichiers |

*Au moins `-toRm` ou `-toKeep` est requis.

### Exemples

```bash
# Supprimer un endpoint
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar \
  -sf src/main/resources/swagger-cobaye.yml \
  -toRm post:/profiling,get:/profilings

# Conserver uniquement certains endpoints et décomposer le résultat
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar \
  -sf src/main/resources/swagger-cobaye.yml \
  -toKeep post:/profiling,get:/profilings \
  -d -pf

# Aide
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar --help
```

Via Gradle :
```bash
./gradlew runCli --args="-sf src/main/resources/swagger-cobaye.yml -toRm post:/profiling,get:/profilings -pf -d"
```

---

## Mode Serveur REST

Passer `server` comme premier argument. Le port est optionnel (défaut : `8080`).

```bash
# Port par défaut (8080)
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar server

# Port personnalisé
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar server 9090
```

Via Gradle :
```bash
./gradlew runRest
```

### Endpoints disponibles

| Méthode | Chemin | Description |
|---|---|---|
| `POST` | `/clear-endpoints` | Supprime des endpoints du swagger fourni |
| `POST` | `/keep-endpoints` | Conserve uniquement les endpoints fournis |
| `POST` | `/decompose` | Décompose le swagger en une archive ZIP |
| `GET` | `/swagger-ui` | Interface graphique Swagger UI |

#### Paramètres communs (query string)

- `extension` *(obligatoire)* — format de sortie : `json`, `yml` ou `yaml`
- `endpoints` *(obligatoire pour `/clear-endpoints` et `/keep-endpoints`)* — liste séparée par des virgules, format `method:path`

#### Corps de la requête

Le fichier Swagger peut être envoyé :
- En **multipart/form-data** avec le champ `file` (recommandé)
- En **corps brut** (`application/octet-stream`)

### Exemples curl

```bash
# Supprimer un endpoint
curl -X POST \
  "http://localhost:8080/clear-endpoints?extension=yml&endpoints=get:/profiling/%7Bprofiling_id%7D" \
  -F "file=@swagger.yml" --output swagger-cleared.zip

# Conserver des endpoints
curl -X POST \
  "http://localhost:8080/keep-endpoints?extension=yml&endpoints=get:/profiling/%7Bprofiling_id%7D" \
  -F "file=@swagger.yml" --output swagger-kept.zip

# Décomposer
curl -X POST \
  "http://localhost:8080/decompose?extension=yml" \
  -F "file=@swagger.yml" --output swagger-decomposed.zip
```

---

## Analyse d'amélioration

Cette section recense les axes d'amelioration techniques identifies dans le package process.

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
