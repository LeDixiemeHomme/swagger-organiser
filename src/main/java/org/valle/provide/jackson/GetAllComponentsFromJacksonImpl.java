package org.valle.provide.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.valle.provide.GetAllComponents;

import java.io.File;

@Slf4j
public class GetAllComponentsFromJacksonImpl implements GetAllComponents {

    private final JacksonUtils jacksonUtils;

    public GetAllComponentsFromJacksonImpl(String swaggerFilePath) {
        this.jacksonUtils = new JacksonUtils(new File(swaggerFilePath));
    }

    @Override
    public JsonNode provide() {
        JsonNode openApi = this.jacksonUtils.readValue();
        return openApi.get("components");
    }
}
