package org.valle.present.picocli;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistDecomposedSwagger;
import org.valle.persist.PersistResult;
import org.valle.persist.jackson.PersistDecomposedSwaggerImpl;
import org.valle.persist.jackson.PersistResultNodeImpl;
import org.valle.present.logger.ShowEndpointsLoggerImpl;
import org.valle.process.ClearEndpointOnDemand;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.DecomposeSwagger;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.GetAndShowEndpoints;
import org.valle.process.KeepEndpointOnDemand;
import org.valle.process.KeepEndpointOnDemandImpl;
import org.valle.process.ShowEndpointsImpl;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;
import org.valle.provide.fromnode.GetSwaggerNodeFromNodeImpl;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Slf4j
@Command(name = "swagger-organiser",
        mixinStandardHelpOptions = true,
        description = "Outil de gestion et d'organisation des fichiers Swagger")
public class CliApp implements Runnable {

    static final String DECOMPOSED_PATH = "./gene-res/decomp-from-cli";
    static final String RESULT_PATH = "./gene-res/swagger-from-cli.yml";

    @Option(names = {"-sf", "--swaggerFilePath"}, required = true,
            description = "Chemin du fichier swagger, ex: -sf src/main/resources/swagger-cobaye.yml")
    private String swaggerFilePath;

    @Option(names = {"-toRm", "--endPointToRemove"}, required = false, split = ",",
            description = "Liste des endpoints a supprimer, format: method:path, ex: -toRm get:toto/id,post:tata. "
                    + "Ignoré si --endPointToKeep est aussi renseigné.")
    private Set<EndPoint> endPointToRemove;

    @Option(names = {"-toKeep", "--endPointToKeep"}, required = false, split = ",",
            description = "Liste des endpoints a conserver (tous les autres seront supprimes), "
                    + "format: method:path, ex: -toKeep get:toto/id,post:tata. "
                    + "Prioritaire sur --endPointToRemove si les deux options sont renseignees.")
    private Set<EndPoint> endPointToKeep;

    @Option(names = {"-d", "--decomposeSwagger"},
            description = "A renseigner si le programme doit decomposer le swagger en plusieurs fichiers, defaut: false")
    private boolean shouldDecomposeSwagger;

    @Option(names = {"-pf", "--persistFile"},
            description = "A renseigner si le programme doit creer des fichiers contenant le resultat de l'execution, defaut: false")
    private boolean shouldPersistFile;

    Function<File, GetSwaggerNode> swaggerNodeFactory = GetSwaggerNodeJacksonFromFileImpl::new;
    Function<GetSwaggerNode, GetAndShowEndpoints> showFactory = gsn -> new ShowEndpointsImpl(gsn, new ShowEndpointsLoggerImpl());
    Function<GetSwaggerNode, ClearEndpointOnDemand> clearFactory = ClearEndpointOnDemandImpl::new;
    Function<GetSwaggerNode, KeepEndpointOnDemand> keepFactory = KeepEndpointOnDemandImpl::new;
    Function<SwaggerNode, GetSwaggerNode> nodeProviderFactory = GetSwaggerNodeFromNodeImpl::new;
    Function<GetSwaggerNode, DecomposeSwagger> decomposeFactory = DecomposeSwaggerImpl::new;
    Function<String, PersistDecomposedSwagger> persistDecomposedFactory = PersistDecomposedSwaggerImpl::new;
    Function<File, PersistResult<ObjectNode>> persistResultFactory = PersistResultNodeImpl::new;

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new CliApp());
        commandLine.registerConverter(EndPoint.class, EndPoint::fromString);

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        if ((endPointToKeep == null || endPointToKeep.isEmpty())
                && (endPointToRemove == null || endPointToRemove.isEmpty())) {
            throw new CommandLine.ParameterException(
                    new CommandLine(this),
                    "Au moins une des options --endPointToRemove (-toRm) ou --endPointToKeep (-toKeep) doit être renseignée.");
        }

        GetSwaggerNode provider = swaggerNodeFactory.apply(new File(swaggerFilePath));

        showFactory.apply(provider).execute();

        final SwaggerNode resultNode;
        if (endPointToKeep != null && !endPointToKeep.isEmpty()) {
            if (endPointToRemove != null && !endPointToRemove.isEmpty()) {
                log.warn("--endPointToRemove et --endPointToKeep sont tous les deux renseignés : "
                        + "--endPointToRemove sera ignoré.");
            }
            resultNode = keepFactory.apply(provider).execute(endPointToKeep);
            log.info("Kept {} endpoint(s): {}", endPointToKeep.size(), endPointToKeep);
        } else {
            resultNode = clearFactory.apply(provider).execute(endPointToRemove);
            log.info("Removed {} endpoint(s): {}", endPointToRemove.size(), endPointToRemove);
        }

        GetSwaggerNode resultProvider = nodeProviderFactory.apply(resultNode);

        Optional<DecomposedSwagger> decomposed = Optional.empty();
        if (shouldDecomposeSwagger) {
            DecomposedSwagger result = decomposeFactory.apply(resultProvider).execute();
            log.info("Decomposed Swagger: {}", result);
            decomposed = Optional.of(result);
        }

        if (shouldPersistFile) {
            if (decomposed.isPresent()) {
                persistDecomposedFactory.apply(DECOMPOSED_PATH).persist(decomposed.get());
            } else {
                persistResultFactory.apply(new File(RESULT_PATH)).persist((ObjectNode) resultNode.node());
            }
        }
    }
}
