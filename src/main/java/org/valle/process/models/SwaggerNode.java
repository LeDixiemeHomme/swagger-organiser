package org.valle.process.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public record SwaggerNode(
        @NotNull @Valid JsonNode node
) {

    public Set<String> getSchemaNamesToBeRemoved(
            Set<EndPoint> endPointsToBeRemoved
    ) {

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
        JsonNode selectedPath = this.node()
                .get("paths")
                .get(endPoint.path())
                .get(endPoint.method());
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
            node.put("$ref", "paths/%s.%s".formatted(ref, "yaml"));
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
                    field.setValue(new TextNode(dollarRef.getFileReference() + ".yaml"));
                } else {
                    new SwaggerNode(field.getValue()).addComponentFileReferences();
                }
            }
        } else if (this.node().isArray()) {
            for (JsonNode item : this.node()) {
                new SwaggerNode(item).addComponentFileReferences();
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
                field.setValue(new TextNode(dollarRef.getPathReference()));
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
        return new SwaggerNode(paths);
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
        return new SwaggerNode(components);
    }

    public SwaggerNode removeElementsByName(
            Set<EndPoint> endPointsToRemove,
            Set<String> schemasToRemove
    ) {
        endPointsToRemove.forEach(endPointToRm ->
                ((ObjectNode) this.node().get("paths")).remove(endPointToRm.path()));

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