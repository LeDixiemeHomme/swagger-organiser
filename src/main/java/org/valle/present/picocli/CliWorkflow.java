package org.valle.present.picocli;

import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistDecomposedSwagger;
import org.valle.persist.PersistResult;
import org.valle.process.ClearEndpointOnDemand;
import org.valle.process.DecomposeSwagger;
import org.valle.process.GetAndShowEndpoints;
import org.valle.process.KeepEndpointOnDemand;
import org.valle.process.MergeSwagger;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Coordinates the CLI pipeline without coupling option parsing to transformations.
 */
@Slf4j
final class CliWorkflow {

    private CliWorkflow() {
    }

    record Options(
            String swaggerFilePath,
            Set<EndPoint> endpointsToRemove,
            Set<EndPoint> endpointsToKeep,
            boolean decompose,
            boolean persist,
            boolean merge) {
    }

    record Services(
            Function<File, GetSwaggerNode> swaggerNodeFactory,
            Function<GetSwaggerNode, GetAndShowEndpoints> showFactory,
            Function<GetSwaggerNode, ClearEndpointOnDemand> clearFactory,
            Function<GetSwaggerNode, KeepEndpointOnDemand> keepFactory,
            Function<SwaggerNode, GetSwaggerNode> nodeProviderFactory,
            Function<GetSwaggerNode, DecomposeSwagger> decomposeFactory,
            Function<String, PersistDecomposedSwagger> persistDecomposedFactory,
            Function<File, PersistResult<SwaggerNode>> persistResultFactory,
            BiFunction<GetSwaggerNode, File, MergeSwagger> mergeFactory) {
    }

    static void execute(Options options, Services services, String decomposedPath, String resultPath) {
        boolean hasKeep = hasValues(options.endpointsToKeep());
        boolean hasRemove = hasValues(options.endpointsToRemove());
        File swaggerFile = new File(options.swaggerFilePath());
        GetSwaggerNode provider = services.swaggerNodeFactory().apply(swaggerFile);

        if (options.merge()) {
            SwaggerNode merged = services.mergeFactory()
                    .apply(provider, swaggerFile.getParentFile())
                    .execute();
            log.info("Swagger fusionné avec succès.");
            provider = services.nodeProviderFactory().apply(merged);
        }

        services.showFactory().apply(provider).execute();
        GetSwaggerNode resultProvider = applyEndpointSelection(
                provider, options.endpointsToKeep(), options.endpointsToRemove(),
                hasKeep, hasRemove, services);

        Optional<DecomposedSwagger> decomposed = Optional.empty();
        if (options.decompose()) {
            DecomposedSwagger result = services.decomposeFactory().apply(resultProvider).execute();
            log.info("Decomposed Swagger: {}", result);
            decomposed = Optional.of(result);
        }

        if (options.persist()) {
            if (decomposed.isPresent()) {
                services.persistDecomposedFactory().apply(decomposedPath).persist(decomposed.get());
            } else {
                services.persistResultFactory().apply(new File(resultPath))
                        .persist(resultProvider.provide());
            }
        }
    }

    private static GetSwaggerNode applyEndpointSelection(
            GetSwaggerNode provider,
            Set<EndPoint> endpointsToKeep,
            Set<EndPoint> endpointsToRemove,
            boolean hasKeep,
            boolean hasRemove,
            Services services) {
        if (!hasKeep && !hasRemove) {
            return provider;
        }
        if (hasKeep) {
            if (hasRemove) {
                log.warn("--endPointToRemove et --endPointToKeep sont tous les deux renseignés : "
                        + "--endPointToRemove sera ignoré.");
            }
            SwaggerNode kept = services.keepFactory().apply(provider).execute(endpointsToKeep);
            log.info("Kept {} endpoint(s): {}", endpointsToKeep.size(), endpointsToKeep);
            return services.nodeProviderFactory().apply(kept);
        }
        SwaggerNode cleared = services.clearFactory().apply(provider).execute(endpointsToRemove);
        log.info("Removed {} endpoint(s): {}", endpointsToRemove.size(), endpointsToRemove);
        return services.nodeProviderFactory().apply(cleared);
    }

    private static boolean hasValues(Set<EndPoint> endpoints) {
        return endpoints != null && !endpoints.isEmpty();
    }
}
