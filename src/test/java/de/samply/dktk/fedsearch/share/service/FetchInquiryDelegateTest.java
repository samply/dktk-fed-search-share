package de.samply.dktk.fedsearch.share.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.samply.dktk.fedsearch.share.BrokerClient;
import de.samply.dktk.fedsearch.share.Variables;
import de.samply.dktk.fedsearch.share.broker.model.Inquiry;
import de.samply.dktk.fedsearch.share.model.InquiryState;
import de.samply.dktk.fedsearch.share.model.InquiryStateRepository;
import de.samply.dktk.fedsearch.share.util.Either;
import java.util.Optional;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FetchInquiryDelegateTest {

  private static final long ID = 175444;
  private static final String INQUIRY_ID = "inquiry-id-183704";
  private static final String STRUCTURED_QUERY = "structured-query-175526";
  private static final String ERROR_MSG = "error-msg-135809";

  @Mock
  private BrokerClient client;

  @Mock
  private InquiryStateRepository inquiryStateRepository;

  @InjectMocks
  private FetchInquiryDelegate delegate;

  @Test
  void execute() throws Exception {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable(Variables.INQUIRY_STATE_ID)).thenReturn(ID);
    when(inquiryStateRepository.findById(ID))
        .thenReturn(Optional.of(InquiryState.inProgress(INQUIRY_ID)));
    when(client.fetchInquiry(INQUIRY_ID))
        .thenReturn(Either.right(Inquiry.of(INQUIRY_ID, STRUCTURED_QUERY)));

    delegate.execute(execution);

    verify(execution).setVariable(Variables.STRUCTURED_QUERY, STRUCTURED_QUERY);
  }

  @Test
  void execute_WithFetchInquiryError() {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable(Variables.INQUIRY_STATE_ID)).thenReturn(ID);
    when(inquiryStateRepository.findById(ID))
        .thenReturn(Optional.of(InquiryState.inProgress(INQUIRY_ID)));
    when(client.fetchInquiry(INQUIRY_ID)).thenReturn(Either.left(ERROR_MSG));

    var error = assertThrows(Exception.class, () -> delegate.execute(execution));

    assertEquals(ERROR_MSG, error.getMessage());
    verify(execution, never()).setVariable(Variables.STRUCTURED_QUERY, STRUCTURED_QUERY);
  }

  @Test
  void execute_InquiryWithoutStructuredQuery() {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable(Variables.INQUIRY_STATE_ID)).thenReturn(ID);
    when(inquiryStateRepository.findById(ID))
        .thenReturn(Optional.of(InquiryState.inProgress(INQUIRY_ID)));
    when(client.fetchInquiry(INQUIRY_ID)).thenReturn(Either.right(Inquiry.of(INQUIRY_ID)));

    var error = assertThrows(Exception.class, () -> delegate.execute(execution));

    assertEquals("missing structured query in inquiry with id `%s`".formatted(INQUIRY_ID),
        error.getMessage());
    verify(execution, never()).setVariable(Variables.STRUCTURED_QUERY, STRUCTURED_QUERY);
  }
}
