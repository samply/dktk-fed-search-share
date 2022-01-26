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
import de.samply.dktk.fedsearch.share.util.Either;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FetchInquiryDelegateTest {

  private static final String ID = "175444";
  private static final String STRUCTURED_QUERY = "structured-query-175526";
  private static final String ERROR_MSG = "error-msg-135809";

  @Mock
  private BrokerClient client;

  @InjectMocks
  private FetchInquiryDelegate delegate;

  @Test
  void execute() throws Exception {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable(Variables.INQUIRY_ID)).thenReturn(ID);
    when(client.fetchInquiry(ID)).thenReturn(Either.right(Inquiry.of(ID, STRUCTURED_QUERY)));

    delegate.execute(execution);

    verify(execution).setVariable(Variables.STRUCTURED_QUERY, STRUCTURED_QUERY);
  }

  @Test
  void execute_WithFetchInquiryError() {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable(Variables.INQUIRY_ID)).thenReturn(ID);
    when(client.fetchInquiry(ID)).thenReturn(Either.left(ERROR_MSG));

    var error = assertThrows(Exception.class, () -> delegate.execute(execution));

    assertEquals(ERROR_MSG, error.getMessage());
    verify(execution, never()).setVariable(Variables.STRUCTURED_QUERY, STRUCTURED_QUERY);
  }

  @Test
  void execute_InquiryWithoutStructuredQuery() {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable(Variables.INQUIRY_ID)).thenReturn(ID);
    when(client.fetchInquiry(ID)).thenReturn(Either.right(Inquiry.of(ID)));

    var error = assertThrows(Exception.class, () -> delegate.execute(execution));

    assertEquals("missing structured query in inquiry with id `%s`".formatted(ID),
        error.getMessage());
    verify(execution, never()).setVariable(Variables.STRUCTURED_QUERY, STRUCTURED_QUERY);
  }
}
