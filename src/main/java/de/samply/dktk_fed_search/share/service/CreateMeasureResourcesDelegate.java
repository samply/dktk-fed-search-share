package de.samply.dktk_fed_search.share.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;

import de.numcodex.sq2cql.PrintContext;
import de.numcodex.sq2cql.Translator;
import de.numcodex.sq2cql.model.structured_query.StructuredQuery;
import de.samply.dktk_fed_search.share.Config;
import de.samply.dktk_fed_search.share.FhirClient;
import de.samply.dktk_fed_search.share.Reader;
import de.samply.dktk_fed_search.share.Variables;
import de.samply.dktk_fed_search.share.util.Either;
import java.util.Objects;
import java.util.function.Supplier;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Takes a structured query from {@link Variables#getStructuredQuery() variables}, translates it to
 * CQL, creates a Measure and Library resource on the FHIR server and puts the canonical URI of the
 * Measure into variables.
 */
@Component
public class CreateMeasureResourcesDelegate implements JavaDelegate {

  private static final Logger logger = LoggerFactory.getLogger(
      CreateMeasureResourcesDelegate.class);

  private final Reader<StructuredQuery> structuredQueryReader;
  private final Translator translator;
  private final Supplier<String> randomUriSupplier;
  private final FhirClient client;
  private final Config config;

  public CreateMeasureResourcesDelegate(Reader<StructuredQuery> structuredQueryReader,
      Translator translator, Supplier<String> randomUriSupplier, FhirClient client,
      Config config) {
    this.structuredQueryReader = Objects.requireNonNull(structuredQueryReader);
    this.translator = Objects.requireNonNull(translator);
    this.randomUriSupplier = Objects.requireNonNull(randomUriSupplier);
    this.client = Objects.requireNonNull(client);
    this.config = Objects.requireNonNull(config);
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    var uri = getStructuredQuery(execution).flatMap(query -> {
      var cql = translator.toCql(query).print(PrintContext.ZERO);
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

  private static Bundle createBundle1(Library library, Measure measure) {
    var bundle = new Bundle();
    bundle.setType(TRANSACTION);
    bundle.addEntry().setResource(library).getRequest().setMethod(POST).setUrl("Library");
    bundle.addEntry().setResource(measure).getRequest().setMethod(POST).setUrl("Measure");
    return bundle;
  }
}
