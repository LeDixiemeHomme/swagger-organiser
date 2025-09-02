package org.valle.persist.jackson;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistResult;
import org.valle.provide.jackson.JacksonUtils;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class PersistResultFileWithJacksonImpl implements PersistResult<Map<String, Object>> {

    private final JacksonUtils jacksonUtils;

    @Override
    public void persist(Map<String, Object> toPersist) {
        this.jacksonUtils.writeRawValueToSwaggerFile(toPersist);
    }
}
