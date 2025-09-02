package org.valle.persist;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.provide.jackson.JacksonUtils;

@Slf4j
@AllArgsConstructor
public class PersistResultNodeImpl implements PersistResult<ObjectNode> {

    private final JacksonUtils jacksonUtils;

    @Override
    public void persist(ObjectNode toPersist) {
        this.jacksonUtils.writeValue(toPersist);
    }
}
