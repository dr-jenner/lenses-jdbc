package com.landoop.jdbc4

import org.apache.avro.Schema

data class TypeInfo(
    val name: String,
    val dataType: Int, // the java.sql type int that matches this type
    val precision: Int,
    val signed: Boolean,
    val maxScale: Int,
    val minScale: Int) {

  companion object {

    val Boolean = TypeInfo(Schema.Type.BOOLEAN.name, java.sql.Types.BOOLEAN, 0, false, 0, 0)
    val Bytes = TypeInfo(Schema.Type.BYTES.name, java.sql.Types.ARRAY, 0, false, 0, 0)
    val Decimal = TypeInfo("DECIMAL", java.sql.Types.DECIMAL, 32, false, 22, 0)
    val Double = TypeInfo(Schema.Type.DOUBLE.name, java.sql.Types.DOUBLE, 0, false, 0, 0)
    val Float = TypeInfo(Schema.Type.FLOAT.name, java.sql.Types.FLOAT, 0, false, 0, 0)
    val Date = TypeInfo("DATE", java.sql.Types.DATE, 0, false, 0, 0)
    val Time = TypeInfo("TIME", java.sql.Types.TIME, 0, false, 0, 0)
    val Timestamp = TypeInfo("TIMESTAMP", java.sql.Types.TIMESTAMP, 0, false, 0, 0)
    val Int = TypeInfo(Schema.Type.INT.name, java.sql.Types.INTEGER, 0, false, 0, 0)
    val Long = TypeInfo(Schema.Type.LONG.name, java.sql.Types.BIGINT, 0, false, 0, 0)
    val String = TypeInfo(Schema.Type.STRING.name, java.sql.Types.VARCHAR, 0, false, 0, 0)

    val all = listOf(
        Boolean, Bytes, Double, Float, Date, Time, Timestamp, Int, Long, String
    )
  }
}