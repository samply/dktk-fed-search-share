package de.samply.dktk_fed_search.share.service;

import de.samply.dktk_fed_search.share.BrokerClient;
import de.samply.dktk_fed_search.share.Variables;
import java.util.Objects;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Takes a count from {@link Variables#getCount()  variables} and reports it back to the connector.
 */
@Component
public class ReportCountDelegate implements JavaDelegate {

  private static final Logger logger = LoggerFactory.getLogger(ReportCountDelegate.class);

  private final BrokerClient client;

  public ReportCountDelegate(BrokerClient client) {
    this.client = Objects.requireNonNull(client);
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    Variables.of(execution).getCount()
        .flatMap(client::reportCount)
        .orElseThrow(Exception::new);
  }
}
