package de.samply.dktk_fed_search.share;

import de.samply.dktk_fed_search.share.util.Either;
import java.util.Objects;
import org.camunda.bpm.engine.delegate.VariableScope;

public final class Variables {

  public static final String STRUCTURED_QUERY = "structured-query";
  public static final String MEASURE_URI = "measure-uri";

  private final VariableScope scope;

  private Variables(VariableScope scope) {
    this.scope = Objects.requireNonNull(scope);
  }

  public static Variables of(VariableScope scope) {
    return new Variables(scope);
  }

  public Either<String, String> getStructuredQuery() {
    var query = (String) scope.getVariable(STRUCTURED_QUERY);
    return query == null ? Either.left("missing process var `structured-query`")
        : Either.right(query);
  }

  public String getMeasureUri() {
    return (String) scope.getVariable(MEASURE_URI);
  }

  public void setMeasureUri(String uri) {
    scope.setVariable(MEASURE_URI, uri);
  }
}
