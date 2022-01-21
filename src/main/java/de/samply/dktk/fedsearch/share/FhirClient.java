package de.samply.dktk.fedsearch.share;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IClientExecutable;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.ITransactionTyped;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import de.samply.dktk.fedsearch.share.util.Either;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A client for the REST API of a FHIR server.
 */
@Component
@SuppressWarnings("ClassCanBeRecord")
public class FhirClient {

  private static final Logger logger = LoggerFactory.getLogger(FhirClient.class);

  private final IGenericClient client;

  public FhirClient(IGenericClient client) {
    this.client = requireNonNull(client);
  }

  private static <T> Either<String, T> execute(IClientExecutable<?, T> mono) {
    return Either.tryGet(mono::execute).mapLeft(Exception::getMessage);
  }

  private ITransactionTyped<Bundle> createTransactionOperation(Bundle bundle) {
    return client.transaction().withBundle(bundle).encodedJson();
  }

  /**
   * Executes a transaction described in {@code bundle}.
   *
   * @param bundle the transaction
   * @return either the transaction response or an error message
   */
  public Either<String, Bundle> transact(Bundle bundle) {
    logger.info("Transact a bundle.");
    return execute(createTransactionOperation(bundle));
  }

  /**
   * Evaluates the measure with the given {@code canonicalUri}.
   *
   * @param canonicalUri the canonical URI of the measure
   * @return either the resulting {@link MeasureReport} or an error message
   */
  public Either<String, MeasureReport> evaluateMeasure(String canonicalUri) {
    logger.info("Evaluate measure with canonical URI `{}`.", canonicalUri);
    return execute(createOperation(canonicalUri));
  }

  private IOperationUntypedWithInput<MeasureReport> createOperation(String canonicalUri) {
    return client.operation()
        .onType(Measure.class)
        .named("evaluate-measure")
        .withSearchParameter(Parameters.class, "measure", new StringParam(canonicalUri))
        .andSearchParameter("periodStart", new DateParam("1900"))
        .andSearchParameter("periodEnd", new DateParam("2100"))
        .useHttpGet()
        .returnResourceType(MeasureReport.class)
        .encodedJson();
  }
}
