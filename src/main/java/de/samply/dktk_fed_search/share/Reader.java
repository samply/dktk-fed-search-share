package de.samply.dktk_fed_search.share;

import com.fasterxml.jackson.databind.ObjectReader;
import de.samply.dktk_fed_search.share.util.Either;

public class Reader<T> {

  private final ObjectReader reader;

  public Reader(ObjectReader reader) {
    this.reader = reader;
  }

  public Either<String, T> readValue(String src) {
    try {
      return Either.right(reader.readValue(src));
    } catch (Exception e) {
      return Either.left(e.getMessage());
    }
  }
}
