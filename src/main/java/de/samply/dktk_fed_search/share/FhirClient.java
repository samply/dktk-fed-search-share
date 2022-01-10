package de.samply.dktk_fed_search.share;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.samply.dktk_fed_search.share.util.Either;
import java.util.Objects;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

@Component
public class FhirClient {

  private final IGenericClient client;

  public FhirClient(IGenericClient client) {
    this.client = Objects.requireNonNull(client);
  }

  public Either<String, Bundle> transaction(Bundle bundle) {
    return Either.tryGet(() -> client.transaction().withBundle(bundle).encodedJson().execute())
        .mapLeft(Exception::getMessage);
  }
}
