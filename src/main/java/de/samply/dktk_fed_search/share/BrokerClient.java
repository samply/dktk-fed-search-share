package de.samply.dktk_fed_search.share;

import static org.springframework.http.MediaType.APPLICATION_XML;

import de.samply.dktk_fed_search.share.broker.model.Inquiry;
import de.samply.dktk_fed_search.share.broker.model.InquiryIds;
import de.samply.dktk_fed_search.share.broker.model.InquiryId;
import de.samply.dktk_fed_search.share.util.Either;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class BrokerClient {

  private static final Logger logger = LoggerFactory.getLogger(BrokerClient.class);

  private final WebClient client;

  public BrokerClient(WebClient client) {
    this.client = Objects.requireNonNull(client);
  }

  /**
   * Fetches the list of new inquiry ids from the broker.
   *
   * @return the list of new inquiry ids
   * @throws RuntimeException on any problem
   */
  public List<String> fetchNewInquiryIds() {
    return client.get()
        .uri("/inquiries")
        .accept(APPLICATION_XML)
        .exchangeToMono(response -> switch (response.statusCode()) {
          case OK -> response.bodyToMono(InquiryIds.class)
              .map(inquiryIds -> inquiryIds.inquiryIds.stream().map(InquiryId::getId).collect(
                  Collectors.toList()));
          case UNAUTHORIZED -> Mono.error(new RuntimeException("Unauthorized"));
          default -> response.createException().flatMap(Mono::error);
        })
        .blockOptional()
        .orElse(List.of());
  }

  /**
   * Fetches the inquiry with {@code id} from the broker.
   *
   * @param id the identifier of the inquiry to fetch
   * @return the inquiry with {@code id}
   * @throws RuntimeException on any problem
   */
  public Inquiry fetchInquiry(String id) {
    logger.info("Fetch inquiry with id {} from broker...", id);
    return client.get()
        .uri("/inquiries/{id}", id)
        .accept(APPLICATION_XML)
        .header("query-language", "STRUCTURED_QUERY")
        .exchangeToMono(response -> switch (response.statusCode()) {
          case OK -> response.bodyToMono(Inquiry.class);
          case UNAUTHORIZED -> Mono.error(new RuntimeException("Unauthorized"));
          default -> response.createException().flatMap(Mono::error);
        })
        .block();
  }

  public Either<String, Void> reportCount(Integer count) {
    logger.info("Report count {}.", count);
    return Either.right();
  }
}
