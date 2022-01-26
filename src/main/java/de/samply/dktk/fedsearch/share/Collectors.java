package de.samply.dktk.fedsearch.share;

import java.util.stream.Collector;

/**
 * Custom collectors for the stream API.
 */
public interface Collectors {

  /**
   * Returns a Collector that accumulates the input elements and returns the first one.
   *
   * @param <T> the type of input elements to the collector.
   * @return a Collector which collects all the input elements and return the first one.
   */
  static <T> Collector<T, ?, T> first() {
    return java.util.stream.Collectors.collectingAndThen(
        java.util.stream.Collectors.toList(),
        list -> list.get(0)
    );
  }
}
