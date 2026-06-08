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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Builder(toBuilder = true)
public record SwaggerNode(
        @NotNull @Valid JsonNode node,
        @NotNull @Valid Extension extension
) {

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
        JsonNode selectedPath;
        try {
            selectedPath = this.node()
                    .get("paths")
                    .get(endPoint.path())
                    .get(endPoint.method());
        } catch (NullPointerException e) {
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
                if (field.getKey().equals("$ref") && field.getValue().isTextual()) {
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
        ((ObjectNode) this.node()).remove("components");
        return this;
    }

    public SwaggerNode changePathReferences() {
        JsonNode paths = this.node().get("paths");
        paths.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            String withoutFirstSlash = key.startsWith("/") ? key.substring(1) : key;
            String ref = withoutFirstSlash
                    .replace("/", "-")
                    .replace("{", "")
                    .replace("}", "");
            ObjectNode node = new ObjectMapper().createObjectNode();
            node.put("$ref", "paths/%s.%s".formatted(ref, this.extension().toString().toLowerCase()));
            entry.setValue(node);
        });
        return this;
    }

    public SwaggerNode addComponentFileReferences() {
        if (this.node().isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = this.node().fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                // condition d'ajout dans la liste des références
                if (field.getKey().equals("$ref") && field.getValue().isTextual()) {
                    DollarRef dollarRef = new DollarRef(field.getValue().asText());
                    String refValue = dollarRef.getComponentFileReference() + ".%s".formatted(this.extension().toString().toLowerCase());
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
                                String refValue = dollarRef.getComponentFileReference() + ".%s".formatted(this.extension().toString().toLowerCase());
                                entry.setValue(new TextNode(refValue));
                            }
                        }
                    }
                } else {
                    this.toBuilder().node(field.getValue()).build().addComponentFileReferences();
                }
            }
        } else if (this.node().isArray()) {
            for (JsonNode item : this.node()) {
                this.toBuilder().node(item).build().addComponentFileReferences();
            }
        }
        return this;
    }

    public SwaggerNode addPathFileReferences() {
        JsonNode paths = this.node().get("paths");
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
        ObjectNode paths = new ObjectMapper().createObjectNode();
        this.node().get("paths").fields().forEachRemaining(entry -> {
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
        // Extraction des components/schemas
        ObjectNode components = new ObjectMapper().createObjectNode();
        if (node().has("components")) {
            node().get("components").fields().forEachRemaining(entry -> {
                entry.getValue().fields().forEachRemaining(field -> {
                    components.putIfAbsent(field.getKey(), field.getValue());
                });
            });
        }
        return this.toBuilder().node(components).build();
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
            components.fields().forEachRemaining(entry -> {
                ((ObjectNode) entry.getValue()).remove(schemaToRm);
            });
        });

        return this;
    }

    public Set<EndPoint> getAllEndpoints() {
        // Lecture des endpoints du swagger
        JsonNode paths = this.node().get("paths");

        Iterator<Map.Entry<String, JsonNode>> pathsFields = paths.fields();
        Set<EndPoint> endpoints = new HashSet<>();

        while (pathsFields.hasNext()) {
            Map.Entry<String, JsonNode> pField = pathsFields.next();
            String path = pField.getKey();
            JsonNode methods = pField.getValue();
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