package de.samply.dktk.fedsearch.share.broker.model;

import java.util.List;
import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A list of inquiry ids.
 */
@XmlRootElement(name = "Inquiries")
public class InquiryIds {

  @XmlElement(name = "Inquiry")
  public List<InquiryId> inquiryIds = List.of();

  @Override
  public String toString() {
    return new StringJoiner(", ", InquiryIds.class.getSimpleName() + "[", "]")
        .add("inquiryIds=" + inquiryIds)
        .toString();
  }

  public List<String> getIds() {
    return inquiryIds.stream().map(InquiryId::getId).toList();
  }
}
