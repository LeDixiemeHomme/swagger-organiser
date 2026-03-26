package org.valle;

import org.valle.present.picocli.CliApp;
import org.valle.present.rest.RestServer;
import org.valle.process.models.EndPoint;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Arrays;

/**
 * Point d'entrée unifié du JAR.
 *
 * <ul>
 *   <li><b>Serveur REST</b> : {@code java -jar swagger-organiser.jar server [port]}</li>
 *   <li><b>CLI</b>         : {@code java -jar swagger-organiser.jar -sf swagger.yml -toRm post:/path ...}</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            String[] serverArgs = Arrays.copyOfRange(args, 1, args.length);
            RestServer.main(serverArgs);
        } else {
            CommandLine commandLine = new CommandLine(new CliApp());
            commandLine.registerConverter(EndPoint.class, EndPoint::fromString);
            int exitCode = commandLine.execute(args);
            System.exit(exitCode);
        }
    }
}