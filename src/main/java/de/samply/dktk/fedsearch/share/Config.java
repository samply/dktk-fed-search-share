package de.samply.dktk.fedsearch.share;

import static de.samply.dktk.fedsearch.share.ClasspathIo.slurp;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.sq2cql.model.Mapping;
import de.numcodex.sq2cql.model.TermCodeNode;
import de.samply.dktk.fedsearch.share.util.Either;
import java.util.List;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Classpath config resources.
 */
@Component
public class Config {

  private static final Logger logger = LoggerFactory.getLogger(Config.class);

  private final Reader<List<Mapping>> mappingsReader;
  private final Reader<TermCodeNode> termCodeNodeReader;
  private final FhirParser fhirParser;

  /**
   * Creates a new Config instance.
   *
   * @param jsonMapper the JSON object mapper from Jackson
   * @param fhirParser the FHIR parser
   */
  public Config(ObjectMapper jsonMapper, FhirParser fhirParser) {
    this.mappingsReader = new Reader<>(jsonMapper.readerForListOf(Mapping.class));
    this.termCodeNodeReader = new Reader<>(jsonMapper.readerFor(TermCodeNode.class));
    this.fhirParser = requireNonNull(fhirParser);
  }

  /**
   * Returns the mappings used in the CQL translation.
   *
   * @return a list of mappings
   */
  @Bean
  public List<Mapping> mappings() {
    return slurp("term-code-mapping.json").flatMap(mappingsReader::readValue)
        .orElseGet(msg -> {
          logger.error(
              "I/O error while reading the file `term-code-mapping.json` from classpath: {} "
                  + "Proceeding with an empty list of mappings.",
              msg);
          return List.of();
        });
  }

  /**
   * Returns the concept tree used in the CQL translation.
   *
   * @return the concept tree
   */
  @Bean
  public TermCodeNode conceptTree() {
    return slurp("term-code-tree.json").flatMap(termCodeNodeReader::readValue)
        .orElseGet(msg -> {
          logger.error(
              "I/O error while reading the file `term-code-tree.json` from classpath: {} "
                  + "Proceeding with an empty term code tree.",
              msg);
          return TermCodeNode.of();
        });
  }

  /**
   * Reads the FHIR library resource used for CQL.
   *
   * @return either the FHIR library resource or an error message
   */
  public Either<String, Library> readLibrary() {
    return slurp("Library.json").flatMap(s -> fhirParser.parseResource(Library.class, s));
  }

  /**
   * Reads the FHIR measure resource used for CQL.
   *
   * @return either the FHIR measure resource or an error message
   */
  public Either<String, Measure> readMeasure() {
    return slurp("Measure.json").flatMap(s -> fhirParser.parseResource(Measure.class, s));
  }
}
