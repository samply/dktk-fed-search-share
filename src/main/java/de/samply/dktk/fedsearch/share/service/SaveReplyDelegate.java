package de.samply.dktk.fedsearch.share.service;

import static de.samply.dktk.fedsearch.share.model.InquiryState.State.FINISHED;
import static java.util.Objects.requireNonNull;

import de.samply.dktk.fedsearch.share.BrokerClient;
import de.samply.dktk.fedsearch.share.Variables;
import de.samply.dktk.fedsearch.share.model.InquiryState;
import de.samply.dktk.fedsearch.share.model.InquiryStateRepository;
import de.samply.dktk.fedsearch.share.util.Either;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * Takes a count from {@link Variables#getCount() variables} and reports it back to the connector.
 */
@Component
public class SaveReplyDelegate extends AbstractInquiryStateDelegate {

  private final BrokerClient client;

  public SaveReplyDelegate(BrokerClient client, InquiryStateRepository inquiryStateRepository) {
    super(inquiryStateRepository);
    this.client = requireNonNull(client);
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    var variables = Variables.of(execution);
    var finishedInquiryState = variables.getCount()
        .flatMap(count -> getInquiryState(variables)
            .flatMap(inquiryState -> saveReply(inquiryState, count)))
        .orElseThrow(Exception::new);
    inquiryStateRepository.save(finishedInquiryState);
  }

  private Either<String, InquiryState> saveReply(InquiryState inquiryState, Integer count) {
    return client.saveReply(inquiryState.inquiryId(), count)
        .map(v -> inquiryState.withState(FINISHED));
  }
}
