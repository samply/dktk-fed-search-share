package de.samply.dktk.fedsearch.share.service;

import static java.util.Objects.requireNonNull;

import de.samply.dktk.fedsearch.share.Variables;
import de.samply.dktk.fedsearch.share.model.InquiryState;
import de.samply.dktk.fedsearch.share.model.InquiryStateRepository;
import de.samply.dktk.fedsearch.share.util.Either;
import org.camunda.bpm.engine.delegate.JavaDelegate;

abstract class AbstractInquiryStateDelegate implements JavaDelegate {

  final InquiryStateRepository inquiryStateRepository;

  AbstractInquiryStateDelegate(InquiryStateRepository inquiryStateRepository) {
    this.inquiryStateRepository = requireNonNull(inquiryStateRepository);
  }

  Either<String, InquiryState> getInquiryState(Variables variables) {
    return variables.getInquiryStateId()
        .flatMap(id -> Either.fromOptional(inquiryStateRepository.findById(id),
            "missing inquiry-state with id %d".formatted(id)));
  }
}
