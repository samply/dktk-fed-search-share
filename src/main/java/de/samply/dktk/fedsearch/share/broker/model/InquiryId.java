package de.samply.dktk.fedsearch.share.broker.model;

import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * One inquiry id.
 */
@XmlRootElement(name = "Inquiry")
public class InquiryId {

  @XmlElement(name = "Id")
  private String id;

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", InquiryId.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .toString();
  }
}
