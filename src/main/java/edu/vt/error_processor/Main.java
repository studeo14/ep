package edu.vt.error_processor;

import edu.vt.datasheet_text_processor.Errors.ProcessorException;
import edu.vt.error_processor.cli.Application;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

class Main {
    private static final Logger logger = LoggerFactory.getLogger( Main.class);

    public static void main(String... args) {
        var options = new Application();
        var cli = new CommandLine(options);
        try {
            cli.parseArgs(args);
            handleCli(cli, options);
        } catch ( IOException | ProcessorException e) {
            logger.error(e.getMessage(), e);
        } catch (CommandLine.ParameterException e) {
            logger.error(e.getMessage());
            cli.usage(System.out);
        } catch (RuntimeException e) {
            logger.error("Unknown runtime exception!: {} :: {}", e.getMessage(), e.getCause(), e);
        }
    }

    private static void handleCli(CommandLine cli, Application options) throws IOException, ProcessorException {
        if (cli.isUsageHelpRequested()) {
            cli.usage(System.out);
        } else {
            // verbosity change (package only)
            if (options.verbose) {
                Configurator.setAllLevels("edu.vt", Level.DEBUG);
            }
            // handle
            if (options.doShowTime) {
                Instant start = Instant.now();
                OptionHandler.handle(options);
                Instant end = Instant.now();
                var duration = Duration.between( start, end );
                logger.info("Execution Duration: {} [{} (ms)]", duration, duration.toMillis());
            } else {
                OptionHandler.handle(options);
            }
        }
    }
}
