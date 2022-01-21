package de.samply.dktk_fed_search.share.service;

import de.samply.dktk_fed_search.share.BrokerClient;
import de.samply.dktk_fed_search.share.Variables;
import de.samply.dktk_fed_search.share.broker.model.Inquiry;
import de.samply.dktk_fed_search.share.util.Either;
import java.util.Objects;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fetches the inquiry with the identifier taken from {@link Variables#getInquiryId() variables} and
 * puts the structured query of the inquiry into variables.
 */
@Component
public class FetchInquiryDelegate implements JavaDelegate {

  private final BrokerClient client;

  public FetchInquiryDelegate(BrokerClient client) {
    this.client = Objects.requireNonNull(client);
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    String structuredQuery = Variables.of(execution).getInquiryId()
        .map(client::fetchInquiry)
        .flatMap(this::getStructuredQuery)
        .orElseThrow(Exception::new);
    Variables.of(execution).setStructuredQuery(structuredQuery);
  }

  private Either<String, String> getStructuredQuery(Inquiry inquiry) {
    return Either.fromOptional(inquiry.getStructuredQuery(),
        "missing structured query in inquiry with id `%s`".formatted(inquiry.getId()));
  }
}
