package de.samply.dktk.fedsearch.share;

import static de.samply.dktk.fedsearch.share.ClasspathIo.slurp;
import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import de.samply.dktk.fedsearch.share.broker.model.Reply;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectReader;

@SpringBootTest
@Testcontainers
class DktkFedSearchApplicationTest {

  private static final String AUTH_TOKEN = "token-131538";
  private static final String MAIL = "foo@bar.de";

  private static final Network network = Network.newNetwork();

  @Container
  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> brokerDb = new PostgreSQLContainer<>("postgres:9.6")
      .withDatabaseName("searchbroker")
      .withUsername("searchbroker")
      .withPassword("searchbroker")
      .withNetwork(network)
      .withNetworkAliases("postgres")
      .waitingFor(Wait.forListeningPort())
      .withExposedPorts(5432)
      .withStartupAttempts(3);

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> broker = new GenericContainer<>(
      "samply/searchbroker:feature-structureQuery")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .dependsOn(brokerDb)
      .withEnv("POSTGRES_HOST", "postgres")
      .withEnv("POSTGRES_DB", "searchbroker")
      .withEnv("POSTGRES_USER", "searchbroker")
      .withEnv("POSTGRES_PASS", "searchbroker")
      .withNetwork(network)
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/broker/rest/health").forStatusCode(200))
      .withStartupAttempts(3);

  @Container
  @SuppressWarnings("resource")
  private static final GenericContainer<?> store = new GenericContainer<>("samply/blaze:0.17")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .withEnv("LOG_LEVEL", "debug")
      .withNetwork(network)
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/health").forStatusCode(200))
      .withStartupAttempts(3);

  @Container
  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> shareDb = new PostgreSQLContainer<>("postgres:14")
      .withDatabaseName("dktk-fed-search-share")
      .withUsername("dktk-fed-search-share")
      .withPassword("dktk-fed-search-share")
      .waitingFor(Wait.forListeningPort())
      .withExposedPorts(5432)
      .withStartupAttempts(3);

  private final ObjectReader replyReader = new ObjectMapper().readerFor(Reply.class);

  private static String brokerBaseUrl() {
    return "http://%s:%d/broker/rest/searchbroker".formatted(broker.getHost(),
        broker.getFirstMappedPort());
  }

  private static String storeBaseUrl() {
    return "http://%s:%d/fhir".formatted(store.getHost(), store.getFirstMappedPort());
  }

  private static String springDataSourceUrl() {
    return "jdbc:postgresql://%s:%d/dktk-fed-search-share".formatted(shareDb.getHost(),
        shareDb.getFirstMappedPort());
  }

  private FhirParser fhirParser;
  private FhirClient fhirClient;

  @DynamicPropertySource
  static void registerPgProperties(DynamicPropertyRegistry registry) {
    registry.add("app.broker.baseUrl", DktkFedSearchApplicationTest::brokerBaseUrl);
    registry.add("app.broker.authToken", () -> AUTH_TOKEN);
    registry.add("app.broker.mail", () -> MAIL);
    registry.add("app.store.baseUrl", DktkFedSearchApplicationTest::storeBaseUrl);
    registry.add("spring.datasource.url", DktkFedSearchApplicationTest::springDataSourceUrl);
  }

  private static void createOneInquiry() throws Exception {
    var ds = createDataSource();
    var authTokenId = performInsert(ds,
        "insert into samply.authtoken (value) values ('" + AUTH_TOKEN + "')");
    var bankId = performInsert(ds,
        "insert into samply.bank (email, authtoken_id) values ('%s', %d)".formatted(
            MAIL, authTokenId));
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
        """.formatted(inquiryId, slurp("structured-query.json").orElseThrow(Exception::new)));
  }

  private static int performInsert(DataSource ds, String sql) throws SQLException {
    try (var statement = ds.getConnection().createStatement()) {
      statement.execute(sql, RETURN_GENERATED_KEYS);
      var keys = statement.getGeneratedKeys();
      keys.next();
      return keys.getInt(1);
    }
  }

  private static DataSource createDataSource() {
    var dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName(brokerDb.getDriverClassName());
    dataSourceBuilder.url(brokerDb.getJdbcUrl());
    dataSourceBuilder.username(brokerDb.getUsername());
    dataSourceBuilder.password(brokerDb.getPassword());
    return dataSourceBuilder.build();
  }

  @BeforeEach
  void setUp() {
    var fhirContext = FhirContext.forR4();
    fhirParser = new FhirParser(fhirContext);
    fhirClient = new FhirClient(fhirContext.newRestfulGenericClient(storeBaseUrl()));
  }

  @Test
  void testOneInquiry() throws Exception {
    createOneInquiry();
    slurp("fhir-data.json")
        .flatMap(s -> fhirParser.parseResource(Bundle.class, s))
        .map(fhirClient::transact)
        .orElseThrow(Exception::new);

    Thread.sleep(30000);

    try (var connection = createDataSource().getConnection();
        var statement = connection.createStatement()) {
      statement.execute("select * from samply.reply");
      try (var rs = statement.getResultSet()) {
        assertTrue(rs.next());
        assertEquals(Reply.of(1), replyReader.readValue(rs.getString("content")));
      }
    }
  }
}
