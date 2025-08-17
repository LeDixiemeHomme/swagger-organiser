package org.valle.provide.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.provide.GetAllComponents;

@Slf4j
@AllArgsConstructor
public class GetAllComponentsFromJacksonImpl implements GetAllComponents {

    private final JacksonUtils jacksonUtils;

    @Override
    public JsonNode provide() {
        JsonNode openApi = this.jacksonUtils.readValue();
        return openApi.get("components");
    }
}
