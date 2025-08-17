package org.valle.provide.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.provide.GetAllSchemas;

@Slf4j
@AllArgsConstructor
public class GetAllSchemasFromJacksonImpl implements GetAllSchemas {

    private final JacksonUtils jacksonUtils;

    @Override
    public JsonNode provide() {
        JsonNode openApi = this.jacksonUtils.readValue();
        JsonNode components = openApi.get("components");
        return components.get("schemas");
    }
}
