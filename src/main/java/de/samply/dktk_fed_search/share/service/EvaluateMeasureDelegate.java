package de.samply.dktk_fed_search.share.service;

import de.samply.dktk_fed_search.share.FhirClient;
import de.samply.dktk_fed_search.share.Variables;
import java.util.Objects;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Takes a Measure URI from {@link Variables#getMeasureUri() variables} and evaluates that Measure
 * and puts the count into variables.
 */
@Component
public class EvaluateMeasureDelegate implements JavaDelegate {

  private static final Logger logger = LoggerFactory.getLogger(EvaluateMeasureDelegate.class);

  private final FhirClient client;

  public EvaluateMeasureDelegate(FhirClient client) {
    this.client = Objects.requireNonNull(client);
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    var count = Variables.of(execution).getMeasureUri()
        .flatMap(client::evaluateMeasure)
        .map(report -> report.getGroupFirstRep().getPopulationFirstRep().getCount())
        .orElseThrow(Exception::new);
    System.out.println("count = " + count);
    Variables.of(execution).setCount(count);
  }
}
