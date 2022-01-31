package de.samply.dktk.fedsearch.share.model;

import org.springframework.data.annotation.Id;

/**
 * The state of processing an inquiry.
 */
public record InquiryState(@Id Long id, String inquiryId, State state) {

  public static InquiryState inProgress(String inquiryId) {
    return new InquiryState(null, inquiryId, State.IN_PROGRESS);
  }

  InquiryState withId(Long id) {
    return new InquiryState(id, inquiryId, state);
  }

  /**
   * Returns a new inquiry state as copy of this inquiry state with {@code state} applied.
   *
   * @param state the state of the newly returned inquiry state
   * @return a copy of this inquiry state with {@code state} applied
   */
  public InquiryState withState(State state) {
    return new InquiryState(id, inquiryId, state);
  }

  /**
   * The state of an inquiry state.
   */
  public enum State {
    IN_PROGRESS, FINISHED
  }
}
