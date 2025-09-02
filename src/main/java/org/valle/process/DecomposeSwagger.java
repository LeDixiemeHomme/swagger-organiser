package org.valle.process;

import org.valle.process.models.SwaggerNode;

import java.util.Map;

public interface DecomposeSwagger {
    Map<String, SwaggerNode> execute();
}
