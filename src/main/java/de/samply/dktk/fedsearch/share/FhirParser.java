package de.samply.dktk.fedsearch.share;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import de.samply.dktk.fedsearch.share.util.Either;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

/**
 * A FHIR parser providing an {@link Either} API.
 */
@Component
@SuppressWarnings("ClassCanBeRecord")
public class FhirParser {

  private final FhirContext fhirContext;

  public FhirParser(FhirContext fhirContext) {
    this.fhirContext = requireNonNull(fhirContext);
  }

  /**
   * Parses a FHIR resource of the given {@code type} from {@code s}.
   *
   * @param type the type of the FHIR resource
   * @param s    the string to parse
   * @param <T>  the type of the FHIR resource
   * @return either the parsed FHIR resource or an error message
   */
  public <T extends IBaseResource> Either<String, T> parseResource(Class<T> type, String s) {
    var parser = fhirContext.newJsonParser();
    return Either.tryGet(() -> type.cast(parser.parseResource(s))).mapLeft(Exception::getMessage);
  }
}
