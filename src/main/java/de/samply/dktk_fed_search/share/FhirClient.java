package de.samply.dktk_fed_search.share;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.ITransactionTyped;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import de.samply.dktk_fed_search.share.util.Either;
import java.util.Objects;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FhirClient {

  private static final Logger logger = LoggerFactory.getLogger(FhirClient.class);

  private final IGenericClient client;

  public FhirClient(IGenericClient client) {
    this.client = Objects.requireNonNull(client);
  }

  public Either<String, Bundle> transact(Bundle bundle) {
    logger.info("Transact a bundle.");
    return Either.tryGet(() -> createTransactionOperation(bundle).execute())
        .mapLeft(Exception::getMessage);
  }

  private ITransactionTyped<Bundle> createTransactionOperation(Bundle bundle) {
    return client.transaction().withBundle(bundle).encodedJson();
  }

  public Either<String, MeasureReport> evaluateMeasure(String measureUri) {
    logger.info("Evaluate measure with canonical URI `{}`.", measureUri);
    return Either.tryGet(() -> createOperation(measureUri).execute())
        .mapLeft(Exception::getMessage);
  }

  private IOperationUntypedWithInput<MeasureReport> createOperation(String measureUri) {
    return client.operation()
        .onType(Measure.class)
        .named("evaluate-measure")
        .withSearchParameter(Parameters.class, "measure", new StringParam(measureUri))
        .andSearchParameter("periodStart", new DateParam("1900"))
        .andSearchParameter("periodEnd", new DateParam("2100"))
        .useHttpGet()
        .returnResourceType(MeasureReport.class)
        .encodedJson();
  }
}
