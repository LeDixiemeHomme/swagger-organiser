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
| `-sf`, `--swaggerFilePath`, `--swagger-file` | Oui | Chemin du fichier Swagger source |
| `-toRm`, `--endPointToRemove`, `--remove-endpoints` | Non | Endpoints à supprimer, format `method:path`, séparés par `,`. Ignoré si `-toKeep` est aussi renseigné |
| `-toKeep`, `--endPointToKeep`, `--keep-endpoints` | Non | Endpoints à conserver (tous les autres sont supprimés). Prioritaire sur `-toRm` si les deux sont fournis |
| `-m`, `--mergeSwagger`, `--merge` | Non | Fusionne un swagger décomposé (multi-fichiers `$ref`) en un seul fichier — contraire de `-d` |
| `-d`, `--decomposeSwagger`, `--decompose` | Non | Décompose le swagger en plusieurs fichiers — contraire de `-m` |
| `-pf`, `--persistFile`, `--persist` | Non | Persiste le résultat dans des fichiers |

> `-toRm` et `-toKeep` sont tous les deux optionnels. Si aucune transformation n'est fournie, le swagger est seulement affiché.
> Si les deux sont fournis, `-toKeep` est prioritaire et `-toRm` est ignoré.

### Ordre d'exécution

```
merge → affichage des endpoints → keep → remove → decompose → persist
```

### Exemples

```bash
# Afficher les endpoints uniquement (sans filtre ni persistance)
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar \
  -sf src/main/resources/swagger-cobaye.yml

# Rassembler le swagger en un seul fichier (sans filtre)
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar -sf src/main/resources/swagger-cobaye.yml -pf

# Fusionner un swagger décomposé (multi-fichiers $ref) en un seul fichier
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar -sf src/main/resources/q1-api-v2/q1-api.yml -m -pf

# Supprimer des endpoints et persister le résultat
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar -sf src/main/resources/swagger-cobaye.yml -toRm post:/profiling,get:/profilings -pf

# Conserver uniquement certains endpoints et décomposer le résultat
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar -sf src/main/resources/swagger-cobaye.yml -toKeep post:/profiling,get:/profilings -d -pf


# Fusionner puis filtrer
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar -sf src/main/resources/q1-api-v2/q1-api.yml -m -toRm delete:/profiling -pf

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

# Port personnalisé avec une option explicite
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar server --port 9090
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
| `POST` | `/merge` | Fusionne un swagger décomposé (ZIP) en un seul fichier — contraire de `/decompose` |
| `GET` | `/swagger-ui` | Interface graphique Swagger UI |
| `GET` | `/health` | Vérifie la disponibilité du serveur |

#### Paramètres communs (query string)

- `extension` *(obligatoire)* — format de sortie : `json`, `yml` ou `yaml`
- `endpoints` *(obligatoire pour `/clear-endpoints` et `/keep-endpoints`)* — liste séparée par des virgules, format `method:path`

#### Corps de la requête

Le fichier Swagger peut être envoyé :
- En **multipart/form-data** avec le champ `file` (recommandé)
- En **corps brut** (`application/octet-stream`)

### Erreurs

Les erreurs REST sont retournées en JSON avec une forme commune :

```json
{
  "code": "INVALID_REQUEST",
  "message": "Paramètre 'extension' manquant (json, yml, yaml)."
}
```

Les codes principaux sont `INVALID_REQUEST` (400), `METHOD_NOT_ALLOWED` (405) et
`INTERNAL_ERROR` (500). Les codes HTTP des routes existantes restent inchangés.

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

# Fusionner (opération inverse de decompose)
curl -X POST \
  "http://localhost:8080/merge?extension=yml" \
  -F "file=@swagger-decomposed.zip" --output swagger-merged.zip
```

---

## État de qualité

Les premiers travaux de fiabilisation sont couverts par des tests dédiés :

- navigation explicite vers un endpoint absent avec exception métier ;
- parsing `method:path` avec validation et conservation des `:` présents dans le chemin ;
- références `$ref` invalides ou déjà externalisées ;
- extensions de fichiers sans sensibilité à la casse ;
- persistance de plusieurs fichiers sans suppression du contenu précédent ;
- lecture multipart/raw, génération ZIP et fusion de Swagger ;
- validation des options CLI et du port REST.

Les sujets restant à traiter après stabilisation de la suite de tests sont la stratégie d'immutabilité de `SwaggerNode`, la préservation des sections de composants lors de la décomposition et l'harmonisation finale des formats d'erreur CLI/REST.
