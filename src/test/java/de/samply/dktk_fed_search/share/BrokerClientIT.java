package de.samply.dktk_fed_search.share;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.samply.dktk_fed_search.share.broker.model.Inquiry;
import de.samply.dktk_fed_search.share.util.Either;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SuppressWarnings("NewClassNamingConvention")
class BrokerClientIT {

  private static final String AUTH_TOKEN = "token-131538";
  private static final String STRUCTURED_QUERY = "structured-query-175656";

  private final Network network = Network.newNetwork();

  @Container
  private final PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:9.6")
      .withDatabaseName("searchbroker")
      .withUsername("searchbroker")
      .withPassword("searchbroker")
      .withNetwork(network)
      .withNetworkAliases("postgres")
      .waitingFor(Wait.forListeningPort())
      .withExposedPorts(5432)
      .withStartupAttempts(3);

  @Container
  private final GenericContainer<?> broker = new GenericContainer<>(
      "samply/searchbroker:feature-structureQuery"
      //"searchbroker1"
  )
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .dependsOn(db)
      .withEnv("POSTGRES_HOST", "postgres")
      .withEnv("POSTGRES_DB", "searchbroker")
      .withEnv("POSTGRES_USER", "searchbroker")
      .withEnv("POSTGRES_PASS", "searchbroker")
      .withNetwork(network)
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/broker/rest/health").forStatusCode(200))
      .withStartupAttempts(3);

  private BrokerClient client;

  @BeforeEach
  void setUp() {
    client = new BrokerClient(WebClient.builder()
        .baseUrl(brokerUrl())
        .defaultHeader("Authorization", "Samply " + AUTH_TOKEN)
        .build());
  }

  private String brokerUrl() {
    return "http://localhost:%d/broker/rest/searchbroker".formatted(broker.getFirstMappedPort());
  }

  @Test
  void fetchNewInquiryIds() throws SQLException {
    createOneInquiry();

    List<String> inquiryIds = client.fetchNewInquiryIds();

    assertEquals(List.of("1"), inquiryIds);
  }

  @Test
  void fetchNewInquiryIds_unauthorized() {
    var client = new BrokerClient(WebClient.builder()
        .baseUrl(brokerUrl())
        .build());

    var error = assertThrows(Exception.class, client::fetchNewInquiryIds);

    assertEquals("Unauthorized", error.getMessage());
  }

  @Test
  void fetchInquiry() throws SQLException {
    createOneInquiry();

    Inquiry inquiry = client.fetchInquiry("1");

    assertEquals("1", inquiry.getId());
    assertEquals(Optional.of(STRUCTURED_QUERY), inquiry.getStructuredQuery());
  }

  private void createOneInquiry() throws SQLException {
    var authTokenId = performInsert(
        "insert into samply.authtoken (value) values ('" + AUTH_TOKEN + "')");
    var bankId = performInsert(
        "insert into samply.bank (email, authtoken_id) values ('foo@bar.de', %d)".formatted(
            authTokenId));
    var siteId = performInsert("insert into samply.site (name) values ('foo')");
    performInsert(
        "insert into samply.bank_site (bank_id, site_id, approved) values (%d, %d, true)".formatted(
            bankId, siteId
        ));
    var authorId = performInsert("insert into samply.user (username, authid) values ('foo', 1)");
    var inquiryId = performInsert(
        "insert into samply.inquiry (author_id, status, revision) values (%d, 'IS_RELEASED', 1)".formatted(
            authorId));
    performInsert(
        "insert into samply.inquiry_site (inquiry_id, site_id) values (%d, %d)".formatted(inquiryId,
            siteId));
    performInsert("""
        INSERT INTO samply.inquiry_criteria (inquiry_id, criteria, type)
        VALUES (%d, '%s', 'IC_STRUCTURED_QUERY')
        """.formatted(inquiryId, STRUCTURED_QUERY));
  }

  private int performInsert(String sql) throws SQLException {
    DataSource ds = getDataSource();
    Statement statement = ds.getConnection().createStatement();
    statement.execute(sql, RETURN_GENERATED_KEYS);
    var keys = statement.getGeneratedKeys();
    keys.next();
    return keys.getInt(1);
  }

  private DataSource getDataSource() {
    var dataSourceBuilder = DataSourceBuilder.create();
    System.out.println(db.getDriverClassName());
    dataSourceBuilder.driverClassName(db.getDriverClassName());
    dataSourceBuilder.url(db.getJdbcUrl());
    dataSourceBuilder.username(db.getUsername());
    dataSourceBuilder.password(db.getPassword());
    return dataSourceBuilder.build();
  }
}
