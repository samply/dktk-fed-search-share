package de.samply.dktk.fedsearch.share.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.sq2cql.Translator;
import de.numcodex.sq2cql.model.cql.Library;
import de.numcodex.sq2cql.model.structured_query.StructuredQuery;
import de.samply.dktk.fedsearch.share.Config;
import de.samply.dktk.fedsearch.share.FhirClient;
import de.samply.dktk.fedsearch.share.FhirParser;
import de.samply.dktk.fedsearch.share.Reader;
import de.samply.dktk.fedsearch.share.Variables;
import de.samply.dktk.fedsearch.share.util.Either;
import java.util.List;
import java.util.function.Supplier;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateMeasureResourcesDelegateTest {

  private static final String STRUCTURED_QUERY_STRING = "structured-query-174912";
  private static final StructuredQuery STRUCTURED_QUERY = StructuredQuery.of(List.of());
  private static final Library LIBRARY = Library.of();
  private static final String LIBRARY_URI = "library-uri-140816";
  private static final String MEASURE_URI = "measure-uri-140816";
  private static final String ERROR_MSG = "error-msg-142556";

  @Mock
  private Reader<StructuredQuery> structuredQueryReader;

  @Mock
  private Translator translator;

  @Mock
  private Supplier<String> randomUriSupplier;

  @Mock
  private FhirClient fhirClient;

  private CreateMeasureResourcesDelegate delegate;

  @BeforeEach
  void setUp() {
    var config = new Config(new ObjectMapper(), new FhirParser(FhirContext.forR4()));
    delegate = new CreateMeasureResourcesDelegate(structuredQueryReader, translator,
        randomUriSupplier, fhirClient, config);
  }

  @Test
  void execute() throws Exception {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable(Variables.STRUCTURED_QUERY)).thenReturn(STRUCTURED_QUERY_STRING);
    when(structuredQueryReader.readValue(STRUCTURED_QUERY_STRING)).thenReturn(
        Either.right(STRUCTURED_QUERY));
    when(translator.toCql(STRUCTURED_QUERY)).thenReturn(LIBRARY);
    when(randomUriSupplier.get()).thenReturn(LIBRARY_URI, MEASURE_URI);
    when(fhirClient.transact(any(Bundle.class))).thenReturn(Either.right(new Bundle()));

    delegate.execute(execution);

    verify(execution).setVariable(Variables.MEASURE_URI, MEASURE_URI);
  }

  @Test
  void execute_transactionError() {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable(Variables.STRUCTURED_QUERY)).thenReturn(STRUCTURED_QUERY_STRING);
    when(structuredQueryReader.readValue(STRUCTURED_QUERY_STRING)).thenReturn(
        Either.right(STRUCTURED_QUERY));
    when(translator.toCql(STRUCTURED_QUERY)).thenReturn(LIBRARY);
    when(randomUriSupplier.get()).thenReturn(LIBRARY_URI, MEASURE_URI);
    when(fhirClient.transact(any(Bundle.class))).thenReturn(Either.left(ERROR_MSG));

    var error = assertThrows(Exception.class, () -> delegate.execute(execution));

    assertEquals(ERROR_MSG, error.getMessage());
    verify(execution, never()).setVariable(Variables.MEASURE_URI, MEASURE_URI);
  }
}
