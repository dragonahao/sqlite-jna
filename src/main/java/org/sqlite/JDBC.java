/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class JDBC implements Driver {
  private static final String PREFIX = "jdbc:sqlite:";

  static {
    try {
      DriverManager.registerDriver(new JDBC());
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if (!acceptsURL(url)) return null;
    return Conn.open(url.substring(PREFIX.length()), OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_CREATE | OpenFlags.SQLITE_OPEN_FULLMUTEX, null); // FIXME
  }
  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return url.startsWith(PREFIX);
  }
  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    return new DriverPropertyInfo[0];  // TODO
  }
  @Override
  public int getMajorVersion() {
    return 1;
  }
  @Override
  public int getMinorVersion() {
    return 0;
  }
  @Override
  public boolean jdbcCompliant() {
    return false;
  }
  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw Util.unsupported();
  }
}