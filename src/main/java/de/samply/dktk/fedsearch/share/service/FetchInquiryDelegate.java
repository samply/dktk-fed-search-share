package de.samply.dktk.fedsearch.share.service;

import static java.util.Objects.requireNonNull;

import de.samply.dktk.fedsearch.share.BrokerClient;
import de.samply.dktk.fedsearch.share.Variables;
import de.samply.dktk.fedsearch.share.broker.model.Inquiry;
import de.samply.dktk.fedsearch.share.util.Either;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Fetches the inquiry with the identifier taken from {@link Variables#getInquiryId() variables} and
 * puts the structured query of the inquiry into variables.
 */
@Component
public class FetchInquiryDelegate implements JavaDelegate {

  private final BrokerClient client;

  public FetchInquiryDelegate(BrokerClient client) {
    this.client = requireNonNull(client);
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    String structuredQuery = Variables.of(execution).getInquiryId()
        .flatMap(client::fetchInquiry)
        .flatMap(this::getStructuredQuery)
        .orElseThrow(Exception::new);
    Variables.of(execution).setStructuredQuery(structuredQuery);
  }

  private Either<String, String> getStructuredQuery(Inquiry inquiry) {
    return Either.fromOptional(inquiry.getStructuredQuery(),
        "missing structured query in inquiry with id `%s`".formatted(inquiry.getId()));
  }
}
