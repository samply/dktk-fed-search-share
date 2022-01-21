package de.samply.dktk_fed_search.share;

import static de.samply.dktk_fed_search.share.Collectors.first;
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
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.web.reactive.function.client.WebClient;

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

  private final RuntimeService runtimeService;

  public DktkFedSearchApplication(RuntimeService runtimeService) {
    this.runtimeService = runtimeService;
  }

  public static void main(String... args) {
    SpringApplication.run(DktkFedSearchApplication.class, args);
  }

  @EventListener
  public void processPostDeploy(PostDeployEvent event) {
    //runtimeService.startProcessInstanceByKey("DktkFedSearch");
  }

  @Bean
  public Translator translator(MappingContext mappingContext) {
    return Translator.of(mappingContext);
  }

  @Bean
  public MappingContext mappingContext(List<Mapping> mappings, TermCodeNode conceptTree) {
    var mappingMap = mappings.stream().collect(groupingBy(Mapping::getKey, first()));
    return MappingContext.of(mappingMap, conceptTree,
        Map.of("http://fhir.de/CodeSystem/dimdi/icd-10-gm", "icd-10-gm"));
  }

  @Bean
  public Reader<StructuredQuery> structuredQueryReader(ObjectMapper objectMapper) {
    return new Reader<>(objectMapper.readerFor(StructuredQuery.class));
  }

  @Bean
  public Supplier<String> randomUriSupplier() {
    return () -> "urn:uuid" + UUID.randomUUID();
  }

  @Bean
  public FhirContext fhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  public IGenericClient storeClient(FhirContext context) {
    var client = context.newRestfulGenericClient(storeBaseUrl);
    client.setFormatParamStyle(RequestFormatParamStyleEnum.NONE);
    return client;
  }

  @Bean
  public WebClient brokerWebClient() {
    return WebClient.builder()
        .baseUrl(brokerBaseUrl)
        .defaultHeader("Authorization", "Samply " + brokerAuthToken)
        .build();
  }
}
