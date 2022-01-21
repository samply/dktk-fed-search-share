package de.samply.dktk.fedsearch.share.broker.model;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A search query.
 */
@XmlRootElement(namespace = "http://schema.samply.de/samply/Inquiry", name = "Inquiry")
public class Inquiry {

  @XmlAttribute(required = true)
  private String id;

  @XmlElement(namespace = "http://schema.samply.de/samply/Inquiry", name = "StructuredQuery")
  private String structuredQuery;

  /**
   * Creates an inquiry with the given {@code id}.
   *
   * @param id the id
   * @return the inquiry
   * @throws NullPointerException if the {@code id} is null
   */
  public static Inquiry of(String id) {
    var inquiry = new Inquiry();
    inquiry.id = requireNonNull(id);
    return inquiry;
  }

  /**
   * Creates an inquiry with the given {@code id} and {@code structuredQuery}.
   *
   * @param id              the id
   * @param structuredQuery the structured query as serialized JSON string
   * @return the inquiry
   * @throws NullPointerException if either the {@code id} or the {@code structuredQuery} is null
   */
  public static Inquiry of(String id, String structuredQuery) {
    var inquiry = new Inquiry();
    inquiry.id = requireNonNull(id);
    inquiry.structuredQuery = requireNonNull(structuredQuery);
    return inquiry;
  }

  public String getId() {
    return id;
  }

  public Optional<String> getStructuredQuery() {
    return Optional.ofNullable(structuredQuery);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Inquiry.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .add("structuredQuery='" + structuredQuery + "'")
        .toString();
  }
}
