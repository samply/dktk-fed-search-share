package de.samply.dktk.fedsearch.share.util;

import de.samply.dktk.fedsearch.share.util.Anomaly.Fault;
import de.samply.dktk.fedsearch.share.util.Anomaly.Forbidden;
import de.samply.dktk.fedsearch.share.util.Anomaly.NotFound;

/**
 * Anomalies represent errors in a simple, actionable and generic way.
 *
 * @see <a href="https://github.com/cognitect-labs/anomalies">cognitect.anomalies</a>
 */
public sealed interface Anomaly permits Forbidden, NotFound, Fault {

  /**
   * Returns the message of this anomaly.
   *
   * @return the message
   */
  String msg();

  /**
   * Something was not found, like 404 in HTTP.
   */
  record Forbidden(String msg) implements Anomaly {

  }

  /**
   * Something was not found, like 404 in HTTP.
   */
  record NotFound(String msg) implements Anomaly {

  }

  /**
   * Some general callee bug, like 500 in HTTP.
   */
  record Fault(String msg) implements Anomaly {

  }
}
