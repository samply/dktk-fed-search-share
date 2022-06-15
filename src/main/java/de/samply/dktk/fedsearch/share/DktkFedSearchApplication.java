package de.samply.dktk.fedsearch.share;

import static de.samply.dktk.fedsearch.share.Collectors.first;
import static java.util.stream.Collectors.groupingBy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RequestFormatParamStyleEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.sq2cql.Translator;
import de.numcodex.sq2cql.model.Mapping;
import de.numcodex.sq2cql.model.MappingContext;
import de.numcodex.sq2cql.model.TermCodeNode;
import de.numcodex.sq2cql.model.structured_query.StructuredQuery;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Main Application Entrypoint.
 */
@SpringBootApplication
@EnableProcessApplication
public class DktkFedSearchApplication {

  @Value("${app.broker.baseUrl}")
  private String brokerBaseUrl;

  @Value("${app.broker.authToken}")
  private String brokerAuthToken;

  @Value("${app.store.baseUrl}")
  private String storeBaseUrl;

  @Value("${app.store.socketTimeout}")
  private int storeSocketTimeout;

  public static void main(String... args) {
    SpringApplication.run(DktkFedSearchApplication.class, args);
  }

  @Bean
  public Translator cqlTranslator(MappingContext mappingContext) {
    return Translator.of(mappingContext);
  }

  /**
   * Creates the mapping context used in the CQL translation.
   *
   * @param mappings    the CQl translation mappings
   * @param conceptTree the CQL translation concept tree
   * @return the mapping context
   */
  @Bean
  public MappingContext mappingContext(List<Mapping> mappings, TermCodeNode conceptTree) {
    var mappingMap = mappings.stream().collect(groupingBy(Mapping::key, first()));
    return MappingContext.of(mappingMap, conceptTree,
        Map.of("http://fhir.de/CodeSystem/dimdi/icd-10-gm", "icd_10_gm",
            "http://loinc.org", "loinc",
            "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/VitalstatusCS", "vital_status",
            "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/GradingCS", "grading",
            "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/UiccstadiumCS", "uicc",
            "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/TNMTCS", "tnm_t",
            "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/TNMNCS", "tnm_n",
            "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/TNMMCS", "tnm_m",
            "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/TNMySymbolCS", "tnm_y"));
  }

  /**
   * Creates a JSON reader for structured queries.
   *
   * @param jsonMapper the JSON object mapper from Jackson
   * @return the JSON reader
   */
  @Bean
  public Reader<StructuredQuery> structuredQueryReader(ObjectMapper jsonMapper) {
    return new Reader<>(jsonMapper.readerFor(StructuredQuery.class));
  }

  @Bean
  public Supplier<String> randomUriSupplier() {
    return () -> "urn:uuid" + UUID.randomUUID();
  }

  @Bean
  public FhirContext fhirContext() {
    return FhirContext.forR4();
  }

  /**
   * Creates the HAPI FHIR client for the communication with the FHIR server were the queries are
   * executed.
   *
   * @param context the HAPI FHIR context
   * @return the HAPI FHIR client
   */
  @Bean
  public IGenericClient storeClient(FhirContext context) {
    context.getRestfulClientFactory().setSocketTimeout(storeSocketTimeout);
    var client = context.newRestfulGenericClient(storeBaseUrl);
    client.setFormatParamStyle(RequestFormatParamStyleEnum.NONE);
    return client;
  }

  /**
   * Creates the Spring client for the communication with the Searchbroker.
   *
   * @return the Spring client
   */
  @Bean
  public WebClient brokerWebClient() {
    HttpClient httpClient = HttpClient.create().proxyWithSystemProperties();
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .baseUrl(brokerBaseUrl)
        .defaultHeader("Authorization", "Samply " + brokerAuthToken)
        .build();
  }
}
