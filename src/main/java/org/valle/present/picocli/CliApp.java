package org.valle.present.picocli;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.jackson.PersistDecomposedSwaggerImpl;
import org.valle.persist.jackson.PersistResultNodeImpl;
import org.valle.present.logger.ShowEndpointsLoggerImpl;
import org.valle.process.ClearEndpointOnDemand;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.GetAndShowEndpoints;
import org.valle.process.ShowEndpointsImpl;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;
import org.valle.provide.fromnode.GetSwaggerNodeFromNodeImpl;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class CliApp implements Runnable {

    @Option(names = {"-sf", "--swaggerFilePath"}, required = true, description = "Chemin du fichier swagger, ex: -sf src/main/resources/swagger-cobaye.yml")
    private String swaggerFilePath;

    @Option(names = {"-toRm", "--endPointToRemove"}, required = true, split = ",", description = "Liste des endpoints à supprimer, format: method:path, ex: -toRm get:toto/id,post:tata")
    private Set<EndPoint> endPointToRemove;

    @Option(names = {"-d", "--decomposeSwagger"}, description = "A renseigner si le programme doit décomposer le swagger en plusieurs fichiers, défaut: false")
    private boolean shouldDecomposeSwagger;

    @Option(names = {"-pf", "--persistFile"}, description = "A renseigner si le programme doit créer des fichiers contenant le résultat de l'exécution, défaut: false")
    private boolean shouldPersistFile;

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new CliApp());
        commandLine.registerConverter(EndPoint.class, EndPoint::fromString);

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        try {
            File file = new File(this.swaggerFilePath);
            GetSwaggerNode getSwaggerNode = new GetSwaggerNodeJacksonFromFileImpl(file);
            Optional<SwaggerNode> swaggerNode = Optional.empty();
            Optional<DecomposedSwagger> decomposedSwagger = Optional.empty();

            GetAndShowEndpoints showEndpoints = new ShowEndpointsImpl(
                    getSwaggerNode,
                    new ShowEndpointsLoggerImpl()
            );
            showEndpoints.execute();

            for (EndPoint endPoint : endPointToRemove) {
                log.info("Removing endpoint: {}", endPoint);
            }

            if (endPointToRemove != null && !endPointToRemove.isEmpty()) {
                ClearEndpointOnDemand clearEndpointOnDemand = new ClearEndpointOnDemandImpl(getSwaggerNode);
                SwaggerNode executed = clearEndpointOnDemand.execute(endPointToRemove);
                swaggerNode = Optional.of(executed);
                getSwaggerNode = new GetSwaggerNodeFromNodeImpl(executed);
            }

            if (shouldDecomposeSwagger) {
                DecomposedSwagger executed = new DecomposeSwaggerImpl(getSwaggerNode).execute();
                log.info("Decomposed Swagger: {}", executed);
                decomposedSwagger = Optional.of(executed);
            }

            if (shouldPersistFile) {
                if (decomposedSwagger.isPresent()) {
                    new PersistDecomposedSwaggerImpl("src/main/resources/gene-res/decomp-from-cli").persist(decomposedSwagger.get());
                } else if (swaggerNode.isPresent()) {
                    new PersistResultNodeImpl(new File("src/main/resources/gene-res/swagger-from-cli.yml")).persist((ObjectNode) swaggerNode.get().node());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
