package de.samply.dktk.fedsearch.share;

import static de.samply.dktk.fedsearch.share.TestUtil.brokerBaseUrl;
import static de.samply.dktk.fedsearch.share.TestUtil.createOneInquiry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.samply.dktk.fedsearch.share.broker.model.Inquiry;
import de.samply.dktk.fedsearch.share.broker.model.Reply;
import de.samply.dktk.fedsearch.share.util.Either;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class BrokerClientTest {

  private static final String AUTH_TOKEN = "token-131538";
  private static final String STRUCTURED_QUERY = "structured-query-175656";
  private static final String MAIL = "foo@bar.de";
  private static final int COUNT = 95358;
  private static final String INQUIRY_ID = "1";

  private final Network network = Network.newNetwork();

  @Container
  @SuppressWarnings("resource")
  private final PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:9.6")
      .withDatabaseName("searchbroker")
      .withUsername("searchbroker")
      .withPassword("searchbroker")
      .withNetwork(network)
      .withNetworkAliases("postgres")
      .waitingFor(Wait.forListeningPort())
      .withExposedPorts(5432);

  @Container
  @SuppressWarnings("resource")
  private final GenericContainer<?> broker = new GenericContainer<>(
      "samply/searchbroker:develop")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .dependsOn(db)
      .withEnv("POSTGRES_HOST", "postgres")
      .withEnv("POSTGRES_DB", "searchbroker")
      .withEnv("POSTGRES_USER", "searchbroker")
      .withEnv("POSTGRES_PASS", "searchbroker")
      .withNetwork(network)
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/broker/rest/health").forStatusCode(200));

  private DataSource brokerDataSource;
  private BrokerClient client;

  @BeforeEach
  void setUp() {
    brokerDataSource = TestUtil.createDataSource(db);
    client = new BrokerClient(WebClient.builder()
        .baseUrl(brokerBaseUrl(broker))
        .defaultHeader("Authorization", "Samply " + AUTH_TOKEN)
        .build(), MAIL);
  }

  @Test
  void fetchNewInquiryIds() throws Exception {
    createOneInquiry(brokerDataSource, AUTH_TOKEN, MAIL, STRUCTURED_QUERY);

    List<String> inquiryIds = client.fetchNewInquiryIds().orElseThrow(Exception::new);

    assertEquals(List.of(INQUIRY_ID), inquiryIds);
  }

  @Test
  void fetchNewInquiryIds_unauthorized() {
    var client = new BrokerClient(WebClient.builder()
        .baseUrl(brokerBaseUrl(broker))
        .build(), MAIL);

    var error = client.fetchNewInquiryIds();

    assertEquals(Either.left("Unauthorized"), error);
  }

  @Test
  void fetchInquiry() throws Exception {
    createOneInquiry(brokerDataSource, AUTH_TOKEN, MAIL, STRUCTURED_QUERY);

    Inquiry inquiry = client.fetchInquiry(INQUIRY_ID).orElseThrow(Exception::new);

    assertEquals(INQUIRY_ID, inquiry.getId());
    assertEquals(Optional.of(STRUCTURED_QUERY), inquiry.getStructuredQuery());
  }

  @Test
  void safeReplay() throws Exception {
    createOneInquiry(brokerDataSource, AUTH_TOKEN, MAIL, STRUCTURED_QUERY);

    var result = client.saveReply(INQUIRY_ID, COUNT);

    assertTrue(result.isRight());
    TestUtil.assertReplyEquals(Reply.of(COUNT), brokerDataSource);
  }
}
