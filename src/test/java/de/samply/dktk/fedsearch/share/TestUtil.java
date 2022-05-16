package de.samply.dktk.fedsearch.share;

import static de.samply.dktk.fedsearch.share.ClasspathIo.slurp;
import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import de.samply.dktk.fedsearch.share.broker.model.Reply;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtil {

  static void assertReplyEquals(Reply expected, DataSource dataSource) throws Exception {
    try (var connection = dataSource.getConnection();
        var statement = connection.createStatement()) {
      statement.execute("select * from samply.reply");
      try (var rs = statement.getResultSet()) {
        assertTrue(rs.next());
        var replyReader = new ObjectMapper().readerFor(Reply.class);
        assertEquals(expected, replyReader.readValue(rs.getString("content")));
      }
    }
  }

  static String brokerBaseUrl(GenericContainer<?> broker) {
    return "http://%s:%d/broker/rest/searchbroker".formatted(broker.getHost(),
        broker.getMappedPort(8080));
  }

  static String storeBaseUrl(GenericContainer<?> store) {
    return "http://%s:%d/fhir".formatted(store.getHost(), store.getFirstMappedPort());
  }

  static void createOneInquiry(DataSource dataSource, String authToken, String mail,
      String structuredQuery) throws Exception {
    try (var connection = dataSource.getConnection()) {
      var authTokenId = performInsert(
          "insert into samply.authtoken (value) values ('" + authToken + "')",
          connection);
      var bankId = performInsert(
          "insert into samply.bank (email, authtoken_id) values ('%s', %d)".formatted(
              mail, authTokenId), connection);
      var siteId = performInsert("insert into samply.site (name) values ('foo')",
          connection);
      performInsert(
          "insert into samply.bank_site (bank_id, site_id, approved) values (%d, %d, true)".formatted(
              bankId, siteId
          ), connection);
      var authorId = performInsert(
          "insert into samply.user (username, authid) values ('foo', 1)", connection);
      var inquiryId = performInsert(
          "insert into samply.inquiry (author_id, status, revision) values (%d, 'IS_RELEASED', 1)".formatted(
              authorId), connection);
      performInsert(
          "insert into samply.inquiry_site (inquiry_id, site_id) values (%d, %d)".formatted(
              inquiryId,
              siteId), connection);
      performInsert("""
              INSERT INTO samply.inquiry_criteria (inquiry_id, criteria, type)
              VALUES (%d, '%s', 'IC_STRUCTURED_QUERY')
              """.formatted(inquiryId, structuredQuery),
          connection);
    }
  }

  private static int performInsert(String sql, Connection connection) throws SQLException {
    try (var statement = connection.createStatement()) {
      statement.execute(sql, RETURN_GENERATED_KEYS);
      var keys = statement.getGeneratedKeys();
      keys.next();
      return keys.getInt(1);
    }
  }

  static DataSource createDataSource(PostgreSQLContainer<?> db) {
    var dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName(db.getDriverClassName());
    dataSourceBuilder.url(db.getJdbcUrl());
    dataSourceBuilder.username(db.getUsername());
    dataSourceBuilder.password(db.getPassword());
    return dataSourceBuilder.build();
  }

  static void fillStore(GenericContainer<?> store) throws Exception {
    var fhirContext = FhirContext.forR4();
    var fhirParser = new FhirParser(fhirContext);
    var fhirClient = new FhirClient(fhirContext.newRestfulGenericClient(storeBaseUrl(store)));
    slurp("fhir-data.json")
        .flatMap(s -> fhirParser.parseResource(Bundle.class, s))
        .map(fhirClient::transact)
        .orElseThrow(Exception::new);
  }
}
