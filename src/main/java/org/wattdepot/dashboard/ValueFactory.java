package org.wattdepot.dashboard;

import org.restlet.resource.ResourceException;
import org.wattdepot.client.http.api.WattDepotClient;
import org.wattdepot.common.domainmodel.Depository;
import org.wattdepot.common.domainmodel.InterpolatedValue;
import org.wattdepot.common.domainmodel.InterpolatedValueList;
import org.wattdepot.common.domainmodel.Sensor;
import org.wattdepot.common.domainmodel.SensorGroup;
import org.wattdepot.common.domainmodel.SensorGroupList;
import org.wattdepot.common.domainmodel.SensorList;
import org.wattdepot.common.exception.BadCredentialException;
import org.wattdepot.common.exception.IdNotFoundException;
import org.wattdepot.common.exception.NoMeasurementException;
import org.wattdepot.common.util.DateConvert;
import org.wattdepot.common.util.tstamp.Tstamp;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
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
   * five minutes in milliseconds.
   */
  private static final Integer FIVE_MIN = 300000;
  /**
   * one hour in milliseconds.
   */
  private static final Integer ONE_HOUR = 3600000;
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
  private Map<String, CurrentPower> currentPowerMap;
  private Map<String, SensorStatus> currentStatus;
  private Map<String, Sensor> sensorMap;
  private Map<String, SensorGroup> sensorGroupMap;
  private ArrayList<SensorGroup> towerList;
  private Depository power;
  private Depository energy;

  /**
   * Default constructor hidden from the public.
   */
  private ValueFactory() {
    try {
      this.client = new WattDepotClient(SERVER_URL, CLIENT_NAME, CLIENT_ORG, CLIENT_PASSWORD);
      this.historicalData = new HashMap<String, HourlyMinMax>();
      this.currentPowerMap = new HashMap<String, CurrentPower>();
      this.currentStatus = new HashMap<String, SensorStatus>();
      this.sensorMap = new HashMap<String, Sensor>();
      this.sensorGroupMap = new HashMap<String, SensorGroup>();
      this.towerList = new ArrayList<SensorGroup>();
      SensorList sensorList = client.getSensors();
      for (Sensor s : sensorList.getSensors()) {
        sensorMap.put(s.getId(), s);
      }
      SensorGroupList groupList = client.getSensorGroups();
      for (SensorGroup g : groupList.getGroups()) {
        sensorGroupMap.put(g.getId(), g);
        if (g.getId().endsWith("-total")) {
          towerList.add(g);
        }
      }
      this.power = client.getDepository("power");
      this.energy = client.getDepository("energy");
    }
    catch (BadCredentialException e) {
      e.printStackTrace();
    }
    catch (IdNotFoundException e) {
      e.printStackTrace();
    }
  }


  public TowerCurrentPower getCurrentPower(SensorGroup towerGroup) {
    TowerCurrentPower ret = new TowerCurrentPower();
    ret.setTowerId(towerGroup.getId());
    ret.setNumSensors(0);
    ret.setNumSensorsReporting(0);
    ret.setHistoricalMax(0.0);
    ret.setHistoricalMin(0.0);
    ret.setHistoricalAve(0.0);
    ret.setCurrentValue(0.0);
    for (String sensorId : towerGroup.getSensors()) {
      Sensor s = sensorMap.get(sensorId);
      if (s != null) {
        ret.setNumSensors(ret.getNumSensors() + 1);
        CurrentPower power = getCurrentPower(sensorId);
        if (power != null) {
          SensorStatus status = currentStatus.get(sensorId);
          if (status.getStatus().equals(Status.GREEN)) {
            ret.setNumSensorsReporting(ret.getNumSensorsReporting() + 1);
            ret.setHistoricalMin(ret.getHistoricalMin() + power.getHistoricalMin());
            ret.setHistoricalMax(ret.getHistoricalMax() + power.getHistoricalMax());
            ret.setHistoricalAve(ret.getHistoricalAve() + power.getHistoricalAve());
            ret.setCurrentValue(ret.getCurrentValue() + power.getCurrentValue());
            ret.setTimestamp(power.getTimestamp());
          }
        }
      }
    }
    return ret;
  }

  public CurrentPower getCurrentPower(String sensorId) {
    return currentPowerMap.get(sensorId);
  }

  public void updateCurrentPower() {
    for (String s : sensorMap.keySet()) {
      updateCurrentPower(s);
    }
  }

  public void updateCurrentPower(String sensorId) {
    Sensor s = sensorMap.get(sensorId);
    if (s != null) {
      try {
        InterpolatedValue value = client.getLatestValue(power, s);
        XMLGregorianCalendar time = Tstamp.makeTimestamp(value.getEnd().getTime());
        XMLGregorianCalendar now = Tstamp.makeTimestamp();
        long deltaT = Tstamp.diff(now, time);
        SensorStatus status = new SensorStatus();
        status.setTimestamp(now);
        status.setSensor(s);
        if (Math.abs(deltaT) < FIVE_MIN) {
          status.setStatus(Status.GREEN);
          currentStatus.put(s.getId(), status);
        }
        else if (Math.abs(deltaT) > ONE_HOUR) {
          status.setStatus(Status.RED);
          currentStatus.put(s.getId(), status);
        }
        else {
          status.setStatus(Status.YELLOW);
          currentStatus.put(s.getId(), status);
        }
        HourlyMinMax historicalMinMax = historicalData.get(sensorId);
        if (historicalMinMax == null) {
          try {
            historicalMinMax = getHistoricalMinMax(power, s, time, 5);
          }
          catch (NoMeasurementException e) {
            // no measurements so sensor hasn't reported
            status.setStatus(Status.BLACK);
            currentStatus.put(s.getId(), status);
          }
          historicalData.put(sensorId, historicalMinMax);
        }
        else {
          XMLGregorianCalendar beginHour = beginningHour(time);
          if (historicalMinMax.getHour().getHour() != beginHour.getHour()) {
            try {
              historicalMinMax = getHistoricalMinMax(power, s, time, 5);
            }
            catch (NoMeasurementException e) {
              // no measurements so sensor hasn't reported
              status.setStatus(Status.BLACK);
              currentStatus.put(s.getId(), status);
            }
            historicalData.put(sensorId, historicalMinMax);
          }
        }
        CurrentPower currentPower = new CurrentPower();
        currentPower.setSensor(s);
        currentPower.setCurrentValue(value.getValue());
        currentPower.setTimestamp(time);
        currentPower.setHistoricalMax(historicalMinMax.getMax());
        currentPower.setHistoricalMin(historicalMinMax.getMin());
        currentPower.setHistoricalAve(historicalMinMax.getAverage());
        currentPowerMap.put(sensorId, currentPower);
      }
      catch (ResourceException re) {
        // no measurements so sensor hasn't reported
        SensorStatus status = new SensorStatus();
        status.setSensor(s);
        status.setStatus(Status.BLACK);
        status.setTimestamp(Tstamp.makeTimestamp());
        currentStatus.put(s.getId(), status);
      }
    }
  }

  public SensorStatus getCurrentStatus(String sensorId) {
    return currentStatus.get(sensorId);
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