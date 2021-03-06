package com.landoop.jdbc4

import com.landoop.jdbc4.client.RestClient
import com.landoop.jdbc4.client.domain.InsertRecord
import com.landoop.jdbc4.client.domain.PreparedInsertInfo
import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.net.URL
import java.sql.Blob
import java.sql.Clob
import java.sql.Connection
import java.sql.Date
import java.sql.NClob
import java.sql.ParameterMetaData
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.RowId
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.SQLXML
import java.sql.Time
import java.sql.Timestamp
import java.util.*

class LsqlPreparedInsertStatement(conn: Connection,
                                  private val client: RestClient,
                                  sql: String) : LsqlStatement(conn, client), PreparedStatement, Logging {

  // for a prepared statement we need to connect to the lenses server, as the parsing
  // of the SQL will take place on the server side
  // The server will return back to us an object containing the required details of the
  // topic name, parameters, schema etc.
  private val info: PreparedInsertInfo = client.prepareStatement(sql).let {
    it.info ?: throw SQLException(it.error ?: "No error info available")
  }

  // the last resultset generated by this statement
  private var rs: ResultSet = RowResultSet.empty()

  // holder for values currently being built up
  private var builder = RecordBuilder(info)

  // holds all the records for a batch, should be cleared between batches
  private val batch = mutableListOf<InsertRecord>()

  private fun setValue(k: Int, value: Any?) = builder.put(k, value)

  /**
   * Clears the current parameter values immediately.
   * That is, the current record that is being "built" will be reset to empty.
   */
  override fun clearParameters() {
    builder = RecordBuilder(info)
  }

  private fun dispatchRecord() {
    builder.checkRecord()
    client.executePreparedInsert(info.topic, info.keyType, info.valueType, listOf(builder.build()))
  }

  override fun execute(): Boolean {
    dispatchRecord()
    return true
  }

  override fun executeUpdate(): Int {
    dispatchRecord()
    // this method returns 1 because executeUpdate() always sends only a single insert statement
    // if we want to send multiple we use executeBatch()
    return 1
  }

  // -- meta data methods

  /**
   * @return an empty result set because we do not yet support prepared statements for queries
   */
  override fun getMetaData(): ResultSetMetaData = EmptyResultSetMetaData

  override fun getParameterMetaData(): ParameterMetaData = AvroSchemaParameterMetaData(info)

  // -- batching support

  // returns the current batch size
  fun batchSize(): Int = batch.size

  // adds the current record to the batch
  override fun addBatch() {
    builder.checkRecord()
    if (batch.size == Constants.BATCH_HARD_LIMIT)
      throw SQLException("Batch size would exceed maximum of ${Constants.BATCH_HARD_LIMIT}")
    batch.add(builder.build())
    // we don't clear the builder here as parameters should remain in force for the next record
  }

  override fun clearBatch() {
    batch.clear()
  }

  override fun executeBatch(): IntArray {
    logger.debug("Executing batch of ${batch.size} records")
    client.executePreparedInsert(info.topic, info.keyType, info.valueType, batch.toList())
    // we should return an array of update counts, but we are only inserting, so we return an array of 0s
    val size = batch.size
    return IntArray(size, { _ -> 0 })
  }

  // -- methods which set values on the current record

  override fun setCharacterStream(parameterIndex: Int, reader: Reader?, length: Int) = setCharacterStream(parameterIndex, reader)
  override fun setCharacterStream(parameterIndex: Int, reader: Reader?, length: Long) = setCharacterStream(parameterIndex, reader)
  override fun setCharacterStream(parameterIndex: Int, reader: Reader?) = setValue(parameterIndex, reader!!.readText())
  override fun setDate(parameterIndex: Int, d: Date?) = setValue(parameterIndex, d)
  override fun setDate(parameterIndex: Int, d: Date?, cal: Calendar?) = setValue(parameterIndex, d)
  override fun setObject(parameterIndex: Int, x: Any?) = setValue(parameterIndex, x)
  override fun setLong(parameterIndex: Int, x: Long) = setValue(parameterIndex, x)
  override fun setNString(parameterIndex: Int, x: String?) = setValue(parameterIndex, x)
  override fun setURL(parameterIndex: Int, u: URL?) = setValue(parameterIndex, u)
  override fun setFloat(parameterIndex: Int, f: Float) = setValue(parameterIndex, f)
  override fun setTime(parameterIndex: Int, t: Time?) = setValue(parameterIndex, t)
  override fun setTime(parameterIndex: Int, x: Time?, cal: Calendar?) = setValue(parameterIndex, x)
  override fun setNCharacterStream(parameterIndex: Int, value: Reader?, length: Long) = setValue(parameterIndex, value!!.readLines())
  override fun setNCharacterStream(parameterIndex: Int, value: Reader?) = setValue(parameterIndex, value!!.readLines())
  override fun setInt(parameterIndex: Int, x: Int) = setValue(parameterIndex, x)
  override fun setDouble(parameterIndex: Int, x: Double) = setValue(parameterIndex, x)
  override fun setBigDecimal(parameterIndex: Int, x: BigDecimal?) = setValue(parameterIndex, x)
  override fun setObject(parameterIndex: Int, x: Any?, targetSqlType: Int) = setValue(parameterIndex, x)
  override fun setString(parameterIndex: Int, x: String?) = setValue(parameterIndex, x)
  override fun setNull(parameterIndex: Int, sqlType: Int) = setValue(parameterIndex, null)
  override fun setNull(parameterIndex: Int, sqlType: Int, typeName: String?) = setValue(parameterIndex, null)
  override fun setTimestamp(parameterIndex: Int, ts: Timestamp?) = setValue(parameterIndex, ts)
  override fun setTimestamp(parameterIndex: Int, ts: Timestamp?, cal: Calendar?) = setValue(parameterIndex, ts)
  override fun setShort(parameterIndex: Int, s: Short) = setValue(parameterIndex, s)
  override fun setBoolean(parameterIndex: Int, b: Boolean) = setValue(parameterIndex, b)
  override fun setByte(parameterIndex: Int, b: Byte) = setValue(parameterIndex, b)

  // -- unsupported types

  override fun setBinaryStream(parameterIndex: Int, x: InputStream?, length: Int) = throw SQLFeatureNotSupportedException()
  override fun setBinaryStream(parameterIndex: Int, x: InputStream?, length: Long) = throw SQLFeatureNotSupportedException()
  override fun setBinaryStream(parameterIndex: Int, x: InputStream?) = throw SQLFeatureNotSupportedException()
  override fun setClob(parameterIndex: Int, x: Clob?) = throw SQLFeatureNotSupportedException()
  override fun setClob(parameterIndex: Int, reader: Reader?, length: Long) = throw SQLFeatureNotSupportedException()
  override fun setClob(parameterIndex: Int, reader: Reader?) = throw SQLFeatureNotSupportedException()
  override fun setUnicodeStream(parameterIndex: Int, x: InputStream?, length: Int) = throw SQLFeatureNotSupportedException()
  override fun setObject(parameterIndex: Int, x: Any?, targetSqlType: Int, scaleOrLength: Int) = throw SQLFeatureNotSupportedException()
  override fun setBytes(parameterIndex: Int, x: ByteArray?) = throw SQLFeatureNotSupportedException()
  override fun setSQLXML(parameterIndex: Int, xmlObject: SQLXML?) = throw SQLFeatureNotSupportedException()
  override fun setRef(parameterIndex: Int, x: Ref?) = throw SQLFeatureNotSupportedException()
  override fun setBlob(parameterIndex: Int, x: Blob?) = throw SQLFeatureNotSupportedException()
  override fun setBlob(parameterIndex: Int, inputStream: InputStream?, length: Long) = throw SQLFeatureNotSupportedException()
  override fun setBlob(parameterIndex: Int, inputStream: InputStream?) = throw SQLFeatureNotSupportedException()
  override fun setArray(parameterIndex: Int, x: java.sql.Array?) = throw SQLFeatureNotSupportedException()
  override fun setRowId(parameterIndex: Int, x: RowId?) = throw SQLFeatureNotSupportedException()
  override fun setAsciiStream(parameterIndex: Int, x: InputStream?, length: Int) = throw SQLFeatureNotSupportedException()
  override fun setAsciiStream(parameterIndex: Int, x: InputStream?, length: Long) = throw SQLFeatureNotSupportedException()
  override fun setAsciiStream(parameterIndex: Int, x: InputStream?) = throw SQLFeatureNotSupportedException()
  override fun setNClob(parameterIndex: Int, value: NClob?) = throw SQLFeatureNotSupportedException()
  override fun setNClob(parameterIndex: Int, reader: Reader?, length: Long) = throw SQLFeatureNotSupportedException()
  override fun setNClob(parameterIndex: Int, reader: Reader?) = throw SQLFeatureNotSupportedException()

  // -- execute methods that accept SQL are not used by prepared statements

  override fun execute(sql: String): Boolean = throw SQLFeatureNotSupportedException("This method cannot be called on a prepared statement")
  override fun addBatch(sql: String?) = throw SQLFeatureNotSupportedException("This method cannot be called on a prepared statement")
  override fun executeQuery(sql: String): ResultSet = throw SQLFeatureNotSupportedException("This method cannot be called on a prepared statement")
  override fun execute(sql: String?, autoGeneratedKeys: Int): Boolean = throw SQLFeatureNotSupportedException("This method cannot be called on a prepared statement")
  override fun execute(sql: String?, columnIndexes: IntArray?): Boolean = throw SQLFeatureNotSupportedException("This method cannot be called on a prepared statement")
  override fun execute(sql: String?, columnNames: Array<out String>?): Boolean = throw SQLFeatureNotSupportedException("This method cannot be called on a prepared statement")

  override fun executeQuery(): ResultSet = throw SQLFeatureNotSupportedException("Prepared statements are only supported for inserts in this version")

  // == auto generated keys are not supported by kafka/lenses ==

  override fun getGeneratedKeys(): ResultSet = throw SQLFeatureNotSupportedException("Auto generated keys are not supported by Lenses")
}

