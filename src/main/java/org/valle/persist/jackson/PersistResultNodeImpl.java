package org.valle.persist.jackson;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistResult;
import org.valle.process.models.SwaggerNode;

import java.io.File;

import static org.valle.utils.JacksonUtils.writeValue;

@Slf4j
@AllArgsConstructor
public class PersistResultNodeImpl implements PersistResult<SwaggerNode> {

    private final File swaggerFile;

    @Override
    public void persist(SwaggerNode toPersist) {
        writeValue(swaggerFile, toPersist);
    }

    public void persist(ObjectNode toPersist) {
        writeValue(swaggerFile, toPersist);
    }
}
