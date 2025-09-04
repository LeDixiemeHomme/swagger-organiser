package org.valle.persist.jackson;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistResult;
import org.valle.utils.JacksonUtils;

@Slf4j
@AllArgsConstructor
public class PersistResultNodeImpl implements PersistResult<ObjectNode> {

    private final JacksonUtils jacksonUtils;

    @Override
    public void persist(ObjectNode toPersist) {
        this.jacksonUtils.writeValue(toPersist);
    }
}
