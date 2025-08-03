package org.valle.provide.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import org.valle.provide.GetAllPaths;

import java.io.File;

public class GetAllPathsFromJacksonImpl implements GetAllPaths {

    private final JacksonUtils jacksonUtils;

    public GetAllPathsFromJacksonImpl(String swaggerFilePath) {
        this.jacksonUtils = new JacksonUtils(new File(swaggerFilePath));
    }

    @Override
    public JsonNode provide() {
        JsonNode openApi = this.jacksonUtils.readValue();
        return openApi.get("paths");
    }
}
