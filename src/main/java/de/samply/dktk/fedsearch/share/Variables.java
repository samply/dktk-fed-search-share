package de.samply.dktk.fedsearch.share;

import static java.util.Objects.requireNonNull;

import de.samply.dktk.fedsearch.share.util.Either;
import java.util.List;
import org.camunda.bpm.engine.delegate.VariableScope;

/**
 * Utility class to work with Camunda variables.
 */
@SuppressWarnings("ClassCanBeRecord")
public final class Variables {

  public static final String NEW_INQUIRY_IDS = "newInquiryIds";
  public static final String INQUIRY_ID = "inquiryId";
  public static final String STRUCTURED_QUERY = "structuredQuery";
  public static final String MEASURE_URI = "measureUri";
  public static final String COUNT = "count";

  private final VariableScope scope;

  private Variables(VariableScope scope) {
    this.scope = requireNonNull(scope);
  }

  public static Variables of(VariableScope scope) {
    return new Variables(scope);
  }

  public Either<String, String> getStructuredQuery() {
    return getVariable(String.class, STRUCTURED_QUERY);
  }

  public void setStructuredQuery(String structuredQuery) {
    scope.setVariable(STRUCTURED_QUERY, structuredQuery);
  }

  public void setNewInquiryIds(List<String> ids) {
    scope.setVariable(NEW_INQUIRY_IDS, ids);
  }

  private <T> Either<String, T> getVariable(Class<T> type, String name) {
    var variable = type.cast(scope.getVariable(name));
    return variable == null
        ? Either.left("missing process var `%s`".formatted(name))
        : Either.right(variable);
  }

  public Either<String, String> getMeasureUri() {
    return getVariable(String.class, MEASURE_URI);
  }

  public void setMeasureUri(String uri) {
    scope.setVariable(MEASURE_URI, uri);
  }

  public Either<String, Integer> getCount() {
    return getVariable(Integer.class, COUNT);
  }

  public void setCount(Integer count) {
    scope.setVariable(COUNT, count);
  }

  public Either<String, String> getInquiryId() {
    return getVariable(String.class, INQUIRY_ID);
  }
}