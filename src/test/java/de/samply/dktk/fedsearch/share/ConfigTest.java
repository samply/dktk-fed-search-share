package de.samply.dktk.fedsearch.share;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigTest {

  private Config config;

  @BeforeEach
  void setUp() {
    config = new Config(new ObjectMapper(), new FhirParser(FhirContext.forR4()));
  }

  @Test
  void readMappings() {
    var mappings = config.mappings();

    assertEquals(830, mappings.size());
  }

  @Test
  void readConceptTree() {
    var termCodeNode = config.conceptTree();

    assertTrue(termCodeNode.getTermCode().getCode().isEmpty());
  }
}
