package org.wattdepot.dashboard;

import org.restlet.resource.ResourceException;
import org.wattdepot.client.http.api.WattDepotClient;
import org.wattdepot.common.domainmodel.Depository;
import org.wattdepot.common.domainmodel.InterpolatedValue;
import org.wattdepot.common.domainmodel.InterpolatedValueList;
import org.wattdepot.common.domainmodel.Sensor;
import org.wattdepot.common.domainmodel.SensorGroup;
import org.wattdepot.common.exception.BadCredentialException;
import org.wattdepot.common.exception.IdNotFoundException;
import org.wattdepot.common.exception.NoMeasurementException;
import org.wattdepot.common.util.DateConvert;
import org.wattdepot.common.util.tstamp.Tstamp;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Carleton on 8/6/2015.
 */
public class ValueFactory {
  /**
   * The URL to the WattDepot server.
   */
  private static final String SERVER_URL = "http://mopsa.ics.hawaii.edu:8192/";
  /**
   * The WattDepot client name.
   */
  private static final String CLIENT_NAME = "hale_aloha";
  /**
   * The WattDepot client's password.
   */
  private static final String CLIENT_PASSWORD = "dashboard1";
  /**
   * The WattDepot client's organization.
   */
  private static final String CLIENT_ORG = "uh";
  /**
   * The singleton instance.
   */
  private static ValueFactory ourInstance = new ValueFactory();

  public static ValueFactory getInstance() {
    return ourInstance;
  }

  /**
   * WattDepotClient used to get the values.
   */
  private WattDepotClient client;
  private Map<String, HourlyMinMax> historicalData;

  /**
   * Default constructor hidden from the public.
   */
  private ValueFactory() {
    try {
      this.client = new WattDepotClient(SERVER_URL, CLIENT_NAME, CLIENT_ORG, CLIENT_PASSWORD);
      this.historicalData = new HashMap<String, HourlyMinMax>();
    }
    catch (BadCredentialException e) {
      e.printStackTrace();
    }
  }

  public TowerCurrentPower getCurrentPower(Depository depository, SensorGroup towerGroup) {
    if (depository.getMeasurementType().getName().startsWith("Power")) {
      TowerCurrentPower ret = new TowerCurrentPower();
      ret.setTowerId(towerGroup.getId());
      ret.setNumSensors(0);
      ret.setNumSensorsReporting(0);
      ret.setHistoricalMax(0.0);
      ret.setHistoricalMin(0.0);
      ret.setHistoricalAve(0.0);
      ret.setCurrentValue(0.0);
      for (String sensorId : towerGroup.getSensors()) {
        try {
          Sensor s = client.getSensor(sensorId);
          ret.setNumSensors(ret.getNumSensors() + 1);
          CurrentPower power = getCurrentPower(depository, s);
          if (power != null) {
            ret.setNumSensorsReporting(ret.getNumSensorsReporting() + 1);
            ret.setHistoricalMin(ret.getHistoricalMin() + power.getHistoricalMin());
            ret.setHistoricalMax(ret.getHistoricalMax() + power.getHistoricalMax());
            ret.setHistoricalAve(ret.getHistoricalAve() + power.getHistoricalAve());
            ret.setCurrentValue(ret.getCurrentValue() + power.getCurrentValue());
            ret.setTimestamp(power.getTimestamp());
          }
        }
        catch (IdNotFoundException e) {
          e.printStackTrace();
        }
        catch (NoMeasurementException e) {
          e.printStackTrace();
        }
        catch (ResourceException re) {
          System.out.println(sensorId + " not reporting.");
        }
      }
      return ret;
    }
    return null;
  }

  public CurrentPower getCurrentPower(Depository depository, Sensor sensor) throws NoMeasurementException {
    if (depository.getMeasurementType().getName().startsWith("Power")) {
      InterpolatedValue value = client.getLatestValue(depository, sensor);
      XMLGregorianCalendar time = Tstamp.makeTimestamp(value.getEnd().getTime());
      HourlyMinMax historicalMinMax = historicalData.get(sensor.getId());
      if (historicalMinMax == null) {
        historicalMinMax = getHistoricalMinMax(depository, sensor, time, 5);
        historicalData.put(sensor.getId(), historicalMinMax);
      }
      else {
        XMLGregorianCalendar beginHour = beginningHour(time);
        if (historicalMinMax.getHour().getHour() != beginHour.getHour()) {
          historicalMinMax = getHistoricalMinMax(depository, sensor, time, 5);
          historicalData.put(sensor.getId(), historicalMinMax);
        }
      }
      CurrentPower currentPower = new CurrentPower();
      currentPower.setSensor(sensor);
      currentPower.setCurrentValue(value.getValue());
      currentPower.setTimestamp(time);
      currentPower.setHistoricalMax(historicalMinMax.getMax());
      currentPower.setHistoricalMin(historicalMinMax.getMin());
      currentPower.setHistoricalAve(historicalMinMax.getAverage());
      return currentPower;
    }
    return null;
  }

  private XMLGregorianCalendar beginningHour(XMLGregorianCalendar now) {
    XMLGregorianCalendar beginning = Tstamp.makeTimestamp(now.toGregorianCalendar().getTimeInMillis());
    beginning.setMinute(0);
    beginning.setMillisecond(0);
    return beginning;
  }

  private XMLGregorianCalendar endingHour(XMLGregorianCalendar now) {
    XMLGregorianCalendar ending = Tstamp.incrementHours(now, 1);
    ending.setMinute(0);
    ending.setMillisecond(0);
    return ending;
  }

  private HourlyMinMax getHistoricalMinMax(Depository depository, Sensor sensor, XMLGregorianCalendar now, int weeks) throws NoMeasurementException {
    XMLGregorianCalendar beginHour = beginningHour(now);
    XMLGregorianCalendar endHour = endingHour(now);
    Double min = Double.MAX_VALUE;
    Double max = Double.MIN_VALUE;
    Double sum = 0.0;
    Integer count = 0;
    for (int i = 0; i < weeks; i++) {
      beginHour = Tstamp.incrementDays(beginHour, -7); // a week ago
      endHour = Tstamp.incrementDays(endHour, -7); // a week ago
      InterpolatedValueList valueList = client.getMinimumValues(depository, sensor, DateConvert.convertXMLCal(beginHour), DateConvert.convertXMLCal(endHour), 15, true);
      for (InterpolatedValue val : valueList.getInterpolatedValues()) {
        if (min > val.getValue()) {
          min = val.getValue();
        }
      }
      valueList = client.getMaximumValues(depository, sensor, DateConvert.convertXMLCal(beginHour), DateConvert.convertXMLCal(endHour), 15, true);
      for (InterpolatedValue val : valueList.getInterpolatedValues()) {
        if (max < val.getValue()) {
          max = val.getValue();
        }
      }
      valueList = client.getAverageValues(depository, sensor, DateConvert.convertXMLCal(beginHour), DateConvert.convertXMLCal(endHour), 15, true);
      for (InterpolatedValue val : valueList.getInterpolatedValues()) {
        sum += val.getValue();
        count++;
      }

    }
    HourlyMinMax hourlyMinMax = new HourlyMinMax(beginHour, min, max, sum / count);
    return hourlyMinMax;
  }

}