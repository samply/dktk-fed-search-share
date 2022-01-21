package de.samply.dktk.fedsearch.share;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectReader;
import de.samply.dktk.fedsearch.share.util.Either;

/**
 * A {@link ObjectReader reader} providing an {@link Either} API.
 *
 * @param <T> the type of the value
 */
public class Reader<T> {

  private final ObjectReader reader;

  public Reader(ObjectReader reader) {
    this.reader = requireNonNull(reader);
  }

  /**
   * Reads a value from {@code src}.
   *
   * @param src the string to read
   * @return either the read value or an error message
   */
  public Either<String, T> readValue(String src) {
    try {
      return Either.right(reader.readValue(src));
    } catch (Exception e) {
      return Either.left(e.getMessage());
    }
  }
}
