package de.samply.dktk.fedsearch.share;

import static de.samply.dktk.fedsearch.share.ClasspathIo.slurp;
import static de.samply.dktk.fedsearch.share.TestUtil.brokerBaseUrl;
import static de.samply.dktk.fedsearch.share.TestUtil.createOneInquiry;
import static de.samply.dktk.fedsearch.share.TestUtil.storeBaseUrl;

import de.samply.dktk.fedsearch.share.broker.model.Reply;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@DirtiesContext
class DktkFedSearchApplicationTest {

  private static final Logger logger = LoggerFactory.getLogger(
      DktkFedSearchApplicationTest.class);

  private static final String AUTH_TOKEN = "token-131538";
  private static final String MAIL = "foo@bar.de";

  private static final Network brokerNetwork = Network.newNetwork();

  @Container
  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> brokerDb = new PostgreSQLContainer<>("postgres:9.6")
      .withDatabaseName("searchbroker")
      .withUsername("searchbroker")
      .withPassword("searchbroker")
      .withNetwork(brokerNetwork)
      .withNetworkAliases("postgres")
      .waitingFor(Wait.forListeningPort())
      .withExposedPorts(5432);

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> broker = new GenericContainer<>(
      "samply/searchbroker:develop")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .dependsOn(brokerDb)
      .withEnv("POSTGRES_HOST", "postgres")
      .withEnv("POSTGRES_DB", "searchbroker")
      .withEnv("POSTGRES_USER", "searchbroker")
      .withEnv("POSTGRES_PASS", "searchbroker")
      .withNetwork(brokerNetwork)
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/broker/rest/health").forStatusCode(200))
      .withLogConsumer(new Slf4jLogConsumer(logger));

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> store = new GenericContainer<>("samply/blaze:0.17")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .withEnv("LOG_LEVEL", "debug")
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/health").forStatusCode(200));

  private DataSource brokerDataSource;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("app.broker.baseUrl", () -> brokerBaseUrl(broker));
    registry.add("app.broker.authToken", () -> AUTH_TOKEN);
    registry.add("app.broker.mail", () -> MAIL);
    registry.add("app.store.baseUrl", () -> storeBaseUrl(store));
  }

  @BeforeEach
  void setUp() {
    brokerDataSource = TestUtil.createDataSource(brokerDb);
  }

  @Test
  void testOneInquiry() throws Exception {
    TestUtil.fillStore(store);
    createOneInquiry(brokerDataSource, AUTH_TOKEN, MAIL, slurp("structured-query.json")
        .orElseThrow(Exception::new));

    Thread.sleep(10000);

    TestUtil.assertReplyEquals(Reply.of(1), brokerDataSource);
  }
}
