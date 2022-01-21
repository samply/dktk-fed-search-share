package de.samply.dktk_fed_search.share.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.samply.dktk_fed_search.share.BrokerClient;
import de.samply.dktk_fed_search.share.Variables;
import java.util.List;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FetchNewInquiryIdsDelegateTest {

  private static final List<String> NEW_INQUIRY_IDS = List.of("1");
  private static final String ERROR_MSG = "msg-163104";

  @Mock
  private BrokerClient client;

  @InjectMocks
  private FetchNewInquiryIdsDelegate delegate;

  @Test
  void execute() {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(client.fetchNewInquiryIds()).thenReturn(NEW_INQUIRY_IDS);

    delegate.execute(execution);

    verify(execution).setVariable(Variables.NEW_INQUIRY_IDS, NEW_INQUIRY_IDS);
  }

  @Test
  void execute_error() {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(client.fetchNewInquiryIds()).thenThrow(new RuntimeException(ERROR_MSG));

    var error = assertThrows(Exception.class, () -> delegate.execute(execution));

    assertEquals(ERROR_MSG, error.getMessage());
    verify(execution, never()).setVariable(Variables.NEW_INQUIRY_IDS, NEW_INQUIRY_IDS);
  }
}
