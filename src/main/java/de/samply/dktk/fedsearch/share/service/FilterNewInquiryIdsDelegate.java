package de.samply.dktk.fedsearch.share.service;

import static java.util.Objects.requireNonNull;

import de.samply.dktk.fedsearch.share.Variables;
import de.samply.dktk.fedsearch.share.model.InquiryState;
import de.samply.dktk.fedsearch.share.model.InquiryStateRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Filters the list of inquiry ids reported by the broker for new ones and safes them as in-progress
 * inquiry-states.
 */
@Component
public class FilterNewInquiryIdsDelegate implements JavaDelegate {

  private static final Logger logger = LoggerFactory.getLogger(FilterNewInquiryIdsDelegate.class);

  private final InquiryStateRepository inquiryStateRepository;

  public FilterNewInquiryIdsDelegate(InquiryStateRepository inquiryStateRepository) {
    this.inquiryStateRepository = requireNonNull(inquiryStateRepository);
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    var variables = Variables.of(execution);
    var inquiryIds = variables.getInquiryIds().orElseThrow(Exception::new);
    var knownInquiryIds = knownInquiryIds(inquiryIds);
    var newInquiryStates = inquiryIds.stream()
        .filter(id -> !knownInquiryIds.contains(id))
        .map(InquiryState::inProgress)
        .toList();
    logger.info("found {} new inquiries to process", newInquiryStates.size());
    var newInquiryStateIds = saveAllInquiries(newInquiryStates).map(InquiryState::id).toList();
    variables.setNewInquiryStateIds(newInquiryStateIds);
  }

  private Set<String> knownInquiryIds(List<String> inquiryIds) {
    return inquiryStateRepository.findByInquiryIdIn(inquiryIds).stream()
        .map(InquiryState::inquiryId)
        .collect(Collectors.toUnmodifiableSet());
  }

  private Stream<InquiryState> saveAllInquiries(List<InquiryState> newInquiries) {
    return StreamSupport.stream(inquiryStateRepository.saveAll(newInquiries).spliterator(), false);
  }
}
