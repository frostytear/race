/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package gov.nasa.race.track.avro;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class TrackRoutePoint extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -8528429039669215384L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"TrackRoutePoint\",\"namespace\":\"gov.nasa.race.track.avro\",\"fields\":[{\"name\":\"date\",\"type\":\"long\",\"doc\":\"unix epoch for this track point\",\"units\":\"milliseconds\"},{\"name\":\"latitude\",\"type\":\"double\",\"doc\":\"WGS84 latitude of track point\",\"units\":\"degrees\"},{\"name\":\"longitude\",\"type\":\"double\",\"doc\":\"WGS84 longitude of track point\",\"units\":\"degrees\"},{\"name\":\"altitude\",\"type\":\"double\",\"doc\":\"(barometric) altitude of track point\",\"units\":\"meters\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** unix epoch for this track point */
  @Deprecated public long date;
  /** WGS84 latitude of track point */
  @Deprecated public double latitude;
  /** WGS84 longitude of track point */
  @Deprecated public double longitude;
  /** (barometric) altitude of track point */
  @Deprecated public double altitude;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public TrackRoutePoint() {}

  /**
   * All-args constructor.
   * @param date unix epoch for this track point
   * @param latitude WGS84 latitude of track point
   * @param longitude WGS84 longitude of track point
   * @param altitude (barometric) altitude of track point
   */
  public TrackRoutePoint(java.lang.Long date, java.lang.Double latitude, java.lang.Double longitude, java.lang.Double altitude) {
    this.date = date;
    this.latitude = latitude;
    this.longitude = longitude;
    this.altitude = altitude;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return date;
    case 1: return latitude;
    case 2: return longitude;
    case 3: return altitude;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: date = (java.lang.Long)value$; break;
    case 1: latitude = (java.lang.Double)value$; break;
    case 2: longitude = (java.lang.Double)value$; break;
    case 3: altitude = (java.lang.Double)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'date' field.
   * @return unix epoch for this track point
   */
  public java.lang.Long getDate() {
    return date;
  }

  /**
   * Sets the value of the 'date' field.
   * unix epoch for this track point
   * @param value the value to set.
   */
  public void setDate(java.lang.Long value) {
    this.date = value;
  }

  /**
   * Gets the value of the 'latitude' field.
   * @return WGS84 latitude of track point
   */
  public java.lang.Double getLatitude() {
    return latitude;
  }

  /**
   * Sets the value of the 'latitude' field.
   * WGS84 latitude of track point
   * @param value the value to set.
   */
  public void setLatitude(java.lang.Double value) {
    this.latitude = value;
  }

  /**
   * Gets the value of the 'longitude' field.
   * @return WGS84 longitude of track point
   */
  public java.lang.Double getLongitude() {
    return longitude;
  }

  /**
   * Sets the value of the 'longitude' field.
   * WGS84 longitude of track point
   * @param value the value to set.
   */
  public void setLongitude(java.lang.Double value) {
    this.longitude = value;
  }

  /**
   * Gets the value of the 'altitude' field.
   * @return (barometric) altitude of track point
   */
  public java.lang.Double getAltitude() {
    return altitude;
  }

  /**
   * Sets the value of the 'altitude' field.
   * (barometric) altitude of track point
   * @param value the value to set.
   */
  public void setAltitude(java.lang.Double value) {
    this.altitude = value;
  }

  /**
   * Creates a new TrackRoutePoint RecordBuilder.
   * @return A new TrackRoutePoint RecordBuilder
   */
  public static gov.nasa.race.track.avro.TrackRoutePoint.Builder newBuilder() {
    return new gov.nasa.race.track.avro.TrackRoutePoint.Builder();
  }

  /**
   * Creates a new TrackRoutePoint RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new TrackRoutePoint RecordBuilder
   */
  public static gov.nasa.race.track.avro.TrackRoutePoint.Builder newBuilder(gov.nasa.race.track.avro.TrackRoutePoint.Builder other) {
    return new gov.nasa.race.track.avro.TrackRoutePoint.Builder(other);
  }

  /**
   * Creates a new TrackRoutePoint RecordBuilder by copying an existing TrackRoutePoint instance.
   * @param other The existing instance to copy.
   * @return A new TrackRoutePoint RecordBuilder
   */
  public static gov.nasa.race.track.avro.TrackRoutePoint.Builder newBuilder(gov.nasa.race.track.avro.TrackRoutePoint other) {
    return new gov.nasa.race.track.avro.TrackRoutePoint.Builder(other);
  }

  /**
   * RecordBuilder for TrackRoutePoint instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<TrackRoutePoint>
    implements org.apache.avro.data.RecordBuilder<TrackRoutePoint> {

    /** unix epoch for this track point */
    private long date;
    /** WGS84 latitude of track point */
    private double latitude;
    /** WGS84 longitude of track point */
    private double longitude;
    /** (barometric) altitude of track point */
    private double altitude;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(gov.nasa.race.track.avro.TrackRoutePoint.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.date)) {
        this.date = data().deepCopy(fields()[0].schema(), other.date);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.latitude)) {
        this.latitude = data().deepCopy(fields()[1].schema(), other.latitude);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.longitude)) {
        this.longitude = data().deepCopy(fields()[2].schema(), other.longitude);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.altitude)) {
        this.altitude = data().deepCopy(fields()[3].schema(), other.altitude);
        fieldSetFlags()[3] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing TrackRoutePoint instance
     * @param other The existing instance to copy.
     */
    private Builder(gov.nasa.race.track.avro.TrackRoutePoint other) {
            super(SCHEMA$);
      if (isValidValue(fields()[0], other.date)) {
        this.date = data().deepCopy(fields()[0].schema(), other.date);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.latitude)) {
        this.latitude = data().deepCopy(fields()[1].schema(), other.latitude);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.longitude)) {
        this.longitude = data().deepCopy(fields()[2].schema(), other.longitude);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.altitude)) {
        this.altitude = data().deepCopy(fields()[3].schema(), other.altitude);
        fieldSetFlags()[3] = true;
      }
    }

    /**
      * Gets the value of the 'date' field.
      * unix epoch for this track point
      * @return The value.
      */
    public java.lang.Long getDate() {
      return date;
    }

    /**
      * Sets the value of the 'date' field.
      * unix epoch for this track point
      * @param value The value of 'date'.
      * @return This builder.
      */
    public gov.nasa.race.track.avro.TrackRoutePoint.Builder setDate(long value) {
      validate(fields()[0], value);
      this.date = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'date' field has been set.
      * unix epoch for this track point
      * @return True if the 'date' field has been set, false otherwise.
      */
    public boolean hasDate() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'date' field.
      * unix epoch for this track point
      * @return This builder.
      */
    public gov.nasa.race.track.avro.TrackRoutePoint.Builder clearDate() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'latitude' field.
      * WGS84 latitude of track point
      * @return The value.
      */
    public java.lang.Double getLatitude() {
      return latitude;
    }

    /**
      * Sets the value of the 'latitude' field.
      * WGS84 latitude of track point
      * @param value The value of 'latitude'.
      * @return This builder.
      */
    public gov.nasa.race.track.avro.TrackRoutePoint.Builder setLatitude(double value) {
      validate(fields()[1], value);
      this.latitude = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'latitude' field has been set.
      * WGS84 latitude of track point
      * @return True if the 'latitude' field has been set, false otherwise.
      */
    public boolean hasLatitude() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'latitude' field.
      * WGS84 latitude of track point
      * @return This builder.
      */
    public gov.nasa.race.track.avro.TrackRoutePoint.Builder clearLatitude() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'longitude' field.
      * WGS84 longitude of track point
      * @return The value.
      */
    public java.lang.Double getLongitude() {
      return longitude;
    }

    /**
      * Sets the value of the 'longitude' field.
      * WGS84 longitude of track point
      * @param value The value of 'longitude'.
      * @return This builder.
      */
    public gov.nasa.race.track.avro.TrackRoutePoint.Builder setLongitude(double value) {
      validate(fields()[2], value);
      this.longitude = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'longitude' field has been set.
      * WGS84 longitude of track point
      * @return True if the 'longitude' field has been set, false otherwise.
      */
    public boolean hasLongitude() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'longitude' field.
      * WGS84 longitude of track point
      * @return This builder.
      */
    public gov.nasa.race.track.avro.TrackRoutePoint.Builder clearLongitude() {
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'altitude' field.
      * (barometric) altitude of track point
      * @return The value.
      */
    public java.lang.Double getAltitude() {
      return altitude;
    }

    /**
      * Sets the value of the 'altitude' field.
      * (barometric) altitude of track point
      * @param value The value of 'altitude'.
      * @return This builder.
      */
    public gov.nasa.race.track.avro.TrackRoutePoint.Builder setAltitude(double value) {
      validate(fields()[3], value);
      this.altitude = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'altitude' field has been set.
      * (barometric) altitude of track point
      * @return True if the 'altitude' field has been set, false otherwise.
      */
    public boolean hasAltitude() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'altitude' field.
      * (barometric) altitude of track point
      * @return This builder.
      */
    public gov.nasa.race.track.avro.TrackRoutePoint.Builder clearAltitude() {
      fieldSetFlags()[3] = false;
      return this;
    }

    @Override
    public TrackRoutePoint build() {
      try {
        TrackRoutePoint record = new TrackRoutePoint();
        record.date = fieldSetFlags()[0] ? this.date : (java.lang.Long) defaultValue(fields()[0]);
        record.latitude = fieldSetFlags()[1] ? this.latitude : (java.lang.Double) defaultValue(fields()[1]);
        record.longitude = fieldSetFlags()[2] ? this.longitude : (java.lang.Double) defaultValue(fields()[2]);
        record.altitude = fieldSetFlags()[3] ? this.altitude : (java.lang.Double) defaultValue(fields()[3]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  private static final org.apache.avro.io.DatumWriter
    WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  private static final org.apache.avro.io.DatumReader
    READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}
