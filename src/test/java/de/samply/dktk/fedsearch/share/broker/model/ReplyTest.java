package de.samply.dktk.fedsearch.share.broker.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

class ReplyTest {

  @Test
  void fromJson() throws Exception {
    var reply = new ObjectMapper().readValue("""
        {
          "donor": {
            "count": 102941
          }
        }
        """, Reply.class);

    assertEquals(Reply.of(102941), reply);
  }
}
