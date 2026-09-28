package org.valle.process.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.valle.process.exceptions.EndPointNotFoundException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Builder(toBuilder = true)
public record SwaggerNode(
        @NotNull @Valid JsonNode node,
        @NotNull @Valid Extension extension,
        String comments
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SwaggerNode {
        comments = comments == null ? "" : comments;
    }

    public Set<String> getSchemaNamesToBeRemoved(Set<EndPoint> endPointsToBeRemoved) {

        Set<EndPoint> endPointsToKeep = new HashSet<>(this.getAllEndpoints());
        endPointsToKeep.removeAll(endPointsToBeRemoved);

        Set<String> schemasToKeep = endPointsToKeep.stream()
                .flatMap(endPoint -> this.getAllNamedReferencesOfAPath(endPoint).stream())
                .collect(Collectors.toSet());

        Set<String> schemasToRemove = endPointsToBeRemoved.stream()
                .flatMap(endPoint -> this.getAllNamedReferencesOfAPath(endPoint).stream())
                .collect(Collectors.toSet());

        schemasToRemove.removeAll(schemasToKeep);

        return schemasToRemove;
    }

    public Set<String> getAllNamedReferencesOfAPath(EndPoint endPoint) {
        JsonNode paths = this.node().get("paths");
        JsonNode path = paths == null ? null : paths.get(endPoint.path());
        JsonNode selectedPath = path == null ? null : path.get(endPoint.method());
        if (selectedPath == null || selectedPath.isMissingNode()) {
            throw new EndPointNotFoundException(endPoint, this);
        }
        return findRefs(selectedPath, this.node(), new HashSet<>());
    }

    public static Set<String> findRefs(JsonNode node, JsonNode allComponents, Set<String> visited) {
        Set<String> refs = new HashSet<>();

        // si le noeud est null, pas de traitement
        if (isNull(node)) return refs;

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                // condition d'ajout dans la liste des références
                if (field.getKey().equals("$ref")
                        && field.getValue().isTextual()
                        && field.getValue().asText().startsWith("#")) {
                    DollarRef dollarRef = new DollarRef(field.getValue().asText());
                    String referencedName = dollarRef.getReferencedName();
                    if (!visited.contains(referencedName)) {
                        // pointe de la méthode récursive
                        refs.add(referencedName);
                        visited.add(referencedName);
                        // appel récursif pour trouver les références dans le noeud référencé
                        refs.addAll(findRefs(dollarRef.getReferencedNode(allComponents), allComponents, visited));
                    }
                } else {
                    refs.addAll(findRefs(field.getValue(), allComponents, visited));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                refs.addAll(findRefs(item, allComponents, visited));
            }
        }

        return refs;
    }

    public SwaggerNode removeComponents() {
        if (this.node() instanceof ObjectNode objectNode) {
            objectNode.remove("components");
        }
        return this;
    }

    public SwaggerNode changePathReferences() {
        JsonNode paths = this.node().get("paths");
        if (paths == null || !paths.isObject()) {
            return this;
        }
        paths.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            String withoutFirstSlash = key.startsWith("/") ? key.substring(1) : key;
            String ref = withoutFirstSlash
                    .replace("/", "-")
                    .replace("{", "")
                    .replace("}", "");
            ObjectNode node = MAPPER.createObjectNode();
            node.put("$ref", "paths/%s.%s".formatted(ref, this.extension().toString().toLowerCase()));
            entry.setValue(node);
        });
        return this;
    }

    public SwaggerNode addComponentFileReferences() {
        rewriteComponentReferences(this.node());
        return this;
    }

    private void rewriteComponentReferences(JsonNode currentNode) {
        if (currentNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = currentNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                // condition d'ajout dans la liste des références
                if (field.getKey().equals("$ref")
                        && field.getValue().isTextual()
                        && field.getValue().asText().startsWith("#")) {
                    DollarRef dollarRef = new DollarRef(field.getValue().asText());
                    String refValue = dollarRef.getComponentFileReference() + ".%s"
                            .formatted(this.extension().toString().toLowerCase());
                    field.setValue(new TextNode(refValue));
                } else if (field.getKey().equals("mapping") && field.getValue().isObject()) {
                    // Les valeurs de discriminator.mapping sont des références vers des schémas
                    // (ex: '#/components/schemas/Foo') qui ne sont pas des champs $ref mais doivent
                    // également pointer vers les fichiers décomposés.
                    Iterator<Map.Entry<String, JsonNode>> mappingEntries = field.getValue().fields();
                    while (mappingEntries.hasNext()) {
                        Map.Entry<String, JsonNode> entry = mappingEntries.next();
                        if (entry.getValue().isTextual()) {
                            String mappingValue = entry.getValue().asText();
                            if (mappingValue.startsWith("#/components/schemas/")) {
                                DollarRef dollarRef = new DollarRef(mappingValue);
                                String refValue = dollarRef.getComponentFileReference() + ".%s"
                                        .formatted(this.extension().toString().toLowerCase());
                                entry.setValue(new TextNode(refValue));
                            }
                        }
                    }
                } else {
                    rewriteComponentReferences(field.getValue());
                }
            }
        } else if (currentNode.isArray()) {
            for (JsonNode item : currentNode) {
                rewriteComponentReferences(item);
            }
        }
    }

    public SwaggerNode addPathFileReferences() {
        JsonNode paths = this.node().get("paths");
        if (paths == null || !paths.isObject()) {
            return this;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = paths.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            // condition d'ajout dans la liste des références
            if (field.getKey().equals("$ref") && field.getValue().isTextual()) {
                DollarRef dollarRef = new DollarRef(field.getValue().asText());
                field.setValue(new TextNode(dollarRef.getPathFileReference()));
            }
        }
        return this;
    }

    public SwaggerNode decomposePaths() {
        // Extraction des paths
        ObjectNode paths = MAPPER.createObjectNode();
        JsonNode sourcePaths = this.node().get("paths");
        if (sourcePaths == null || !sourcePaths.isObject()) {
            return this.toBuilder().node(paths).build();
        }
        sourcePaths.fields().forEachRemaining(entry -> {
            // Chaque endpoint dans un fichier séparé (ici une map)
            String key = entry.getKey();
            String withoutFirstSlash = key.startsWith("/") ? key.substring(1) : key;
            String ref = withoutFirstSlash
                    .replace("/", "-")
                    .replace("{", "")
                    .replace("}", "");
            paths.putIfAbsent(ref, entry.getValue());
        });
        return this.toBuilder().node(paths).build();
    }

    public SwaggerNode decomposeComponent() {
        ObjectNode components = MAPPER.createObjectNode();
        JsonNode sourceComponents = node().get("components");
        if (sourceComponents == null || sourceComponents.isNull()) {
            return this.toBuilder().node(components).build();
        }
        getComponentCategories();
        sourceComponents.fields().forEachRemaining(entry -> {
            entry.getValue().fields().forEachRemaining(field ->
                    components.putIfAbsent(field.getKey(), field.getValue()));
        });
        return this.toBuilder().node(components).build();
    }

    /**
     * Returns the category of every component in this document.  Decomposed files retain
     * their historical flat names, so this map is persisted as sidecar metadata.
     */
    public Map<String, String> getComponentCategories() {
        Map<String, String> categories = new LinkedHashMap<>();
        JsonNode sourceComponents = node().get("components");
        if (sourceComponents == null || sourceComponents.isNull()) {
            return categories;
        }
        if (!sourceComponents.isObject()) {
            throw new IllegalArgumentException("Swagger invalide : components doit être un objet.");
        }
        sourceComponents.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isObject()) {
                throw new IllegalArgumentException(
                        "Swagger invalide : la catégorie de composant '%s' doit être un objet."
                                .formatted(entry.getKey()));
            }
            entry.getValue().fieldNames().forEachRemaining(componentName -> {
                String previousCategory = categories.putIfAbsent(componentName, entry.getKey());
                if (previousCategory != null && !previousCategory.equals(entry.getKey())) {
                    throw new IllegalStateException(
                            "Duplicate component name '%s' in categories '%s' and '%s'."
                                    .formatted(componentName, previousCategory, entry.getKey()));
                }
            });
        });
        return categories;
    }

    /**
     * Ajoute au nœud principal ({@code main}) une section {@code components} destinée à la
     * génération de code (ex : openapi-generator). Chaque schéma et chaque security scheme
     * de l'original est remplacé par un simple {@code $ref} vers le fichier décomposé
     * correspondant ({@code ./components/<Nom>.<ext>}).
     *
     * <p>Exemple de résultat pour un schéma {@code Foo} en extension {@code yml} :
     * <pre>
     * components:
     *   schemas:
     *     Foo:
     *       $ref: "./components/Foo.yml"
     * </pre>
     *
     * @param originalComponents le nœud {@code components} du swagger original
     *                           (avant toute transformation), peut être {@code null}
     * @param extension          l'extension de fichier cible (yml, yaml, json)
     * @return {@code this} pour chaînage
     */
    public SwaggerNode addCodeGenerationComponents(JsonNode originalComponents, Extension extension) {
        if (originalComponents == null || !originalComponents.isObject()) {
            return this;
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode codeGenComponents = mapper.createObjectNode();

        originalComponents.fields().forEachRemaining(sectionEntry -> {
            // sectionEntry.getKey() = "schemas", "securitySchemes", etc.
            JsonNode sectionContent = sectionEntry.getValue();
            if (sectionContent.isObject()) {
                ObjectNode sectionNode = mapper.createObjectNode();
                sectionContent.fields().forEachRemaining(schemaEntry -> {
                    ObjectNode ref = mapper.createObjectNode();
                    ref.put("$ref", "./components/%s.%s".formatted(
                            schemaEntry.getKey(), extension.toString().toLowerCase()));
                    sectionNode.set(schemaEntry.getKey(), ref);
                });
                codeGenComponents.set(sectionEntry.getKey(), sectionNode);
            }
        });

        ((ObjectNode) this.node()).set("components", codeGenComponents);
        return this;
    }

    public SwaggerNode removeElementsByName(
            Set<EndPoint> endPointsToRemove,
            Set<String> schemasToRemove
    ) {
        JsonNode paths = this.node().get("paths");
        if (paths instanceof ObjectNode pathsObjectNode) {
            endPointsToRemove.forEach(endPointToRm -> {
                JsonNode pathNode = pathsObjectNode.get(endPointToRm.path());
                if (pathNode instanceof ObjectNode pathObjectNode) {
                    pathObjectNode.remove(endPointToRm.method());
                    if (pathObjectNode.isEmpty()) {
                        pathsObjectNode.remove(endPointToRm.path());
                    }
                }
            });
        }

        schemasToRemove.forEach(schemaToRm -> {
            JsonNode components = this.node().get("components");
            if (components != null && components.isObject()) {
                components.fields().forEachRemaining(entry -> {
                    if (entry.getValue() instanceof ObjectNode componentCategory) {
                        componentCategory.remove(schemaToRm);
                    }
                });
            }
        });

        return this;
    }

    public Set<EndPoint> getAllEndpoints() {
        // Lecture des endpoints du swagger
        JsonNode paths = this.node().get("paths");
        if (paths == null || paths.isNull()) {
            return new HashSet<>();
        }
        if (!paths.isObject()) {
            throw new IllegalArgumentException("Swagger invalide : paths doit être un objet.");
        }

        Iterator<Map.Entry<String, JsonNode>> pathsFields = paths.fields();
        Set<EndPoint> endpoints = new HashSet<>();

        while (pathsFields.hasNext()) {
            Map.Entry<String, JsonNode> pField = pathsFields.next();
            String path = pField.getKey();
            JsonNode methods = pField.getValue();
            if (!methods.isObject()) {
                throw new IllegalArgumentException(
                        "Swagger invalide : la définition du path '%s' doit être un objet."
                                .formatted(path));
            }
            Iterator<Map.Entry<String, JsonNode>> methodsFields = methods.fields();
            while (methodsFields.hasNext()) {
                Map.Entry<String, JsonNode> mField = methodsFields.next();
                endpoints.add(
                        EndPoint.builder()
                                .method(mField.getKey())
                                .path(path)
                                .build()
                );
            }
        }
        return endpoints;
    }
}