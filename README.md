# Swagger Organiser

Outil de gestion et d'organisation de fichiers Swagger/OpenAPI.  
Le même JAR supporte deux modes d'utilisation : **CLI** et **serveur REST**.

## Build

Le projet cible **Java 25**. Gradle force cette version pour la compilation afin
de produire des classes cohérentes avec le runtime Java 25 utilisé par le projet.

```bash
./gradlew shadowJar
```

Le JAR produit : `build/libs/swagger-organiser-2.0.0-all.jar`

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
| `--[no-]preserve-comments` | Non | Préserve les commentaires YAML dans les fichiers YAML générés (défaut : `true`) |

> `-toRm` et `-toKeep` sont tous les deux optionnels. Si aucune transformation n'est fournie, le swagger est seulement affiché.
> Si les deux sont fournis, `-toKeep` est prioritaire et `-toRm` est ignoré.

### Ordre d'exécution

```
merge → affichage des endpoints → keep → remove → decompose → persist
```

### Exemples

```bash
# Afficher les endpoints uniquement (sans filtre ni persistance)
java -jar build/libs/swagger-organiser-2.0.0-all.jar \
  -sf src/main/resources/swagger-cobaye.yml

# Rassembler le swagger en un seul fichier (sans filtre)
java -jar build/libs/swagger-organiser-2.0.0-all.jar -sf src/main/resources/swagger-cobaye.yml -pf

# Fusionner un swagger décomposé (multi-fichiers $ref) en un seul fichier
java -jar build/libs/swagger-organiser-2.0.0-all.jar -sf src/main/resources/q1-api-v2/q1-api.yml -m -pf

# Supprimer des endpoints et persister le résultat
java -jar build/libs/swagger-organiser-2.0.0-all.jar -sf src/main/resources/swagger-cobaye.yml -toRm post:/profiling,get:/profilings -pf

# Conserver uniquement certains endpoints et décomposer le résultat
java -jar build/libs/swagger-organiser-2.0.0-all.jar -sf src/main/resources/swagger-cobaye.yml -toKeep post:/profiling,get:/profilings -d -pf


# Fusionner puis filtrer
java -jar build/libs/swagger-organiser-2.0.0-all.jar -sf src/main/resources/q1-api-v2/q1-api.yml -m -toRm delete:/profiling -pf

# Aide
java -jar build/libs/swagger-organiser-2.0.0-all.jar --help
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
java -jar build/libs/swagger-organiser-2.0.0-all.jar server

# Port personnalisé
java -jar build/libs/swagger-organiser-2.0.0-all.jar server 9090

# Port personnalisé avec une option explicite
java -jar build/libs/swagger-organiser-2.0.0-all.jar server --port 9090
```

Via Gradle :
```bash
./gradlew runRest
```

### Interface web

L'interface web est embarquée dans le JAR et ne nécessite aucune installation
frontend supplémentaire. Pour la lancer depuis les sources :

```bash
# Construire le JAR exécutable
./gradlew shadowJar

# Démarrer le serveur REST sur le port par défaut
java -jar build/libs/swagger-organiser-2.0.0-all.jar server
```

Une fois le serveur démarré, ouvrir l'adresse suivante dans un navigateur :

**<http://localhost:8080/app>**

Le serveur doit rester actif dans le terminal pendant l'utilisation de
l'interface. Si un autre port est utilisé, par exemple `9090`, l'adresse devient
<http://localhost:9090/app> :

```bash
java -jar build/libs/swagger-organiser-2.0.0-all.jar server --port 9090
```

L'interface permet de :

- sélectionner un fichier Swagger/OpenAPI (`.json`, `.yaml` ou `.yml`) ou une archive
  décomposée (`.zip`) ;
- choisir le format de sortie `JSON`, `YAML (.yaml)` ou `YAML (.yml)` ;
- activer/désactiver la préservation des commentaires YAML (case cochée par défaut) ;
- supprimer des endpoints avec l'action **Clear endpoints** ;
- conserver uniquement certains endpoints avec l'action **Keep endpoints** ;
- décomposer un fichier OpenAPI avec l'action **Decompose** ;
- fusionner une archive décomposée avec l'action **Merge** ;
- saisir les endpoints au format `method:/path`, séparés par des virgules, pour
  les actions **Clear endpoints** et **Keep endpoints** ;
