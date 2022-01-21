package de.samply.dktk_fed_search.share;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigTest {

  private Config config;

  @BeforeEach
  void setUp() {
    config = new Config(new ObjectMapper(), FhirContext.forR4());
  }

  @Test
  void readMappings() {
    var mappings = config.readMappings();

    System.out.println(mappings);
  }

  @Test
  void readConceptTree() {
    var TermCodeNode = config.readConceptTree();

    System.out.println(TermCodeNode);
  }
}
