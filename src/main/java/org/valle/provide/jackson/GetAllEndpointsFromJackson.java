package org.valle.provide.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.EndPoint;
import org.valle.provide.GetAllEndpoints;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Slf4j
@AllArgsConstructor
public class GetAllEndpointsFromJackson implements GetAllEndpoints {

    private final JacksonUtils jacksonUtils;

    @Override
    public Set<EndPoint> provide() {
        // Lecture des endpoints du swagger
        JsonNode openApi = this.jacksonUtils.readValue();
        JsonNode paths = openApi.get("paths");

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
