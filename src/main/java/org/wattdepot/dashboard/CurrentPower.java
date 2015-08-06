package org.wattdepot.dashboard;

import org.wattdepot.common.domainmodel.Sensor;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Created by Carleton on 8/6/2015.
 */
public class CurrentPower {
  private Sensor sensor;
  private XMLGregorianCalendar timestamp;
  private double historicalMin;
  private double historicalMax;
  private double currentValue;

  public XMLGregorianCalendar getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(XMLGregorianCalendar timestamp) {
    this.timestamp = timestamp;
  }

  public Sensor getSensor() {
    return sensor;
  }

  public void setSensor(Sensor sensor) {
    this.sensor = sensor;
  }

  public double getHistoricalMin() {
    return historicalMin;
  }

  public void setHistoricalMin(double historicalMin) {
    this.historicalMin = historicalMin;
  }

  public double getHistoricalMax() {
    return historicalMax;
  }

  public void setHistoricalMax(double historicalMax) {
    this.historicalMax = historicalMax;
  }

  public double getCurrentValue() {
    return currentValue;
  }

  public void setCurrentValue(double currentValue) {
    this.currentValue = currentValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CurrentPower)) return false;

    CurrentPower that = (CurrentPower) o;

    if (Double.compare(that.getHistoricalMin(), getHistoricalMin()) != 0) return false;
    if (Double.compare(that.getHistoricalMax(), getHistoricalMax()) != 0) return false;
    if (Double.compare(that.getCurrentValue(), getCurrentValue()) != 0) return false;
    return getTimestamp().equals(that.getTimestamp());

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    result = getTimestamp().hashCode();
    temp = Double.doubleToLongBits(getHistoricalMin());
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(getHistoricalMax());
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(getCurrentValue());
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "CurrentPower{" +
        "sensor=" + sensor.getId() +
        ", timestamp=" + timestamp +
        ", historicalMin=" + historicalMin +
        ", historicalMax=" + historicalMax +
        ", currentValue=" + currentValue +
        '}';
  }
}
