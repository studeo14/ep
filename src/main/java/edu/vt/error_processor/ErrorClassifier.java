package edu.vt.error_processor;

import edu.vt.datasheet_text_processor.Errors.Context.*;
import edu.vt.datasheet_text_processor.Errors.Warning;
import edu.vt.datasheet_text_processor.input.AllMappings;
import edu.vt.datasheet_text_processor.tokens.TokenInstance.TokenInstance;
import edu.vt.datasheet_text_processor.tokens.TokenModel.Token;
import edu.vt.datasheet_text_processor.util.Constants;
import edu.vt.datasheet_text_processor.wordid.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ErrorClassifier {

    private static final Logger logger = LoggerFactory.getLogger( ErrorClassifier.class );

    public static void showMessage ( Warning warning, AllMappings allMappings ) {
        var context = warning.getContext();
        var contextType = getContextType( context );
        logger.debug( "{}", contextType );
        switch ( contextType ) {
            case GENERIC:
                handleGeneric( context );
                break;
            case IR:
                handleIR( context, allMappings );
                break;
            case IRPROPERTY:
                handleIRProperty( context );
                break;
            case IRCOMPOUND:
                handleIRCompound( context, allMappings );
                break;
            case IRCONSEQUENT:
                handleIRConsequent( context, allMappings );
                break;
            case SEMEXPR:
                handleSemexpr( context );
                break;
            case BITACCESSNORMALIZER:
                handleBATNormalizer( context, allMappings );
                break;
            case BITACCESSNORMALIZERFINDER:
                handleBATNormalizerFinder( context, allMappings );
                break;
            case FRAMEFINDER:
                handleFrameFinder( context, allMappings );
                break;
            case SERIALIZER:
                handleSerializer( context );
                break;
            case TOKENIZER:
                handleTokenizer( context, allMappings );
                break;
            default:
                logger.warn( "Unknown context type: {}", contextType );
                break;
        }
    }

    private static void handleTokenizer ( Context context, AllMappings allMappings ) {
        TokenizerContext tc = (TokenizerContext) context;
        logger.info(tc.getMessage());
        var wordText = allMappings.getSerializer().deserialize( List.of(tc.getCurrentWord()) );
        var options = tc.getSearchTreeNode().getChildren().keySet().stream()
                .map(w -> w.equals(Constants.SEARCH_TREE_LEAF_NODE_ID)?"LEAF":allMappings.getSerializer().unconvert( w ))
                .collect(Collectors.toList());
        logger.info("Encountered an unexpected word during tokenization. At word: {}[{}]. Expected options: [{}]", wordText, tc.getWordIndex(), options);
    }

    private static void handleSerializer ( Context context ) {
        SerializerContext sc = (SerializerContext ) context;
        logger.info(sc.getMessage());
        logger.info("Please add a mapping by rerunning the processor with the \"--add-new\" options.");
    }

    private static void handleFrameFinder ( Context context, AllMappings allMappings ) {
        FrameFinderContext fc = (FrameFinderContext) context;
        logger.info(fc.getMessage());
        if (fc.getCurrentNode() == null) {
            var frameText = getTokenText( fc.getFrame().getTokens(), allMappings );
            for (var lit: fc.getFrame().getLiterals()) {
                if (lit.isEmpty()) {
                    var index = fc.getFrame().getLiterals().indexOf( lit );
                    logger.info("In the frame '{}' at literal {}. Fill in the missing information.", frameText, index + 1);
                    return;
                }
            }
            // if here then not empty literal
            var numLit = fc.getFrame().getLiterals().size();
            var expected = allMappings.getFrameMapping().getGeneric().numLiterals(fc.getFrame().getId());
            logger.info("The found frame: '{}' expected {} literals.", frameText, expected);
            if (numLit < expected) {
                logger.info("This difference is often because another frame was detected. This could be in the form of a normal frame (antecedent or consequent) or a compound (and, or, but). If this is the case then you can reorder the frames to get them to work.");
            } else {
                // TODO: ??????
                logger.info("This case should not occur! Contact the developer!");
            }
        } else {
            if (fc.getTokens().isEmpty()) {
                logger.info("Complete literal frame. No valid frame available for parsing. This can be solved by adding a new mapping or alias to the frame mappings.");
            } else {
                var problemToken = fc.getTokens().get(fc.getTokens().size() - 1);
                var problemTokenText = Serializer.mergeWords( allMappings.getSerializer().deserialize( problemToken.getStream() ) );
                fc.getTokens().remove(problemToken);
                var soFar =  getTokenText( fc.getTokens(), allMappings );
                var options = fc.getCurrentNode().getChildren().keySet().stream()
                        .map( tokenId -> allMappings.getTokenMapping().get( tokenId ))
                        .map( s -> s.getId().equals( Constants.LITERAL_TOKEN_ID ) ?Serializer.mergeWords( allMappings.getSerializer().deserialize( s.getStream() ) ):" _ " )
                        .collect(Collectors.toList());
                logger.info("Encountered an unexpected token when parsing the semantic expression. At the token: '{}'. SO far: [{}] Expected options: [{}]", problemTokenText, soFar, options);
            }
        }
    }

    private static void handleBATNormalizerFinder ( Context context, AllMappings allMappings ) {
        BitAccessNormalizerFinderContext batc = (BitAccessNormalizerFinderContext ) context;
        logger.info(batc.getMessage());
        var batcText = getTokenText( batc.getFrameInstance().getTokens(), allMappings );
        var surroundingText = getTokenText( batc.getTokens(), allMappings );
        logger.info("Unable to process the found bit-access '{}' in '{}'", batcText, surroundingText);
    }

    private static void handleBATNormalizer ( Context context, AllMappings allMappings ) {
        BitAccessNormalizerContext batc = (BitAccessNormalizerContext) context;
        logger.info(batc.getMessage());
        var tokenText = getTokenText( batc.getTokens(), allMappings );
        var batText = getTokenText(batc.getBat().getOriginalTokens(), allMappings);
        logger.info("Unable to replace bit-access-token frame '{}' in: '{}'. If this was not not intended to be a bit access, then you must reformat it.", batText, tokenText);
    }

    private static void handleSemexpr ( Context context ) {
        SemanticExpressionContext sc = (SemanticExpressionContext) context;
        logger.info(sc.getMessage());
        if (sc.getSemanticExpression().getAllFrames().isEmpty()) {
            logger.info("This sentence is not supported. Could not find any meaningful information. Try rewriting if this was not intended.");
        } else {
            var antecedentNumber = sc.getSemanticExpression().getAntecedents().size();
            logger.info("Found antecedents but no consequents. Either the consequents are in another sentence (List) or they are not yet supported.");
            logger.debug("In order to fix, join the list sentences together or add a new mapping for the consequent.");
        }

    }

    private static void handleIRConsequent ( Context context, AllMappings allMappings ) {
        IRConsequentContext ic = ( IRConsequentContext ) context;
        logger.info( ic.getMessage() );
        var frameText = getTokenText( ic.getFrame().getTokens(), allMappings );
        logger.info( "Unsupported consequent found in frame: '{}'. " +
                "Try rewriting the frame in a supported format. We found the name '{}' with the description '{}'. " +
                "If this is not what was intended, try reordering the frame.",
                frameText, ic.getNameS(), ic.getDescS() );
    }

    private static void handleIRCompound ( Context context, AllMappings allMappings ) {
        IRCompoundContext ic = ( IRCompoundContext ) context;
        logger.info( ic.getMessage() );
        if ( ic.getOtherFrames() == null && ic.getProblemFrame() == null ) {
            logger.info( "Try adding support for this operator in the mappings file. Or use a supported operator (and, or, but)." );
        } else if ( ic.getOtherFrames() == null && ic.getProblemFrame() != null ) {
            var frameText = getTokenText( ic.getProblemFrame().getTokens(), allMappings );
            logger.info( "Found in the frame: {}. Try reconfiguring to use a supported temporal operator (after, during, before, until).",
                    frameText );
        } else if ( ic.getOtherFrames() != null && ic.getProblemFrame() == null ) {
            var otherFrames = ic.getOtherFrames().stream()
                    .map( f -> getTokenText( f.getTokens(), allMappings ) )
                    .collect( Collectors.toList() );
            logger.info( "Unsupported configuration of frames: '{}'. Try rewriting the sentence to use supported compounds and temporal operators.",
                    otherFrames );
        } else if ( ic.getOtherFrames() != null && ic.getProblemFrame() != null ) {
            var frameText = getTokenText( ic.getProblemFrame().getTokens(), allMappings );
            var otherFrames = ic.getOtherFrames().stream()
                    .map( f -> getTokenText( f.getTokens(), allMappings ) )
                    .collect( Collectors.toList() );
            logger.info( "'{}' was found in '{}'. Try reordering the information so that the compound or " +
                    "temporal operator does not come first. Do not start with \"And...\" or \"Until...\". Otherwise, the sentence is not supported at the compound is connecting no useful information.",
                    frameText, otherFrames );
        }
    }

    private static void handleIRProperty ( Context context ) {
        IRPropertyContext ic = ( IRPropertyContext ) context;
        logger.info( ic.getMessage() );
        logger.info( "Expected two sides of the expression. Found the name: '{}', and property: '{}'",
                ic.getName(), ic.getProp() );
    }

    private static void handleIR ( Context context, AllMappings allMappings ) {
        IRContext ic = ( IRContext ) context;
        logger.info( ic.getMessage() );
        if ( ic.getSemanticExpression() != null ) {
            logger.info( "For the Semantic Expression: '{}'", ic.getSemanticExpression().getTokenText() );
        }
        if ( ic.getProblemFrame() != null ) {
            var frameText = getTokenText( ic.getProblemFrame().getTokens(), allMappings );
            logger.info( "Inside the frame: {}[{}]", ic.getProblemFrame().getId(), frameText );
            logger.info( "Try adding this frame to the Mappings file in order to support it. " +
                    "Otherwise, rewrite the information in the above frame to fit an existing one." );
        }
    }

    private static List< String > getTokenText ( List<TokenInstance> tokens, AllMappings allMappings ) {
        return tokens.stream()
                .map( t -> {
                    if ( t.getType() == TokenInstance.Type.ACCESS ) {
                        return t.toString();
                    } else if ( t.getType() == TokenInstance.Type.COMPOUND ) {
                        return t.getCompoundToken().getOriginalTokens().stream()
                                .map( ti -> {
                                    if ( ti.getType() == TokenInstance.Type.ACCESS ) {
                                        return ti.toString();
                                    } else {
                                        return Serializer.mergeWords( allMappings.getSerializer().deserialize( ti.getStream() ) );
                                    }
                                } )
                                .collect( Collectors.joining( " " ) );
                    } else {
                        return Serializer.mergeWords( allMappings.getSerializer().deserialize( t.getStream() ) );
                    }
                } )
                .collect( Collectors.toList() );
    }

    private static void handleGeneric ( Context context ) {
        GenericContext gc = ( GenericContext ) context;
        logger.info( gc.getMessage() );
    }

    public enum ContextType {
        GENERIC, IR, IRPROPERTY, IRCOMPOUND, IRCONSEQUENT, SEMEXPR, BITACCESSNORMALIZER, BITACCESSNORMALIZERFINDER, FRAMEFINDER, SERIALIZER, TOKENIZER, NONE
    }

    private static ContextType getContextType ( Context context ) {
        if ( context instanceof GenericContext ) {
            return ContextType.GENERIC;
        } else if ( context instanceof IRContext ) {
            return ContextType.IR;
        } else if ( context instanceof IRPropertyContext ) {
            return ContextType.IRPROPERTY;
        } else if ( context instanceof IRCompoundContext ) {
            return ContextType.IRCOMPOUND;
        } else if ( context instanceof IRConsequentContext ) {
            return ContextType.IRCONSEQUENT;
        } else if ( context instanceof SemanticExpressionContext ) {
            return ContextType.SEMEXPR;
        } else if ( context instanceof BitAccessNormalizerContext ) {
            return ContextType.BITACCESSNORMALIZER;
        } else if ( context instanceof BitAccessNormalizerFinderContext ) {
            return ContextType.BITACCESSNORMALIZERFINDER;
        } else if ( context instanceof FrameFinderContext ) {
            return ContextType.FRAMEFINDER;
        } else if ( context instanceof SerializerContext ) {
            return ContextType.SERIALIZER;
        } else if ( context instanceof TokenizerContext ) {
            return ContextType.TOKENIZER;
        } else {
            return ContextType.NONE;
        }
    }
}
