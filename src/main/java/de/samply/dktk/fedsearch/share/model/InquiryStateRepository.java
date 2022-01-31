package de.samply.dktk.fedsearch.share.model;

import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for {@link InquiryState inquiry states}.
 */
public interface InquiryStateRepository extends CrudRepository<InquiryState, Long> {

  /**
   * Loads all {@link InquiryState inquiry states} with inquiry ids from broker.
   *
   * @param inquiryIds the inquiry ids from broker
   * @return a list of {@link InquiryState inquiry states}
   */
  List<InquiryState> findByInquiryIdIn(Collection<String> inquiryIds);
}
