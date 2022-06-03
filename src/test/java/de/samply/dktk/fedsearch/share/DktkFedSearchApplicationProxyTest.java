package de.samply.dktk.fedsearch.share;

import static de.samply.dktk.fedsearch.share.ClasspathIo.slurp;
import static de.samply.dktk.fedsearch.share.TestUtil.createOneInquiry;
import static de.samply.dktk.fedsearch.share.TestUtil.storeBaseUrl;

import de.samply.dktk.fedsearch.share.broker.model.Reply;
import java.util.Objects;
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
class DktkFedSearchApplicationProxyTest {

  private static final Logger logger = LoggerFactory.getLogger(
      DktkFedSearchApplicationProxyTest.class);

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
  private static final GenericContainer<?> brokerBackend = new GenericContainer<>(
      "samply/searchbroker:develop")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .dependsOn(brokerDb)
      .withEnv("POSTGRES_HOST", "postgres")
      .withEnv("POSTGRES_DB", "searchbroker")
      .withEnv("POSTGRES_USER", "searchbroker")
      .withEnv("POSTGRES_PASS", "searchbroker")
      .withNetwork(brokerNetwork)
      .withNetworkAliases("broker-backend")
      .waitingFor(Wait.forHttp("/broker/rest/health").forStatusCode(200));

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> broker = new GenericContainer<>(
      "nginx")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .dependsOn(brokerBackend)
      .withFileSystemBind(getPath("nginx.conf"), "/etc/nginx/conf.d/broker.conf")
      .withFileSystemBind(getPath("broker.crt"), "/etc/nginx/broker.crt")
      .withFileSystemBind(getPath("broker.key"), "/etc/nginx/broker.key")
      .withNetwork(brokerNetwork)
      .withNetworkAliases("broker")
      .withLogConsumer(new Slf4jLogConsumer(logger));

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> proxy = new GenericContainer<>(
      "ubuntu/squid:5.2-22.04_beta")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .withNetwork(brokerNetwork)
      .withExposedPorts(3128)
      .waitingFor(Wait.forListeningPort())
      .withLogConsumer(new Slf4jLogConsumer(logger));

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> store = new GenericContainer<>("samply/blaze:0.17")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .withEnv("LOG_LEVEL", "debug")
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/health").forStatusCode(200));


  @SuppressWarnings("SameParameterValue")
  private static String getPath(String name) {
    return Objects.requireNonNull(DktkFedSearchApplicationProxyTest.class.getResource(name))
        .getPath();
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("app.broker.baseUrl", () -> "https://broker/broker/rest/searchbroker");
    registry.add("app.broker.authToken", () -> AUTH_TOKEN);
    registry.add("app.broker.mail", () -> MAIL);
    registry.add("app.store.baseUrl", () -> storeBaseUrl(store));
    System.setProperty("http.proxyHost", proxy.getHost());
    System.setProperty("http.proxyPort", proxy.getFirstMappedPort().toString());
    System.setProperty("javax.net.ssl.trustStore", getPath("ca.jks"));
    System.setProperty("javax.net.ssl.trustStorePassword", "password");
  }

  private DataSource brokerDataSource;

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
