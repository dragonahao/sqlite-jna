/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import org.sqlite.ColTypes;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

public class Rows implements ResultSet, ResultSetMetaData {
  private Stmt s;
  private org.sqlite.Stmt stmt;
  private int row;
  private Boolean wasNull;

  public Rows(Stmt s, boolean hasRow) throws SQLException {
    this.s = s;
    this.stmt = s.getStmt();
    this.row = hasRow ? 0 : -1; // Initialized at -1 when there is no result otherwise 0
  }

  private org.sqlite.Stmt getStmt() throws SQLException {
    checkOpen();
    // TODO Check Statement is opened?
    return stmt;
  }
  private void checkOpen() throws SQLException {
    if (stmt == null) {
      throw new SQLException("resultSet closed");
    }
  }

  private boolean step() throws SQLException {
    checkOpen();
    if (row == -1) { // no result
      return false;
    }
    if (row == 0) {
      row++;
      return true;
    }
    final boolean hasRow = stmt.step();
    if (hasRow) {
      row++;
    } else {
      close();
    }
    return hasRow;
  }

  private int fixCol(int columnIndex) {
    return columnIndex - 1;
  }

  @Override
  public boolean next() throws SQLException {
    wasNull = null;
    return step();
  }
  @Override
  public void close() throws SQLException {
    Util.trace("ResultSet.close");
    if (stmt != null) {
      if (s.isCloseOnCompletion()) {
        s.close();
      } else {
        stmt.reset();
      }
      s = null;
      stmt = null;
    }
  }
  @Override
  public boolean wasNull() throws SQLException {
    if (wasNull == null) {
      throw new SQLException("no column has been read");
    }
    return wasNull;
  }
  @Override
  public String getString(int columnIndex) throws SQLException {
    final String s = getStmt().getColumnText(fixCol(columnIndex));
    wasNull = s == null;
    return s;
  }
  @Override
  public boolean getBoolean(int columnIndex) throws SQLException {
    return getInt(columnIndex) != 0;
  }
  @Override
  public byte getByte(int columnIndex) throws SQLException {
    return (byte) getInt(columnIndex);
  }
  @Override
  public short getShort(int columnIndex) throws SQLException {
    return (short) getInt(columnIndex);
  }
  @Override
  public int getInt(int columnIndex) throws SQLException {
    final org.sqlite.Stmt stmt = getStmt();
    // After a type conversion, the value returned by sqlite3_column_type() is undefined.
    final int sourceType = stmt.getColumnType(fixCol(columnIndex));
    stmt.checkTypeMismatch(fixCol(columnIndex), sourceType, ColTypes.SQLITE_INTEGER);
    final boolean wasNull = sourceType == ColTypes.SQLITE_NULL;
    this.wasNull = wasNull;
    if (wasNull) {
      return 0;
    } else {
      return stmt.getColumnInt(fixCol(columnIndex));
    }
  }
  @Override
  public long getLong(int columnIndex) throws SQLException {
    final org.sqlite.Stmt stmt = getStmt();
    // After a type conversion, the value returned by sqlite3_column_type() is undefined.
    final int sourceType = stmt.getColumnType(fixCol(columnIndex));
    stmt.checkTypeMismatch(fixCol(columnIndex), sourceType, ColTypes.SQLITE_INTEGER);
    final boolean wasNull = sourceType == ColTypes.SQLITE_NULL;
    this.wasNull = wasNull;
    if (wasNull) {
      return 0;
    } else {
      return getStmt().getColumnLong(fixCol(columnIndex));
    }
  }
  @Override
  public float getFloat(int columnIndex) throws SQLException {
    return (float) getDouble(columnIndex);
  }
  @Override
  public double getDouble(int columnIndex) throws SQLException {
    final org.sqlite.Stmt stmt = getStmt();
    // After a type conversion, the value returned by sqlite3_column_type() is undefined.
    final int sourceType = stmt.getColumnType(fixCol(columnIndex));
    stmt.checkTypeMismatch(fixCol(columnIndex), sourceType, ColTypes.SQLITE_FLOAT);
    final boolean wasNull = sourceType == ColTypes.SQLITE_NULL;
    this.wasNull = wasNull;
    if (wasNull) {
      return 0;
    } else {
      return getStmt().getColumnDouble(fixCol(columnIndex));
    }
  }
  @Override
  @SuppressWarnings("deprecation")
  public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
    throw Util.unsupported("Resultset.getBigDecimal(int,int)");
  }
  @Override
  public byte[] getBytes(int columnIndex) throws SQLException {
    return getStmt().getColumnBlob(fixCol(columnIndex));
  }
  @Override
  public Date getDate(int columnIndex) throws SQLException {
    final long ms = getLong(columnIndex);
    if (wasNull) return null;
    return new Date(ms);
  }
  @Override
  public Time getTime(int columnIndex) throws SQLException {
    final long ms = getLong(columnIndex);
    if (wasNull) return null;
    return new Time(ms);
  }
  @Override
  public Timestamp getTimestamp(int columnIndex) throws SQLException {
    final long ms = getLong(columnIndex);
    if (wasNull) return null;
    return new Timestamp(ms);
  }
  @Override
  public InputStream getAsciiStream(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getAsciiStream");
  }
  @Override
  @SuppressWarnings("deprecation")
  public InputStream getUnicodeStream(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getUnicodeStream");
  }
  @Override
  public InputStream getBinaryStream(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getBinaryStream");
  }
  @Override
  public String getString(String columnLabel) throws SQLException {
    return getString(findColumn(columnLabel));
  }
  @Override
  public boolean getBoolean(String columnLabel) throws SQLException {
    return getBoolean(findColumn(columnLabel));
  }
  @Override
  public byte getByte(String columnLabel) throws SQLException {
    return getByte(findColumn(columnLabel));
  }
  @Override
  public short getShort(String columnLabel) throws SQLException {
    return getShort(findColumn(columnLabel));
  }
  @Override
  public int getInt(String columnLabel) throws SQLException {
    return getInt(findColumn(columnLabel));
  }
  @Override
  public long getLong(String columnLabel) throws SQLException {
    return getLong(findColumn(columnLabel));
  }
  @Override
  public float getFloat(String columnLabel) throws SQLException {
    return getFloat(findColumn(columnLabel));
  }
  @Override
  public double getDouble(String columnLabel) throws SQLException {
    return getDouble(findColumn(columnLabel));
  }
  @Override
  @SuppressWarnings("deprecation")
  public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
    return getBigDecimal(findColumn(columnLabel), scale);
  }
  @Override
  public byte[] getBytes(String columnLabel) throws SQLException {
    return getBytes(findColumn(columnLabel));
  }
  @Override
  public Date getDate(String columnLabel) throws SQLException {
    return getDate(findColumn(columnLabel));
  }
  @Override
  public Time getTime(String columnLabel) throws SQLException {
    return getTime(findColumn(columnLabel));
  }
  @Override
  public Timestamp getTimestamp(String columnLabel) throws SQLException {
    return getTimestamp(findColumn(columnLabel));
  }
  @Override
  public InputStream getAsciiStream(String columnLabel) throws SQLException {
    return getAsciiStream(findColumn(columnLabel));
  }
  @Override
  @SuppressWarnings("deprecation")
  public InputStream getUnicodeStream(String columnLabel) throws SQLException {
    return getUnicodeStream(findColumn(columnLabel));
  }
  @Override
  public InputStream getBinaryStream(String columnLabel) throws SQLException {
    return getBinaryStream(findColumn(columnLabel));
  }
  @Override
  public SQLWarning getWarnings() throws SQLException {
    checkOpen();
    return null;
  }
  @Override
  public void clearWarnings() throws SQLException {
    checkOpen();
  }
  @Override
  public String getCursorName() throws SQLException {
    Util.trace("ResultSet.getCursorName");
    checkOpen();
    return null;
  }
  @Override
  public ResultSetMetaData getMetaData() throws SQLException { // Used by Hibernate
    return this;
  }
  @Override
  public Object getObject(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getObject");
  }
  @Override
  public Object getObject(String columnLabel) throws SQLException {
    return getObject(findColumn(columnLabel));
  }
  @Override
  public int findColumn(String columnLabel) throws SQLException {
    checkOpen();
    return s.findCol(columnLabel);
  }
  @Override
  public Reader getCharacterStream(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getCharacterStream");
  }
  @Override
  public Reader getCharacterStream(String columnLabel) throws SQLException {
    return getCharacterStream(findColumn(columnLabel));
  }
  @Override
  public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    final String stringValue = getString(columnIndex);
    if (stringValue == null) {
      return null;
    } else {
      try {
        return new BigDecimal(stringValue);
      } catch (NumberFormatException e) {
        throw Util.error("Bad value for type BigDecimal : " + stringValue);
      }
    }
  }
  @Override
  public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
    return getBigDecimal(findColumn(columnLabel));
  }
  @Override
  public boolean isBeforeFirst() throws SQLException {
    checkOpen();
    return row < 1;
  }
  @Override
  public boolean isAfterLast() throws SQLException {
    Util.trace("ResultSet.isAfterLast");
    checkOpen();
    return false; // TODO
  }
  @Override
  public boolean isFirst() throws SQLException {
    Util.trace("ResultSet.isFirst");
    checkOpen();
    return row == 1;
  }
  @Override
  public boolean isLast() throws SQLException {
    Util.trace("ResultSet.isLast");
    checkOpen();
    return false; // TODO
  }
  @Override
  public void beforeFirst() throws SQLException {
    throw typeForwardOnly();
  }
  @Override
  public void afterLast() throws SQLException {
    throw typeForwardOnly();
  }
  @Override
  public boolean first() throws SQLException {
    throw typeForwardOnly();
  }
  @Override
  public boolean last() throws SQLException {
    throw typeForwardOnly();
  }
  @Override
  public int getRow() throws SQLException {
    checkOpen();
    return Math.max(row, 0);
  }
  @Override
  public boolean absolute(int row) throws SQLException {
    throw typeForwardOnly();
  }
  @Override
  public boolean relative(int rows) throws SQLException {
    throw typeForwardOnly();
  }
  @Override
  public boolean previous() throws SQLException {
    throw typeForwardOnly();
  }
  @Override
  public void setFetchDirection(int direction) throws SQLException {
    checkOpen();
    if (ResultSet.FETCH_FORWARD != direction) {
      throw Util.caseUnsupported("SQLite supports only FETCH_FORWARD direction");
    }
  }
  @Override
  public int getFetchDirection() throws SQLException {
    checkOpen();
    return FETCH_FORWARD;
  }
  @Override
  public void setFetchSize(int rows) throws SQLException {
    if (rows < 0) throw Util.error("fetch size must be >= 0");
    checkOpen();
    if (rows != 1) {
      throw Util.caseUnsupported("SQLite does not support setting fetch size");
    }
  }
  @Override
  public int getFetchSize() throws SQLException {
    checkOpen();
    return 1;
  }
  @Override
  public int getType() throws SQLException {
    checkOpen();
    return TYPE_FORWARD_ONLY;
  }
  @Override
  public int getConcurrency() throws SQLException {
    checkOpen();
    return CONCUR_READ_ONLY;
  }
  @Override
  public boolean rowUpdated() throws SQLException {
    checkOpen();
    return false;
  }
  @Override
  public boolean rowInserted() throws SQLException {
    checkOpen();
    return false;
  }
  @Override
  public boolean rowDeleted() throws SQLException {
    checkOpen();
    return false;
  }
  @Override
  public void updateNull(int columnIndex) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBoolean(int columnIndex, boolean x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateByte(int columnIndex, byte x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateShort(int columnIndex, short x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateInt(int columnIndex, int x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateLong(int columnIndex, long x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateFloat(int columnIndex, float x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateDouble(int columnIndex, double x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateString(int columnIndex, String x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBytes(int columnIndex, byte[] x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateDate(int columnIndex, Date x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateTime(int columnIndex, Time x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateObject(int columnIndex, Object x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNull(String columnLabel) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBoolean(String columnLabel, boolean x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateByte(String columnLabel, byte x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateShort(String columnLabel, short x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateInt(String columnLabel, int x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateLong(String columnLabel, long x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateFloat(String columnLabel, float x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateDouble(String columnLabel, double x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateString(String columnLabel, String x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBytes(String columnLabel, byte[] x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateDate(String columnLabel, Date x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateTime(String columnLabel, Time x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateObject(String columnLabel, Object x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void insertRow() throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateRow() throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void deleteRow() throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void refreshRow() throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void cancelRowUpdates() throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void moveToInsertRow() throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void moveToCurrentRow() throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public Statement getStatement() throws SQLException {
    checkOpen();
    return s;
  }
  @Override
  public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
    throw Util.unsupported("ResultSet.getObject(int,Map)");
  }
  @Override
  public Ref getRef(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getRef");
  }
  @Override
  public Blob getBlob(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getBlob");
  }
  @Override
  public Clob getClob(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getClob");
  }
  @Override
  public Array getArray(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getArray");
  }
  @Override
  public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
    return getObject(findColumn(columnLabel), map);
  }
  @Override
  public Ref getRef(String columnLabel) throws SQLException {
    return getRef(findColumn(columnLabel));
  }
  @Override
  public Blob getBlob(String columnLabel) throws SQLException {
    return getBlob(findColumn(columnLabel));
  }
  @Override
  public Clob getClob(String columnLabel) throws SQLException {
    return getClob(findColumn(columnLabel));
  }
  @Override
  public Array getArray(String columnLabel) throws SQLException {
    return getArray(findColumn(columnLabel));
  }
  @Override
  public Date getDate(int columnIndex, Calendar cal) throws SQLException {
    throw Util.unsupported("ResultSet.getDate"); // TODO
  }
  @Override
  public Date getDate(String columnLabel, Calendar cal) throws SQLException {
    return getDate(findColumn(columnLabel), cal);
  }
  @Override
  public Time getTime(int columnIndex, Calendar cal) throws SQLException {
    throw Util.unsupported("ResultSet.getTime"); // TODO
  }
  @Override
  public Time getTime(String columnLabel, Calendar cal) throws SQLException {
    return getTime(findColumn(columnLabel), cal);
  }
  @Override
  public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
    throw Util.unsupported("ResultSet.getTimestamp"); // TODO
  }
  @Override
  public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
    return getTimestamp(findColumn(columnLabel), cal);
  }
  @Override
  public URL getURL(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getURL");
  }
  @Override
  public URL getURL(String columnLabel) throws SQLException {
    return getURL(findColumn(columnLabel));
  }
  @Override
  public void updateRef(int columnIndex, Ref x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateRef(String columnLabel, Ref x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBlob(int columnIndex, Blob x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBlob(String columnLabel, Blob x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateClob(int columnIndex, Clob x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateClob(String columnLabel, Clob x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateArray(int columnIndex, Array x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateArray(String columnLabel, Array x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public RowId getRowId(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getRowId");
  }
  @Override
  public RowId getRowId(String columnLabel) throws SQLException {
    return getRowId(findColumn(columnLabel));
  }
  @Override
  public void updateRowId(int columnIndex, RowId x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateRowId(String columnLabel, RowId x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public int getHoldability() throws SQLException {
    return CLOSE_CURSORS_AT_COMMIT;
  }
  @Override
  public boolean isClosed() {
    return stmt == null;
  }
  @Override
  public void updateNString(int columnIndex, String nString) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNString(String columnLabel, String nString) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public NClob getNClob(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getNClob");
  }
  @Override
  public NClob getNClob(String columnLabel) throws SQLException {
    return getNClob(findColumn(columnLabel));
  }
  @Override
  public SQLXML getSQLXML(int columnIndex) throws SQLException {
    throw Util.unsupported("ResultSet.getSQLXML");
  }
  @Override
  public SQLXML getSQLXML(String columnLabel) throws SQLException {
    return getSQLXML(findColumn(columnLabel));
  }
  @Override
  public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public String getNString(int columnIndex) throws SQLException {
    return getString(columnIndex);
  }
  @Override
  public String getNString(String columnLabel) throws SQLException {
    return getNString(findColumn(columnLabel));
  }
  @Override
  public Reader getNCharacterStream(int columnIndex) throws SQLException {
    return getCharacterStream(columnIndex);
  }
  @Override
  public Reader getNCharacterStream(String columnLabel) throws SQLException {
    return getNCharacterStream(findColumn(columnLabel));
  }
  @Override
  public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateClob(int columnIndex, Reader reader) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateClob(String columnLabel, Reader reader) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNClob(int columnIndex, Reader reader) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public void updateNClob(String columnLabel, Reader reader) throws SQLException {
    throw concurReadOnly();
  }
  @Override
  public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
    throw Util.unsupported("ResultSet.getObject(int, Class)"); // TODO
  }
  @Override
  public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
    return getObject(findColumn(columnLabel), type);
  }
  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw Util.error("not a wrapper");
  }
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

// ResultSetMetaData

  @Override
  public int getColumnCount() throws SQLException { // Used by Hibernate
    return getStmt().getColumnCount();
  }
  @Override
  public boolean isAutoIncrement(int column) throws SQLException {
    throw Util.unsupported("ResultSetMetaData.isAutoIncrement"); // TODO
  }
  @Override
  public boolean isCaseSensitive(int column) throws SQLException {
    return true;
  }
  @Override
  public boolean isSearchable(int column) throws SQLException {
    return true;
  }
  @Override
  public boolean isCurrency(int column) throws SQLException {
    return false;
  }
  @Override
  public int isNullable(int column) throws SQLException {
    throw Util.unsupported("ResultSetMetaData.isNullable"); // TODO
  }
  @Override
  public boolean isSigned(int column) throws SQLException {
    return true;
  }
  @Override
  public int getColumnDisplaySize(int column) throws SQLException {
    throw Util.unsupported("ResultSetMetaData.getColumnDisplaySize"); // TODO
  }
  @Override
  public String getColumnLabel(int column) throws SQLException {
    throw Util.unsupported("ResultSetMetaData.getColumnLabel"); // TODO
  }
  @Override
  public String getColumnName(int column) throws SQLException {
    throw Util.unsupported("ResultSetMetaData.getColumnName"); // TODO
  }
  @Override
  public String getSchemaName(int column) throws SQLException {
    Util.trace("ResultSetMetaData.getSchemaName");
    return ""; // TODO Validate
  }
  @Override
  public int getPrecision(int column) throws SQLException {
    Util.trace("ResultSetMetaData.getPrecision");
    return 0; // TODO
  }
  @Override
  public int getScale(int column) throws SQLException {
    Util.trace("ResultSetMetaData.getScale");
    return 0; // TODO
  }
  @Override
  public String getTableName(int column) throws SQLException {
    throw Util.unsupported("ResultSetMetaData.getTableName"); // TODO
  }
  @Override
  public String getCatalogName(int column) throws SQLException {
    Util.trace("ResultSetMetaData.getCatalogName");
    return ""; // TODO Validate
  }
  @Override
  public int getColumnType(int column) throws SQLException {
    throw Util.unsupported("ResultSetMetaData.getColumnType"); // TODO
  }
  @Override
  public String getColumnTypeName(int column) throws SQLException {
    throw Util.unsupported("ResultSetMetaData.getColumnTypeName"); // TODO
  }
  @Override
  public boolean isReadOnly(int column) throws SQLException {
    return false;
  }
  @Override
  public boolean isWritable(int column) throws SQLException {
    return true;
  }
  @Override
  public boolean isDefinitelyWritable(int column) throws SQLException {
    return true;
  }
  @Override
  public String getColumnClassName(int column) throws SQLException {
    throw Util.unsupported("ResultSetMetaData.getColumnClassName"); // TODO
  }

  private static SQLException typeForwardOnly() {
    return Util.error("ResultSet is TYPE_FORWARD_ONLY");
  }
  private static SQLException concurReadOnly() {
    return Util.error("ResultSet is CONCUR_READ_ONLY");
  }
}