- télécharger automatiquement le ZIP généré après une transformation ;
- consulter la documentation interactive via le bouton **Ouvrir Swagger UI Documentation**.

Les actions de filtrage nécessitent au moins un endpoint, par exemple :
`get:/users,post:/users`. Les actions **Decompose** et **Merge** n'utilisent pas
ce champ. Les erreurs retournées par l'API sont affichées directement dans la
page.

Les entrées REST sont protégées par des limites configurables pour éviter les consommations
mémoire ou disque non bornées :

| Propriété JVM | Valeur par défaut |
|---|---:|
| `swagger.organiser.rest.max-request-bytes` | 10 MiB |
| `swagger.organiser.rest.max-zip-entries` | 1 000 |
| `swagger.organiser.rest.max-zip-entry-bytes` | 10 MiB |
| `swagger.organiser.rest.max-zip-total-bytes` | 100 MiB |

Exemple :
```bash
java -Dswagger.organiser.rest.max-request-bytes=5242880 \
  -jar build/libs/swagger-organiser-2.0.0-all.jar server
```

### Endpoints disponibles

| Méthode | Chemin | Description |
|---|---|---|
| `POST` | `/clear-endpoints` | Supprime des endpoints du swagger fourni |
| `POST` | `/keep-endpoints` | Conserve uniquement les endpoints fournis |
| `POST` | `/decompose` | Décompose le swagger en une archive ZIP |
| `POST` | `/merge` | Fusionne un swagger décomposé (ZIP) en un seul fichier — contraire de `/decompose` |
| `GET` | `/app` | Interface web vanilla embarquée (upload, transformation et téléchargement ZIP) |
| `GET` | `/swagger-ui` | Interface graphique Swagger UI |

#### Paramètres communs (query string)

- `extension` *(obligatoire)* — format de sortie : `json`, `yml` ou `yaml`
- `endpoints` *(obligatoire pour `/clear-endpoints` et `/keep-endpoints`)* — liste séparée par des virgules, format `method:path`
- `preserve-comments` *(optionnel, défaut `true`)* — `true` pour conserver les commentaires YAML dans le YAML généré, `false` pour les omettre (sans effet en JSON)
- `archive-name` *(optionnel)* — nom de base explicite de l'archive téléchargée, sans extension. Si ce paramètre est absent ou vide, la valeur de `info.title` du Swagger fourni est utilisée (par exemple `DECISEO-API`) ; à défaut, le nom historique de l'opération est conservé. Le suffixe de l'opération est ajouté automatiquement (`-decomposed`, `-merged`, `-cleared` ou `-kept`).

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

Lorsqu'un fichier YAML contient des commentaires commençant par `#`, ceux-ci sont
conservés lors de sa transformation et réémis dans le document YAML principal
généré par défaut. Ce comportement se pilote explicitement :

- **CLI** : `--preserve-comments` (par défaut) ou `--no-preserve-comments`
- **REST/Web** : query parameters `preserve-comments=true|false` (par défaut `true`) et `archive-name=nom-sans-extension`

Les commentaires JSON ne sont pas concernés, car JSON ne définit pas de syntaxe
de commentaire standard.

### Exemples curl

```bash
# Supprimer un endpoint
curl -X POST \
  "http://localhost:8080/clear-endpoints?extension=yml&endpoints=get:/profiling/%7Bprofiling_id%7D&preserve-comments=true" \
  -F "file=@swagger.yml" --output swagger-cleared.zip

# Conserver des endpoints
curl -X POST \
  "http://localhost:8080/keep-endpoints?extension=yml&endpoints=get:/profiling/%7Bprofiling_id%7D&preserve-comments=false" \
  -F "file=@swagger.yml" --output swagger-kept.zip

# Décomposer
curl -X POST \
  "http://localhost:8080/decompose?extension=yml&preserve-comments=true" \
  -F "file=@swagger.yml" --output swagger-decomposed.zip

# Fusionner (opération inverse de decompose)
curl -X POST \
  "http://localhost:8080/merge?extension=yml&preserve-comments=true" \
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

La décomposition conserve les catégories OpenAPI des composants via le sidecar
`component-categories.json`. Lors d'une fusion, ce sidecar est optionnel : les archives
historiques sans métadonnées restent interprétées avec la convention `components.schemas`.
