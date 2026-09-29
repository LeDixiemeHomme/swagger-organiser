package org.valle.present.picocli;

import org.valle.persist.PersistDecomposedSwagger;
import org.valle.persist.PersistResult;
import org.valle.persist.jackson.PersistDecomposedSwaggerImpl;
import org.valle.persist.jackson.PersistResultNodeImpl;
import org.valle.present.logger.ShowEndpointsLoggerImpl;
import org.valle.process.ClearEndpointOnDemand;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.DecomposeSwagger;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.KeepEndpointOnDemand;
import org.valle.process.KeepEndpointOnDemandImpl;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;
import org.valle.provide.fromnode.GetSwaggerNodeFromNodeImpl;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import org.valle.process.MergeSwagger;
import org.valle.process.MergeSwaggerImpl;
import java.io.File;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

@Command(name = "swagger-organiser",
        mixinStandardHelpOptions = true,
        description = "Outil de gestion et d'organisation des fichiers Swagger")
public class CliApp implements Runnable {

    static final String DECOMPOSED_PATH = "./gene-res/decomp-from-cli";
    static final String RESULT_PATH = "./gene-res/swagger-from-cli.yml";

    @Option(names = {"-sf", "--swaggerFilePath", "--swagger-file"}, required = true,
            description = "Chemin du fichier swagger, ex: -sf src/main/resources/swagger-cobaye.yml")
    private String swaggerFilePath;

    @Option(names = {"-toRm", "--endPointToRemove", "--remove-endpoints"}, required = false, split = ",",
            description = "Liste des endpoints a supprimer, format: method:path, ex: -toRm get:toto/id,post:tata. "
                    + "Ignoré si --endPointToKeep est aussi renseigné.")
    private Set<EndPoint> endPointToRemove;
    @Option(names = {"-toKeep", "--endPointToKeep", "--keep-endpoints"}, required = false, split = ",",
            description = "Liste des endpoints a conserver (tous les autres seront supprimes), "
                    + "format: method:path, ex: -toKeep get:toto/id,post:tata. "
                    + "Prioritaire sur --endPointToRemove si les deux options sont renseignees.")
    private Set<EndPoint> endPointToKeep;

    @Option(names = {"-d", "--decomposeSwagger", "--decompose"},
            description = "A renseigner si le programme doit decomposer le swagger en plusieurs fichiers, defaut: false")
    private boolean shouldDecomposeSwagger;

    @Option(names = {"-pf", "--persistFile", "--persist"},
            description = "A renseigner si le programme doit creer des fichiers contenant le resultat de l'execution, defaut: false")
    private boolean shouldPersistFile;

    @Option(names = {"-m", "--mergeSwagger", "--merge"},
            description = "Fusionne un swagger décomposé (multi-fichiers $ref) en un seul fichier, contraire de --decomposeSwagger. Defaut: false")
    private boolean shouldMergeSwagger;

    @Option(names = "--preserve-comments", negatable = true, defaultValue = "true",
            description = "Préserve les commentaires YAML dans les fichiers générés (défaut: ${DEFAULT-VALUE}).")
    private boolean preserveComments;

    @Spec
    CommandLine.Model.CommandSpec spec;

    Function<File, GetSwaggerNode> swaggerNodeFactory = GetSwaggerNodeJacksonFromFileImpl::new;
    Function<GetSwaggerNode, org.valle.process.GetAndShowEndpoints> showFactory =
            gsn -> new org.valle.process.ShowEndpointsImpl(gsn, new ShowEndpointsLoggerImpl());
    Function<GetSwaggerNode, ClearEndpointOnDemand> clearFactory = ClearEndpointOnDemandImpl::new;
    Function<GetSwaggerNode, KeepEndpointOnDemand> keepFactory = KeepEndpointOnDemandImpl::new;
    Function<SwaggerNode, GetSwaggerNode> nodeProviderFactory = GetSwaggerNodeFromNodeImpl::new;
    Function<GetSwaggerNode, DecomposeSwagger> decomposeFactory = DecomposeSwaggerImpl::new;
    Function<String, PersistDecomposedSwagger> persistDecomposedFactory = PersistDecomposedSwaggerImpl::new;
    Function<File, PersistResult<SwaggerNode>> persistResultFactory =
            PersistResultNodeImpl::new;
    BiFunction<GetSwaggerNode, File, MergeSwagger> mergeFactory = MergeSwaggerImpl::new;

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new CliApp());
        commandLine.registerConverter(EndPoint.class, EndPoint::fromString);

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        if (shouldDecomposeSwagger && shouldMergeSwagger) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    "Les options --decompose et --merge sont exclusives.");
        }
        CliWorkflow.execute(
                new CliWorkflow.Options(swaggerFilePath, endPointToRemove, endPointToKeep,
                        shouldDecomposeSwagger, shouldPersistFile, shouldMergeSwagger, preserveComments),
                new CliWorkflow.Services(
                        swaggerNodeFactory, showFactory, clearFactory, keepFactory,
                        nodeProviderFactory, decomposeFactory, persistDecomposedFactory,
                        persistResultFactory, mergeFactory),
                DECOMPOSED_PATH, RESULT_PATH);
    }
}
