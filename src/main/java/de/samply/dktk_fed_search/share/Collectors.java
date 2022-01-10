package de.samply.dktk_fed_search.share;

import java.util.stream.Collector;

public class Collectors {

  public static <T> Collector<T, ?, T> toSingleton() {
    return java.util.stream.Collectors.collectingAndThen(
        java.util.stream.Collectors.toList(),
        list -> {
          if (list.size() != 1) {
            throw new IllegalStateException();
          }
          return list.get(0);
        }
    );
  }
}
