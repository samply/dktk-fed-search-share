package de.samply.dktk_fed_search.share;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.sq2cql.model.ConceptNode;
import de.numcodex.sq2cql.model.Mapping;
import de.samply.dktk_fed_search.share.service.CreateMeasureResourcesDelegate;
import de.samply.dktk_fed_search.share.util.Either;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class Config {

  private static final Logger logger = LoggerFactory.getLogger(Config.class);

  private final ObjectMapper jsonMapper;
  private final FhirContext fhirContext;

  public Config(ObjectMapper jsonMapper, FhirContext fhirContext) {
    this.jsonMapper = jsonMapper;
    this.fhirContext = fhirContext;
  }

  @Bean
  public List<Mapping> readMappings() {
    try (InputStream in = Config.class.getResourceAsStream("term-code-mapping.json")) {
      return jsonMapper.readerForListOf(Mapping.class).readValue(in);
    } catch (IOException e) {
      return List.of();
    }
  }

  @Bean
  public ConceptNode readConceptTree() {
    try (InputStream in = Config.class.getResourceAsStream("term-code-tree.json")) {
      return jsonMapper.readerFor(ConceptNode.class).readValue(in);
    } catch (IOException e) {
      return ConceptNode.of();
    }
  }

  public Either<String, Library> readLibrary() {
    return slurp("Library.json").flatMap(s -> parseResource(Library.class, s));
  }

  public Either<String, Measure> readMeasure() {
    return slurp("Measure.json").flatMap(s -> parseResource(Measure.class, s));
  }

  private static Either<String, String> slurp(String name) {
    try (InputStream in = Config.class.getResourceAsStream(name)) {
      if (in == null) {
        logger.error("file `{}` not found in classpath", name);
        return Either.left(format("file `%s` not found in classpath", name));
      } else {
        logger.info("read file `{}` from classpath", name);
        return Either.right(new String(in.readAllBytes(), UTF_8));
      }
    } catch (IOException e) {
      logger.error("error while reading the file `{}` from classpath", name, e);
      return Either.left(format("error while reading the file `%s` from classpath", name));
    }
  }

  private <T extends IBaseResource> Either<String, T> parseResource(Class<T> type, String s) {
    var parser = fhirContext.newJsonParser();
    return Either.tryGet(() -> type.cast(parser.parseResource(s))).mapLeft(Exception::getMessage);
  }
}
