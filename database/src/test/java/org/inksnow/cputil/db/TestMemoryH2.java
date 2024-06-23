package org.inksnow.cputil.db;

import org.inksnow.cputil.db.h2.H2v1Database;
import org.inksnow.cputil.db.h2.H2v2Database;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestMemoryH2 {
  private void testConnection(Connection connection) throws SQLException {
    connection.createStatement().execute("CREATE TABLE test (id INT PRIMARY KEY, name VARCHAR(255))");
    connection.createStatement().execute("INSERT INTO test VALUES (1, 'Hello')");
    connection.createStatement().execute("INSERT INTO test VALUES (2, 'World')");
    ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM test");

    Assertions.assertTrue(resultSet.next());
    Assertions.assertEquals(1, resultSet.getInt("id"));
    Assertions.assertEquals("Hello", resultSet.getString("name"));

    Assertions.assertTrue(resultSet.next());
    Assertions.assertEquals(2, resultSet.getInt("id"));
    Assertions.assertEquals("World", resultSet.getString("name"));
  }

  @Test
  public void testLoadH2v1() throws IOException, SQLException {
    try(AuroraDatabase auroraDatabase = AuroraDatabase.builder()
        .databaseType(new H2v1Database())
        .cacheDirectory(Paths.get("build", "tmp", "cache"))
        .jdbcUrl("jdbc:h2:mem:test")
        .build()) {
      try (Connection connection = auroraDatabase.getConnection()) {
        testConnection(connection);
      }
    }

    Assertions.assertThrows(ClassNotFoundException.class, () -> {
      Class.forName("org.h2.Driver");
    });
  }

  @Test
  public void testLoadH2v2() throws IOException, SQLException {
    try(AuroraDatabase auroraDatabase = AuroraDatabase.builder()
        .databaseType(new H2v2Database())
        .cacheDirectory(Paths.get("build", "tmp", "cache"))
        .jdbcUrl("jdbc:h2:mem:test")
        .build()) {
      try (Connection connection = auroraDatabase.getConnection()) {
        testConnection(connection);
      }
    }

    Assertions.assertThrows(ClassNotFoundException.class, () -> {
      Class.forName("org.h2.Driver");
    });
  }
}
