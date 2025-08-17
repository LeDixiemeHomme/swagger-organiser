package org.valle.provide.jackson;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.SwaggerRawValue;
import org.valle.provide.GetSwaggerRawValue;

@Slf4j
@AllArgsConstructor
public class GetSwaggerRawValueJacksonImpl implements GetSwaggerRawValue {

    private final JacksonUtils jacksonUtils;

    @Override
    public SwaggerRawValue provide() {
        return new SwaggerRawValue(jacksonUtils.readRawValue());
    }
}
