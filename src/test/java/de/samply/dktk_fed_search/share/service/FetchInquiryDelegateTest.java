package de.samply.dktk_fed_search.share.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.samply.dktk_fed_search.share.BrokerClient;
import de.samply.dktk_fed_search.share.Variables;
import de.samply.dktk_fed_search.share.broker.model.Inquiry;
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

  @Mock
  private BrokerClient client;

  @InjectMocks
  private FetchInquiryDelegate delegate;

  @Test
  void execute() throws Exception {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable(Variables.INQUIRY_ID)).thenReturn(ID);
    when(client.fetchInquiry(ID)).thenReturn(Inquiry.of(ID, STRUCTURED_QUERY));

    delegate.execute(execution);

    verify(execution).setVariable(Variables.STRUCTURED_QUERY, STRUCTURED_QUERY);
  }

  @Test
  void execute_InquiryWithoutStructuredQuery() throws Exception {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable(Variables.INQUIRY_ID)).thenReturn(ID);
    when(client.fetchInquiry(ID)).thenReturn(Inquiry.of(ID));

    var error = assertThrows(Exception.class, () -> delegate.execute(execution));

    assertEquals("missing structured query in inquiry with id `%s`".formatted(ID),
        error.getMessage());
    verify(execution, never()).setVariable(Variables.STRUCTURED_QUERY, STRUCTURED_QUERY);
  }
}
