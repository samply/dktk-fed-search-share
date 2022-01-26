package de.samply.dktk.fedsearch.share;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.samply.dktk.fedsearch.share.broker.model.Inquiry;
import de.samply.dktk.fedsearch.share.broker.model.Reply;
import de.samply.dktk.fedsearch.share.util.Either;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectReader;

@Testcontainers
@SuppressWarnings("NewClassNamingConvention")
class BrokerClientIT {

  private static final String AUTH_TOKEN = "token-131538";
  private static final String STRUCTURED_QUERY = "structured-query-175656";
  private static final String MAIL = "foo@bar.de";
  private static final int COUNT = 95358;
  private static final String INQUIRY_ID = "1";

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
      "samply/searchbroker:feature-structureQuery")
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
  private ObjectReader replyReader;

  @BeforeEach
  void setUp() {
    client = new BrokerClient(WebClient.builder()
        .baseUrl(brokerUrl())
        .defaultHeader("Authorization", "Samply " + AUTH_TOKEN)
        .build(), MAIL);
    replyReader = new ObjectMapper().readerFor(Reply.class);
  }

  private String brokerUrl() {
    return "http://localhost:%d/broker/rest/searchbroker".formatted(broker.getFirstMappedPort());
  }

  @Test
  void fetchNewInquiryIds() throws Exception {
    createOneInquiry();

    List<String> inquiryIds = client.fetchNewInquiryIds().orElseThrow(Exception::new);

    assertEquals(List.of(INQUIRY_ID), inquiryIds);
  }

  @Test
  void fetchNewInquiryIds_unauthorized() {
    var client = new BrokerClient(WebClient.builder()
        .baseUrl(brokerUrl())
        .build(), MAIL);

    var error = client.fetchNewInquiryIds();

    assertEquals(Either.left("Unauthorized"), error);
  }

  @Test
  void fetchInquiry() throws Exception {
    createOneInquiry();

    Inquiry inquiry = client.fetchInquiry(INQUIRY_ID).orElseThrow(Exception::new);

    assertEquals(INQUIRY_ID, inquiry.getId());
    assertEquals(Optional.of(STRUCTURED_QUERY), inquiry.getStructuredQuery());
  }

  @Test
  void safeReplay() throws Exception {
    createOneInquiry();

    var result = client.saveReply(INQUIRY_ID, COUNT);

    assertTrue(result.isRight());
    var rs = performQuery("select * from samply.reply");
    assertTrue(rs.next());
    assertEquals(Reply.of(COUNT), replyReader.readValue(rs.getString("content")));
  }

  private void createOneInquiry() throws SQLException {
    var ds = getDataSource();
    var authTokenId = performInsert(ds,
        "insert into samply.authtoken (value) values ('" + AUTH_TOKEN + "')");
    var bankId = performInsert(ds,
        ("insert into samply.bank (email, authtoken_id) values ('" + MAIL + "', %d)").formatted(
            authTokenId));
    var siteId = performInsert(ds, "insert into samply.site (name) values ('foo')");
    performInsert(ds,
        "insert into samply.bank_site (bank_id, site_id, approved) values (%d, %d, true)".formatted(
            bankId, siteId
        ));
    var authorId = performInsert(ds,
        "insert into samply.user (username, authid) values ('foo', 1)");
    var inquiryId = performInsert(ds,
        "insert into samply.inquiry (author_id, status, revision) values (%d, 'IS_RELEASED', 1)".formatted(
            authorId));
    performInsert(ds,
        "insert into samply.inquiry_site (inquiry_id, site_id) values (%d, %d)".formatted(inquiryId,
            siteId));
    performInsert(ds, """
        INSERT INTO samply.inquiry_criteria (inquiry_id, criteria, type)
        VALUES (%d, '%s', 'IC_STRUCTURED_QUERY')
        """.formatted(inquiryId, STRUCTURED_QUERY));
  }

  private ResultSet performQuery(String query) throws SQLException {
    var ds = getDataSource();
    var statement = ds.getConnection().createStatement();
    statement.execute(query);
    return statement.getResultSet();
  }

  private int performInsert(DataSource ds, String sql) throws SQLException {
    var statement = ds.getConnection().createStatement();
    statement.execute(sql, RETURN_GENERATED_KEYS);
    var keys = statement.getGeneratedKeys();
    keys.next();
    return keys.getInt(1);
  }

  private DataSource getDataSource() {
    var dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName(db.getDriverClassName());
    dataSourceBuilder.url(db.getJdbcUrl());
    dataSourceBuilder.username(db.getUsername());
    dataSourceBuilder.password(db.getPassword());
    return dataSourceBuilder.build();
  }
}
