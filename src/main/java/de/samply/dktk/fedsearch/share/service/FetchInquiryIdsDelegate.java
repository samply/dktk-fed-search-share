package de.samply.dktk.fedsearch.share.service;

import static java.util.Objects.requireNonNull;

import de.samply.dktk.fedsearch.share.BrokerClient;
import de.samply.dktk.fedsearch.share.Variables;
import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fetches the list of inquiry Ids from the broker.
 */
@Component
public class FetchInquiryIdsDelegate implements JavaDelegate {

  private static final Logger logger = LoggerFactory.getLogger(FetchInquiryIdsDelegate.class);

  private final BrokerClient client;

  public FetchInquiryIdsDelegate(BrokerClient client) {
    this.client = requireNonNull(client);
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    logger.info("Fetch new inquiry ids from broker...");
    long start = System.nanoTime();
    List<String> inquiryIds = client.fetchNewInquiryIds().orElseThrow(Exception::new);
    logger.info("Got {} new inquiry id(s) from broker in {} ms.", inquiryIds.size(),
        ((int) (((double) (System.nanoTime() - start)) / 1000000)));
    Variables.of(execution).setInquiryIds(inquiryIds);
  }
}
