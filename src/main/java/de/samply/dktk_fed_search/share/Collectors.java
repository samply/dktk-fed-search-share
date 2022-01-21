package de.samply.dktk_fed_search.share;

import java.util.stream.Collector;

public class Collectors {

  /**
   * Returns a Collector that accumulates the input elements and returns the first one.
   *
   * @param <T> the type of input elements to the collector.
   * @return a Collector which collects all the input elements and return the first one.
   */
  public static <T> Collector<T, ?, T> first() {
    return java.util.stream.Collectors.collectingAndThen(
        java.util.stream.Collectors.toList(),
        list -> list.get(0)
    );
  }
}
