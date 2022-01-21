package de.samply.dktk_fed_search.share.broker.model;

import de.samply.dktk_fed_search.share.util.Either;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.camunda.feel.syntaxtree.In;

@XmlRootElement(namespace = "http://schema.samply.de/samply/Inquiry", name = "Inquiry")
public class Inquiry {

  @XmlAttribute(required = true)
  private String id;

  @XmlElement(namespace = "http://schema.samply.de/samply/Inquiry", name = "StructuredQuery")
  private String structuredQuery;

  public static Inquiry of(String id) {
    var inquiry = new Inquiry();
    inquiry.id = Objects.requireNonNull(id);
    return inquiry;
  }

  public static Inquiry of(String id, String structuredQuery) {
    var inquiry = new Inquiry();
    inquiry.id = Objects.requireNonNull(id);
    inquiry.structuredQuery = Objects.requireNonNull(structuredQuery);
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
