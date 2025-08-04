package org.valle.process;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistResult;
import org.valle.process.models.DollarRef;
import org.valle.process.models.EndPoint;
import org.valle.provide.GetAllComponents;
import org.valle.provide.GetAllEndpoints;
import org.valle.provide.GetAllPaths;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
public class ClearEndpointOnDemandImpl implements ClearEndpointOnDemand {

    private final GetAllEndpoints getAllEndpoints;
    private final GetAllPaths getAllPaths;
    private final GetAllComponents getAllComponents;
    private final PersistResult<Map<String, Object>> persistResult;

    @Override
    public void execute(Set<EndPoint> toBeCleared) {

        JacksonUtils jacksonUtils = new JacksonUtils(new File("src/main/resources/swagger-cobaye.yml"));

        Map<String, Object> rawValue = jacksonUtils.readRawValue();

        Set<String> schemasToRemove = getSchemaNamesToBeRemoved(
                this.getAllEndpoints.provide(),
                toBeCleared,
                this.getAllComponents.provide()
        );

        Map<String, Object> toWrite = removeElementsByName(toBeCleared, schemasToRemove, rawValue);

        persistResult.persist(toWrite);
    }

    public static Map<String, Object> removeElementsByName(
            Set<EndPoint> endPointsToRemove,
            Set<String> schemasToRemove,
            Map<String, Object> objectsRaw
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
        Iterator<Map.Entry<String, Object>> componentIterator = components.entrySet().iterator();

        while (componentIterator.hasNext()) {
            Map.Entry<String, Object> componentEntry = componentIterator.next();
            Map<String, Object> componentValue = (Map<String, Object>) componentEntry.getValue();
            schemasToRemove.forEach(componentValue::remove);
        }

//        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
//        schemasToRemove.forEach(schemas::remove);
        return objectsRaw;
    }

    public static Set<String> getSchemaNamesToBeRemoved(
            Set<EndPoint> allEndPoints,
            Set<EndPoint> endPointsToBeRemoved,
            JsonNode allComponents
    ) {

        Set<EndPoint> endPointsToKeep = new HashSet<>(allEndPoints);
        endPointsToKeep.removeAll(endPointsToBeRemoved);

        Set<String> schemasToKeep = endPointsToKeep.stream()
                .flatMap(endPoint -> getAllNamedReferencesOfAPath(endPoint, allComponents).stream())
                .collect(Collectors.toSet());

        Set<String> schemasToRemove = endPointsToBeRemoved.stream()
                .flatMap(endPoint -> getAllNamedReferencesOfAPath(endPoint, allComponents).stream())
                .collect(Collectors.toSet());

        schemasToRemove.removeAll(schemasToKeep);

        log.debug("All endPoints size: {}", allEndPoints.size());
        log.info("EndPoints size to keep: {}", endPointsToKeep.size());
        log.info("EndPoints size to be removed: {}", endPointsToBeRemoved.size());
        log.info("allComponents size: {}", allComponents.size());
        log.info("Schemas size to keep: {}", schemasToKeep.size());
        log.info("Schemas size to be removed: {}", schemasToRemove.size());

        log.debug("All endPoints: {}", allEndPoints);
        log.debug("EndPoints to keep: {}", endPointsToKeep);
        log.debug("EndPoints to be removed: {}", endPointsToBeRemoved);
        log.debug("allComponents: {}", allComponents);
        log.debug("Schemas to keep: {}", schemasToKeep);
        log.debug("Schemas to be removed: {}", schemasToRemove);

        return schemasToRemove;
    }

    public static Set<String> getAllNamedReferencesOfAPath(EndPoint endPoint, JsonNode allComponents) {
        JsonNode selectedPath = allComponents.get("paths").get(endPoint.path()).get(endPoint.method());
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
