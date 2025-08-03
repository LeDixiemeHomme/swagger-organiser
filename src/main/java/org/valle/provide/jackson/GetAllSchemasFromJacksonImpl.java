package org.valle.provide.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.valle.provide.GetAllSchemas;

import java.io.File;

@Slf4j
public class GetAllSchemasFromJacksonImpl implements GetAllSchemas {

    private final JacksonUtils jacksonUtils;

    public GetAllSchemasFromJacksonImpl(String swaggerFilePath) {
        this.jacksonUtils = new JacksonUtils(new File(swaggerFilePath));
    }

    @Override
    public JsonNode provide() {
        // Lecture des schémas du swagger
        JsonNode openApi = this.jacksonUtils.readValue();
        JsonNode components = openApi.get("components");
        return components.get("schemas");
    }
}
