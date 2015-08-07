package org.wattdepot.dashboard;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Created by Carleton on 8/6/2015.
 */
public class TowerCurrentPower {
  private String towerId;
  private Integer numSensors;
  private Integer numSensorsReporting;
  private XMLGregorianCalendar timestamp;
  private double historicalMin;
  private double historicalMax;
  private double historicalAve;
  private double currentValue;

  @Override
  public String toString() {
    return "TowerCurrentPower{" +
        "towerId='" + towerId + '\'' +
        ", numSensors=" + numSensors +
        ", numSensorsReporting=" + numSensorsReporting +
        ", timestamp=" + timestamp +
        ", historicalMin=" + historicalMin +
        ", historicalMax=" + historicalMax +
        ", historicalAve=" + historicalAve +
        ", currentValue=" + currentValue +
        '}';
  }

  public double getHistoricalAve() {
    return historicalAve;
  }

  public void setHistoricalAve(double historicalAve) {
    this.historicalAve = historicalAve;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TowerCurrentPower)) return false;

    TowerCurrentPower that = (TowerCurrentPower) o;

    if (Double.compare(that.getHistoricalMin(), getHistoricalMin()) != 0) return false;
    if (Double.compare(that.getHistoricalMax(), getHistoricalMax()) != 0) return false;
    if (Double.compare(that.getCurrentValue(), getCurrentValue()) != 0) return false;
    if (getTowerId() != null ? !getTowerId().equals(that.getTowerId()) : that.getTowerId() != null) return false;
    if (getNumSensors() != null ? !getNumSensors().equals(that.getNumSensors()) : that.getNumSensors() != null)
      return false;
    if (getNumSensorsReporting() != null ? !getNumSensorsReporting().equals(that.getNumSensorsReporting()) : that.getNumSensorsReporting() != null)
      return false;
    return !(getTimestamp() != null ? !getTimestamp().equals(that.getTimestamp()) : that.getTimestamp() != null);

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    result = getTowerId() != null ? getTowerId().hashCode() : 0;
    result = 31 * result + (getNumSensors() != null ? getNumSensors().hashCode() : 0);
    result = 31 * result + (getNumSensorsReporting() != null ? getNumSensorsReporting().hashCode() : 0);
    result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
    temp = Double.doubleToLongBits(getHistoricalMin());
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(getHistoricalMax());
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(getCurrentValue());
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  public String getTowerId() {

    return towerId;
  }

  public void setTowerId(String towerId) {
    this.towerId = towerId;
  }

  public Integer getNumSensors() {
    return numSensors;
  }

  public void setNumSensors(Integer numSensors) {
    this.numSensors = numSensors;
  }

  public Integer getNumSensorsReporting() {
    return numSensorsReporting;
  }

  public void setNumSensorsReporting(Integer numSensorsReporting) {
    this.numSensorsReporting = numSensorsReporting;
  }

  public XMLGregorianCalendar getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(XMLGregorianCalendar timestamp) {
    this.timestamp = timestamp;
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
}
