package org.wattdepot.dashboard;

import org.wattdepot.common.domainmodel.Sensor;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Created by carletonmoore on 8/11/15.
 */
public class SensorStatus {
  private Sensor sensor;
  private XMLGregorianCalendar timestamp;
  private Status status;

  public Sensor getSensor() {
    return sensor;
  }

  public void setSensor(Sensor sensor) {
    this.sensor = sensor;
  }

  public XMLGregorianCalendar getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(XMLGregorianCalendar timestamp) {
    this.timestamp = timestamp;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SensorStatus)) return false;

    SensorStatus that = (SensorStatus) o;

    if (sensor != null ? !sensor.equals(that.sensor) : that.sensor != null) return false;
    if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
    return status == that.status;

  }

  @Override
  public int hashCode() {
    int result = sensor != null ? sensor.hashCode() : 0;
    result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SensorStatus{" +
        "sensor=" + sensor +
        ", timestamp=" + timestamp +
        ", status=" + status +
        '}';
  }
}
