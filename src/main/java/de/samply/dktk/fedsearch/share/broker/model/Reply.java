package de.samply.dktk.fedsearch.share.broker.model;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A search result.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Reply(@JsonProperty("donor") Donor donor) {

  public Reply {
    requireNonNull(donor);
  }

  /**
   * Creates a reply with the given {@code donorCount}.
   *
   * @param donorCount the number of donors
   * @return the reply
   */
  public static Reply of(int donorCount) {
    return new Reply(new Donor(donorCount));
  }

  /**
   * The donor part of the reply.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Donor(@JsonProperty("count") int count) {

  }
}
