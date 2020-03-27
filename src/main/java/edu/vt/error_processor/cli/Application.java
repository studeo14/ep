package edu.vt.error_processor.cli;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;

@Command(name = "dtp")
public class Application {
    @Option(names = {"-m", "--mappings"}, description = "mapping file (JSON)", required = true, paramLabel = "MAPPINGFILE")
    public File mappingFile;

    // new project flags
    @Option(names = {"-v","--verbose"}, description = "display debugging info")
    public boolean verbose;

    @Option(names = {"-f", "--file"}, required = true, paramLabel = "INPUTFILE", description = "the input cas/project/text file")
    public File inputFile;

    @Option(names = {"-M", "--metrics"}, description = "show metrics about the errors")
    public boolean doShowMetrics;

    @Option(names = {"-r", "--reclassify"}, description = "reclassify sentences with unrecoverable errors as 'Comments'")
    public boolean doReclassify;

    @Option(names = {"-e", "--error-messages"}, description = "show helpful messages about the found errors")
    public boolean doShowErrorMessages;

    @Option(names = {"-t", "--time"}, description = "show execution time")
    public boolean doShowTime;

}
