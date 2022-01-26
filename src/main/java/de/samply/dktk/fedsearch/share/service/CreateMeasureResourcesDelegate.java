package de.samply.dktk.fedsearch.share.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;

import de.numcodex.sq2cql.PrintContext;
import de.numcodex.sq2cql.Translator;
import de.numcodex.sq2cql.model.structured_query.StructuredQuery;
import de.samply.dktk.fedsearch.share.Config;
import de.samply.dktk.fedsearch.share.FhirClient;
import de.samply.dktk.fedsearch.share.Reader;
import de.samply.dktk.fedsearch.share.Variables;
import de.samply.dktk.fedsearch.share.util.Either;
import java.util.function.Supplier;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.springframework.stereotype.Component;

/**
 * Takes a structured query from {@link Variables#getStructuredQuery() variables}, translates it to
 * CQL, creates a Measure and Library resource on the FHIR server and puts the canonical URI of the
 * Measure into variables.
 */
@Component
public class CreateMeasureResourcesDelegate implements JavaDelegate {

  private final Reader<StructuredQuery> structuredQueryReader;
  private final Translator cqlTranslator;
  private final Supplier<String> randomUriSupplier;
  private final FhirClient client;
  private final Config config;

  /**
   * Creates a new CreateMeasureResourcesDelegate instance.
   *
   * @param structuredQueryReader a reader for structured queries
   * @param cqlTranslator         the CQL translator
   * @param randomUriSupplier     a supplier of random URIs
   * @param client                the FHIR client
   * @param config                the config instance to obtain the library and measure resources
   */
  public CreateMeasureResourcesDelegate(Reader<StructuredQuery> structuredQueryReader,
      Translator cqlTranslator, Supplier<String> randomUriSupplier, FhirClient client,
      Config config) {
    this.structuredQueryReader = requireNonNull(structuredQueryReader);
    this.cqlTranslator = requireNonNull(cqlTranslator);
    this.randomUriSupplier = requireNonNull(randomUriSupplier);
    this.client = requireNonNull(client);
    this.config = requireNonNull(config);
  }

  private static Bundle createBundle1(Library library, Measure measure) {
    var bundle = new Bundle();
    bundle.setType(TRANSACTION);
    bundle.addEntry().setResource(library).getRequest().setMethod(POST).setUrl("Library");
    bundle.addEntry().setResource(measure).getRequest().setMethod(POST).setUrl("Measure");
    return bundle;
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    var uri = getStructuredQuery(execution).flatMap(query -> {
      var cql = cqlTranslator.toCql(query).print(PrintContext.ZERO);
      var libraryUri = randomUriSupplier.get();
      var measureUri = randomUriSupplier.get();
      return createBundle(cql, libraryUri, measureUri)
          .flatMap(client::transact)
          .map(b -> measureUri);
    }).orElseThrow(Exception::new);
    Variables.of(execution).setMeasureUri(uri);
  }

  private Either<String, StructuredQuery> getStructuredQuery(DelegateExecution execution) {
    return Variables.of(execution).getStructuredQuery().flatMap(structuredQueryReader::readValue);
  }

  private Either<String, Bundle> createBundle(String cql, String libraryUri, String measureUri) {
    return config.readLibrary()
        .map(l -> appendCql(l.setUrl(libraryUri), cql))
        .flatMap(l -> config.readMeasure()
            .map(m -> m.setUrl(measureUri).addLibrary(libraryUri))
            .map(m -> createBundle1(l, m)));
  }

  private Library appendCql(Library library, String cql) {
    library.getContentFirstRep().setContentType("text/cql");
    library.getContentFirstRep().setData(cql.getBytes(UTF_8));
    return library;
  }
}
