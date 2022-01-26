package de.samply.dktk.fedsearch.share.service;

import static java.util.Objects.requireNonNull;

import de.samply.dktk.fedsearch.share.BrokerClient;
import de.samply.dktk.fedsearch.share.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Takes a count from {@link Variables#getCount() variables} and reports it back to the connector.
 */
@Component
public class SaveReplyDelegate implements JavaDelegate {

  private final BrokerClient client;

  public SaveReplyDelegate(BrokerClient client) {
    this.client = requireNonNull(client);
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    var variables = Variables.of(execution);
    variables.getCount()
        .flatMap(count -> variables.getInquiryId()
            .flatMap(inquiryId -> client.saveReply(inquiryId, count)))
        .orElseThrow(Exception::new);
  }
}
