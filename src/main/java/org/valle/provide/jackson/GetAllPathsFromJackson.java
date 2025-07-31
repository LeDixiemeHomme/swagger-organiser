package org.valle.provide.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.valle.provide.GetAllPaths;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GetAllPathsFromJackson implements GetAllPaths {
    @Override
    public List<String> provide() {
        // Lecture des endpoints du swagger
        File swaggerFile = new File("src/main/resources/swagger-cobaye.yml");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> openApi;
        try {
            openApi = mapper.readValue(swaggerFile, Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> paths = (Map<String, Object>) openApi.get("paths");
        StringBuilder sb = new StringBuilder();
        sb.append("Liste des endpoints dans swagger-cobaye.yml :\n\n");
        for (Map.Entry<String, Object> entry : paths.entrySet()) {
            String path = entry.getKey();
            Map<String, Object> methods = (Map<String, Object>) entry.getValue();
            for (String method : methods.keySet()) {
                sb.append(method.toUpperCase()).append(":").append(path).append("\n");
            }
        }
        return List.of(sb.toString());
    }
}
