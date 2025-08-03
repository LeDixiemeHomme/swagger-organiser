package org.valle.persist.jackson;

import org.valle.persist.PersistResult;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;
import java.util.Map;

public class PersistResultFileWithJacksonImpl implements PersistResult<Map<String, Object>> {

    private final JacksonUtils jacksonUtils;

    public PersistResultFileWithJacksonImpl(String swaggerFilePath) {
        this.jacksonUtils = new JacksonUtils(new File(swaggerFilePath));
    }

    @Override
    public void persist(Map<String, Object> toPersist) {
        this.jacksonUtils.writeRawValueToSwaggerFile(toPersist);
    }
}
