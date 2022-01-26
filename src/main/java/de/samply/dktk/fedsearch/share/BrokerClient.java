package de.samply.dktk.fedsearch.share;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

import de.samply.dktk.fedsearch.share.broker.model.Inquiry;
import de.samply.dktk.fedsearch.share.broker.model.InquiryIds;
import de.samply.dktk.fedsearch.share.broker.model.Reply;
import de.samply.dktk.fedsearch.share.util.Either;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * A client for the REST API of the Searchbroker.
 */
@Component
@SuppressWarnings("ClassCanBeRecord")
public class BrokerClient {

  private static final Logger logger = LoggerFactory.getLogger(BrokerClient.class);

  private final WebClient client;
  private final String mail;

  public BrokerClient(WebClient client, @Value("${app.broker.mail}") String mail) {
    this.client = requireNonNull(client);
    this.mail = requireNonNull(mail);
  }

  private static <T> Either<String, T> block(Mono<T> mono) {
    return Either.tryGet(mono::block).mapLeft(Exception::getMessage);
  }

  /**
   * Fetches the list of new inquiry ids from the broker.
   *
   * @return either the list of new inquiry ids or an error message
   */
  public Either<String, List<String>> fetchNewInquiryIds() {
    return block(fetchNewInquiryIdsMono());
  }

  private Mono<List<String>> fetchNewInquiryIdsMono() {
    return client.get()
        .uri("/inquiries")
        .accept(APPLICATION_XML)
        .exchangeToMono(response -> switch (response.statusCode()) {
          case OK -> response.bodyToMono(InquiryIds.class).map(InquiryIds::getIds);
          case UNAUTHORIZED -> Mono.error(new RuntimeException("Unauthorized"));
          default -> response.createException().flatMap(Mono::error);
        });
  }

  /**
   * Fetches the inquiry with {@code id} from the broker.
   *
   * @param id the identifier of the inquiry to fetch
   * @return either the inquiry with {@code id} or an error message
   */
  public Either<String, Inquiry> fetchInquiry(String id) {
    logger.info("Fetch inquiry with id {} from broker...", id);
    return block(fetchInquiryMono(id));
  }

  private Mono<Inquiry> fetchInquiryMono(String id) {
    return client.get()
        .uri("/inquiries/{id}", id)
        .accept(APPLICATION_XML)
        .header("query-language", "STRUCTURED_QUERY")
        .exchangeToMono(response -> switch (response.statusCode()) {
          case OK -> response.bodyToMono(Inquiry.class);
          case UNAUTHORIZED -> Mono.error(new RuntimeException("Unauthorized"));
          default -> response.createException().flatMap(Mono::error);
        });
  }

  /**
   * Saves a reply with the given {@code donorCount} for the inquiry with the given {@code
   * inquiryId} to the broker.
   *
   * @param inquiryId  the id of the inquiry for which the reply should be saved
   * @param donorCount the count of donors to save
   * @return either nothing or an error message
   */
  public Either<String, Void> saveReply(String inquiryId, int donorCount) {
    logger.info("Save reply with donor count {} for inquiry with id {} to broker...", donorCount,
        inquiryId);
    return block(safeReplyMono(inquiryId, donorCount));
  }

  private Mono<Void> safeReplyMono(String inquiryId, int donorCount) {
    return client.put()
        .uri("/inquiries/{id}/replies/{mail}", inquiryId, mail)
        .contentType(APPLICATION_JSON)
        .bodyValue(Reply.of(donorCount))
        .exchangeToMono(response -> switch (response.statusCode()) {
          case OK -> Mono.empty();
          case UNAUTHORIZED -> Mono.error(new RuntimeException("Unauthorized"));
          default -> response.createException().flatMap(Mono::error);
        });
  }
}
