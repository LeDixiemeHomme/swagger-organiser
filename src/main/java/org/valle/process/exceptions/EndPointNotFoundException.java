package org.valle.process.exceptions;

import lombok.Builder;
import lombok.Getter;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;

@Getter
@Builder
public class EndPointNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Endpoint %s non trouvé dans la node suivant : %s";

    private final EndPoint endPoint;

    private final SwaggerNode swaggerNode;

    public EndPointNotFoundException(EndPoint endPoint, SwaggerNode swaggerNode) {
        super(MESSAGE.formatted(endPoint, swaggerNode.node()));
        this.endPoint = endPoint;
        this.swaggerNode = swaggerNode;
    }
}