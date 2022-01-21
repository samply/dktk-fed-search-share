package de.samply.dktk_fed_search.share.broker.model;

import java.util.List;
import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Inquiries")
public class InquiryIds {

  @XmlElement(name = "Inquiry")
  public List<InquiryId> inquiryIds;

  @Override
  public String toString() {
    return new StringJoiner(", ", InquiryIds.class.getSimpleName() + "[", "]")
        .add("inquiryIds=" + inquiryIds)
        .toString();
  }
}
