package de.samply.dktk_fed_search.share.service;

import de.samply.dktk_fed_search.share.BrokerClient;
import de.samply.dktk_fed_search.share.Variables;
import java.util.List;
import java.util.Objects;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fetches the list of new inquiry Ids from the broker.
 */
@Component
public class FetchNewInquiryIdsDelegate implements JavaDelegate {

  private static final Logger logger = LoggerFactory.getLogger(FetchNewInquiryIdsDelegate.class);

  private final BrokerClient client;

  public FetchNewInquiryIdsDelegate(BrokerClient client) {
    this.client = Objects.requireNonNull(client);
  }

  @Override
  public void execute(DelegateExecution execution) {
    logger.info("Fetch new inquiry ids from broker...");
    long start = System.nanoTime();
    List<String> inquiryIds = client.fetchNewInquiryIds();
    logger.info("Got {} new inquiry id(s) from broker in {} ms.", inquiryIds.size(),
        ((double) (System.nanoTime() - start)) / 1000000);
    Variables.of(execution).setNewInquiryIds(inquiryIds);
  }
}
