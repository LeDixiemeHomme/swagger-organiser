package org.valle.process.models;

import lombok.Builder;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Builder(toBuilder = true)
public record SwaggerRawValue(
        Map<String, Object> rawValue,
        Extension extension
) {

    public Map<String, Object> removeElementsByName(
            Set<EndPoint> endPointsToRemove,
            Set<String> schemasToRemove
    ) {
        Map<String, Object> paths = (Map<String, Object>) this.rawValue().get("paths");
        for (EndPoint endPoint : endPointsToRemove) {
            Iterator<Map.Entry<String, Object>> pathIterator = paths.entrySet().iterator();
            while (pathIterator.hasNext()) {
                Map.Entry<String, Object> pathEntry = pathIterator.next();
                if (pathEntry.getKey().equals(endPoint.path())) {
                    Map<String, Object> methods = (Map<String, Object>) pathEntry.getValue();
                    Iterator<Map.Entry<String, Object>> methodIterator = methods.entrySet().iterator();
                    while (methodIterator.hasNext()) {
                        Map.Entry<String, Object> methodEntry = methodIterator.next();
                        if (methodEntry.getKey().equals(endPoint.method())) {
                            methodIterator.remove();
                        }
                    }
                    if (methods.isEmpty()) {
                        pathIterator.remove();
                    }
                }
            }
        }
        Map<String, Object> components = (Map<String, Object>) this.rawValue().get("components");
        Iterator<Map.Entry<String, Object>> componentIterator = components.entrySet().iterator();

        while (componentIterator.hasNext()) {
            Map.Entry<String, Object> componentEntry = componentIterator.next();
            Map<String, Object> componentValue = (Map<String, Object>) componentEntry.getValue();
            schemasToRemove.forEach(componentValue::remove);
        }

        return this.rawValue();
    }
}
