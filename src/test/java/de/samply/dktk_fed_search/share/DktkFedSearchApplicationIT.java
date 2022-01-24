package de.samply.dktk_fed_search.share;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

import java.sql.SQLException;
import javax.sql.DataSource;
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

@SpringBootTest
@Testcontainers
@SuppressWarnings("NewClassNamingConvention")
class DktkFedSearchApplicationIT {

  private static final String AUTH_TOKEN = "token-131538";
  private static final String STRUCTURED_QUERY = """
      {
        "inclusionCriteria": [[{
          "termCodes": [{
            "system": "http://fhir.de/CodeSystem/dimdi/icd-10-gm",
            "code": "C71.1",
            "display": "Malignant neoplasm of brain"
          }]}]]
      }
      """;

  private static final Network network = Network.newNetwork();

  @Container
  private static final PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:9.6")
      .withDatabaseName("searchbroker")
      .withUsername("searchbroker")
      .withPassword("searchbroker")
      .withNetwork(network)
      .withNetworkAliases("postgres")
      .waitingFor(Wait.forListeningPort())
      .withExposedPorts(5432)
      .withStartupAttempts(3);

  @Container
  private static final GenericContainer<?> broker = new GenericContainer<>(
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

  @Container
  private static final GenericContainer<?> store = new GenericContainer<>(
      "samply/blaze:0.15")
      .withImagePullPolicy(PullPolicy.alwaysPull())
      .withEnv("LOG_LEVEL", "debug")
      .withNetwork(network)
      .withExposedPorts(8080)
      .waitingFor(Wait.forHttp("/health").forStatusCode(200))
      .withStartupAttempts(3);

  @DynamicPropertySource
  static void registerPgProperties(DynamicPropertyRegistry registry) {
    registry.add("app.broker.baseUrl", DktkFedSearchApplicationIT::brokerBaseUrl);
    registry.add("app.broker.authToken", () -> AUTH_TOKEN);
    registry.add("app.store.baseUrl", DktkFedSearchApplicationIT::storeBaseUrl);
  }

  private static String brokerBaseUrl() {
    return "http://localhost:%d/broker/rest/searchbroker".formatted(broker.getFirstMappedPort());
  }

  private static String storeBaseUrl() {
    return "http://localhost:%d/fhir".formatted(store.getFirstMappedPort());
  }

  private static void createOneInquiry() throws SQLException {
    var ds = getDataSource();
    var authTokenId = performInsert(ds,
        "insert into samply.authtoken (value) values ('" + AUTH_TOKEN + "')");
    var bankId = performInsert(ds,
        "insert into samply.bank (email, authtoken_id) values ('foo@bar.de', %d)".formatted(
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

  private static int performInsert(DataSource ds, String sql) throws SQLException {
    var statement = ds.getConnection().createStatement();
    statement.execute(sql, RETURN_GENERATED_KEYS);
    var keys = statement.getGeneratedKeys();
    keys.next();
    return keys.getInt(1);
  }

  private static DataSource getDataSource() {
    var dataSourceBuilder = DataSourceBuilder.create();
    System.out.println(db.getDriverClassName());
    dataSourceBuilder.driverClassName(db.getDriverClassName());
    dataSourceBuilder.url(db.getJdbcUrl());
    dataSourceBuilder.username(db.getUsername());
    dataSourceBuilder.password(db.getPassword());
    return dataSourceBuilder.build();
  }

  @Test
  void test1() throws Exception {
    createOneInquiry();

    Thread.sleep(40000);
  }
}
