package de.samply.dktk.fedsearch.share.service;

import static java.util.Objects.requireNonNull;

import de.samply.dktk.fedsearch.share.BrokerClient;
import de.samply.dktk.fedsearch.share.Variables;
import de.samply.dktk.fedsearch.share.broker.model.Inquiry;
import de.samply.dktk.fedsearch.share.model.InquiryState;
import de.samply.dktk.fedsearch.share.model.InquiryStateRepository;
import de.samply.dktk.fedsearch.share.util.Either;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * Fetches the inquiry with the identifier taken from its {@link InquiryState state}, loaded from
 * the database by id taken from {@link Variables#getInquiryStateId() variables} and puts the
 * structured-query of the inquiry into {@link Variables#setStructuredQuery(String) variables}.
 */
@Component
public class FetchInquiryDelegate extends AbstractInquiryStateDelegate {

  private final BrokerClient client;

  public FetchInquiryDelegate(BrokerClient client, InquiryStateRepository inquiryStateRepository) {
    super(inquiryStateRepository);
    this.client = requireNonNull(client);
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    var variables = Variables.of(execution);
    String structuredQuery = getInquiryState(variables)
        .map(InquiryState::inquiryId)
        .flatMap(client::fetchInquiry)
        .flatMap(this::getStructuredQuery)
        .orElseThrow(Exception::new);
    variables.setStructuredQuery(structuredQuery);
  }

  private Either<String, String> getStructuredQuery(Inquiry inquiry) {
    return Either.fromOptional(inquiry.getStructuredQuery(),
        "missing structured query in inquiry with id `%s`".formatted(inquiry.getId()));
  }
}
