package org.valle.process.models;

import com.fasterxml.jackson.databind.JsonNode;
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
            Set<EndPoint> allEndPoints,
            Set<EndPoint> endPointsToBeRemoved
    ) {

        Set<EndPoint> endPointsToKeep = new HashSet<>(allEndPoints);
        endPointsToKeep.removeAll(endPointsToBeRemoved);

        Set<String> schemasToKeep = endPointsToKeep.stream()
                .flatMap(endPoint -> getAllNamedReferencesOfAPath(endPoint, this.node()).stream())
                .collect(Collectors.toSet());

        Set<String> schemasToRemove = endPointsToBeRemoved.stream()
                .flatMap(endPoint -> getAllNamedReferencesOfAPath(endPoint, this.node()).stream())
                .collect(Collectors.toSet());

        schemasToRemove.removeAll(schemasToKeep);

        return schemasToRemove;
    }

    public static Set<String> getAllNamedReferencesOfAPath(EndPoint endPoint, JsonNode allComponents) {
        JsonNode selectedPath = allComponents
                .get("paths")
                .get(endPoint.path())
                .get(endPoint.method());
        return findRefs(selectedPath, allComponents, new HashSet<>());
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
}
