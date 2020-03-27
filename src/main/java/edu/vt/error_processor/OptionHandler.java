package edu.vt.error_processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.vt.datasheet_text_processor.Project;
import edu.vt.datasheet_text_processor.ProjectUtils;
import edu.vt.datasheet_text_processor.Sentence;
import edu.vt.datasheet_text_processor.input.AllMappings;
import edu.vt.error_processor.cli.Application;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OptionHandler {

    private static final Logger logger = LoggerFactory.getLogger( OptionHandler.class );


    private static void showErrorMessages ( Project project, AllMappings allMappings ) {
        logger.info( "Getting Error Messages" );
        var db = project.getDB();
        var repo = db.getRepository( Sentence.class );
        var documents = repo.find( ObjectFilters.eq( "type", Sentence.Type.NONCOMMENT ), FindOptions.sort( "sentenceId", SortOrder.Ascending ) );
        for (var sentence: documents) {
            var warnings = sentence.getWarnings();
            if (warnings != null && warnings.size() > 0) {
                logger.info("For the sentence: {}", sentence.getText());
                for (var warning: warnings) {
                    ErrorClassifier.showMessage(warning, allMappings);
                }
            } else {
                logger.debug("No warnings for sentence: {}", sentence.getSentenceId());
            }

        }

    }

    private static void reclassify ( Project project ) {

    }

    private static void showMetrics ( Project project ) {

    }

    public static void handle ( Application options ) throws IOException {
        // initialize project
        var project = ProjectUtils.openProject(options.inputFile);
        if (project.getDB() == null) {
            logger.warn("Unable to open project!");
        } else {
            // init mappings
            var allMappings = new ObjectMapper().readValue( options.mappingFile, AllMappings.class );
            allMappings.init();
            // check for options
            if (options.doShowErrorMessages) {
                showErrorMessages(project, allMappings);
            }
            if (options.doReclassify) {
                reclassify(project);
            }
            if (options.doShowMetrics) {
                showMetrics(project);
            }
        }
    }
}
