package org.valle.process.models;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Getter
@AllArgsConstructor
public class OpenApiObjects {

    private final JsonNode objects;
    private final Map<String, Object> objectsRaw;

    public JsonNode getPaths() {
        return this.objects.get("paths");
    }

    public JsonNode getSchemas() {
        JsonNode components = this.objects.get("components");
        return components.get("schemas");
    }

    public Map<String, Object> removeElementsByName(
            Set<EndPoint> endPointsToRemove,
            Set<String> schemasToRemove
    ) {
        Map<String, Object> paths = (Map<String, Object>) objectsRaw.get("paths");
        for (EndPoint endPoint : endPointsToRemove) {
            Iterator<Map.Entry<String, Object>> pathIterator = paths.entrySet().iterator();
            while (pathIterator.hasNext()) {
                Map.Entry<String, Object> pathEntry = pathIterator.next();
                if (pathEntry.getKey().equals(endPoint.path())) {
                    Map<String, Object> methods = (Map<String, Object>) pathEntry.getValue();
                    Iterator<Map.Entry<String, Object>> methodIterator = methods.entrySet().iterator();
                    while (methodIterator.hasNext()) {
                        Map.Entry<String, Object> methodEntry = methodIterator.next();
                        if (methodEntry.getKey().equals(endPoint.method())) {
                            methodIterator.remove();
                            log.info("Removing endpoint: {} {}", endPoint.method(), endPoint.path());
                        }
                    }
                    if (methods.isEmpty()) {
                        pathIterator.remove();
                    }
                }
            }
        }
        Map<String, Object> components = (Map<String, Object>) objectsRaw.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        schemasToRemove.forEach(schemas::remove);
        return this.objectsRaw;
    }

    public Set<String> getSchemaNamesToBeRemoved(
            Set<EndPoint> allEndPoints,
            Set<EndPoint> endPointsToBeRemoved
    ) {

        Set<String> allSchemas = allEndPoints.stream()
                .flatMap(endPoint -> this.getAllNamedReferencesOfAPath(endPoint).stream())
                .collect(Collectors.toSet());

        Set<EndPoint> endPointsToKeep = new HashSet<>(allEndPoints);
        endPointsToKeep.removeAll(endPointsToBeRemoved);

        Set<String> schemasToKeep = endPointsToKeep.stream()
                .flatMap(endPoint -> this.getAllNamedReferencesOfAPath(endPoint).stream())
                .collect(Collectors.toSet());

        Set<String> schemasToRemove = endPointsToBeRemoved.stream()
                .flatMap(endPoint -> this.getAllNamedReferencesOfAPath(endPoint).stream())
                .collect(Collectors.toSet());

        schemasToRemove.removeAll(schemasToKeep);

        log.debug("All endPoints size: {}", allEndPoints.size());
        log.info("EndPoints size to keep: {}", endPointsToKeep.size());
        log.info("EndPoints size to be removed: {}", endPointsToBeRemoved.size());
        log.info("allSchemas size: {}", allSchemas.size());
        log.info("Schemas size to keep: {}", schemasToKeep.size());
        log.info("Schemas size to be removed: {}", schemasToRemove.size());

        log.debug("All endPoints: {}", allEndPoints);
        log.debug("EndPoints to keep: {}", endPointsToKeep);
        log.debug("EndPoints to be removed: {}", endPointsToBeRemoved);
        log.debug("allSchemas: {}", allSchemas);
        log.debug("Schemas to keep: {}", schemasToKeep);
        log.debug("Schemas to be removed: {}", schemasToRemove);

        return schemasToRemove;
    }

    public Set<String> getAllNamedReferencesOfAPath(EndPoint endPoint) {
        JsonNode selectedPath = this.getPaths().get(endPoint.path()).get(endPoint.method());
        Set<String> stringSet = this.findRefs(selectedPath);
        return stringSet;
    }

    public Set<String> findRefs(JsonNode node) {
        Set<String> refs = new HashSet<>();

        // si le noeud est null, pas de traitement
        if (isNull(node)) return refs;

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                // condition d'ajout dans la liste des références
                if (field.getKey().equals("$ref") && field.getValue().isTextual()) {
                    String refName = new DollarRef(field.getValue().asText()).getName();
                    refs.addAll(findRefs(this.getSchemas().get(refName)));
                    // pointe de la méthode récursive
                    refs.add(refName);
                } else {
                    refs.addAll(findRefs(field.getValue()));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                refs.addAll(findRefs(item));
            }
        }

        return refs;
    }
}
