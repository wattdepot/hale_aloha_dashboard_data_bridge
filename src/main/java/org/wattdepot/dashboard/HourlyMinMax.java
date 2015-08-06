package org.wattdepot.dashboard;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Created by Carleton on 8/6/2015.
 */
public class HourlyMinMax {
  private Double min;
  private Double max;
  private XMLGregorianCalendar hour;

  public HourlyMinMax(XMLGregorianCalendar hour, Double min, Double max) {
    this.hour = hour;
    this.max = max;
    this.min = min;
  }

  public Double getMin() {
    return min;
  }

  public void setMin(Double min) {
    this.min = min;
  }

  public XMLGregorianCalendar getHour() {
    return hour;
  }

  public void setHour(XMLGregorianCalendar hour) {
    this.hour = hour;
  }

  public Double getMax() {
    return max;

  }

  public void setMax(Double max) {
    this.max = max;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HourlyMinMax)) return false;

    HourlyMinMax that = (HourlyMinMax) o;

    if (getMin() != null ? !getMin().equals(that.getMin()) : that.getMin() != null) return false;
    if (getMax() != null ? !getMax().equals(that.getMax()) : that.getMax() != null) return false;
    return !(getHour() != null ? !getHour().equals(that.getHour()) : that.getHour() != null);

  }

  @Override
  public int hashCode() {
    int result = getMin() != null ? getMin().hashCode() : 0;
    result = 31 * result + (getMax() != null ? getMax().hashCode() : 0);
    result = 31 * result + (getHour() != null ? getHour().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "HourlyMinMax{" +
        "min=" + min +
        ", max=" + max +
        ", hour=" + hour +
        '}';
  }
}
