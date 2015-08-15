package org.wattdepot.dashboard;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.restlet.resource.ResourceException;
import org.wattdepot.client.http.api.WattDepotClient;
import org.wattdepot.common.domainmodel.Depository;
import org.wattdepot.common.domainmodel.InterpolatedValue;
import org.wattdepot.common.domainmodel.InterpolatedValueList;
import org.wattdepot.common.domainmodel.MeasurementList;
import org.wattdepot.common.domainmodel.Sensor;
import org.wattdepot.common.domainmodel.SensorGroup;
import org.wattdepot.common.domainmodel.SensorGroupList;
import org.wattdepot.common.domainmodel.SensorList;
import org.wattdepot.common.domainmodel.SensorStatusList;
import org.wattdepot.common.exception.BadCredentialException;
import org.wattdepot.common.exception.IdNotFoundException;
import org.wattdepot.common.exception.NoMeasurementException;
import org.wattdepot.common.util.DateConvert;
import org.wattdepot.common.util.tstamp.Tstamp;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Date;
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
  private Map<SensorGroup, InterpolatedValueList> last24HoursEnergy;
  private Map<SensorGroup, InterpolatedValueList> dailyEnergy;
  private Map<SensorGroup, SensorStatusList> sensorStatus;
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
      this.last24HoursEnergy = new HashMap<SensorGroup, InterpolatedValueList>();
      this.dailyEnergy = new HashMap<SensorGroup, InterpolatedValueList>();
      this.sensorStatus = new HashMap<SensorGroup, SensorStatusList>();

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

  public void updateHourlyEnergy() {
    XMLGregorianCalendar now = Tstamp.makeTimestamp();
    XMLGregorianCalendar oneDayAgo = Tstamp.incrementDays(now, -1);
    for (SensorGroup g : towerList) {
      InterpolatedValueList hourlyData = this.client.getHourlyValues(this.energy, g, DateConvert.convertXMLCal(oneDayAgo), DateConvert.convertXMLCal(now), false);
      last24HoursEnergy.put(g, hourlyData);
    }
  }

  public void updateDailyEnergy() {
    XMLGregorianCalendar now = Tstamp.makeTimestamp();
    XMLGregorianCalendar thirtyDaysAgo = Tstamp.incrementDays(now, -30);
    for (SensorGroup group : towerList) {
      InterpolatedValueList dailyData = this.client.getDailyValues(this.energy, group, DateConvert.convertXMLCal(thirtyDaysAgo), DateConvert.convertXMLCal(now), false);
      dailyEnergy.put(group, dailyData);
    }
  }

  public void updateSensorStatus() {
    for (SensorGroup group : towerList) {
      SensorStatusList statusList = client.getSensorStatuses(power, group);
      sensorStatus.put(group, statusList);
    }
  }

  public void updateMongoEnergyLast24Hours(DBCollection db) {
    for (SensorGroup tower: towerList) {
      ArrayList<BasicDBObject> hourlyData = getLast24HoursEnergyAsDocs(tower);
      for (BasicDBObject doc : hourlyData) {
        db.insert(doc);
      }
    }
  }

  public void updateMongoEnergyDailyData(DBCollection db) {
    for (SensorGroup tower : towerList) {
      ArrayList<BasicDBObject> dailyData = getDailyEnergyAsDocs(tower);
      for (BasicDBObject doc : dailyData) {
        db.insert(doc);
      }
    }
  }

  public void updateMongoSensorStatus(DBCollection db) {
    for (SensorGroup tower : towerList) {
      ArrayList<BasicDBObject> statuses = getSensorStatusAsDocs(tower);
      for (BasicDBObject doc : statuses) {
        db.insert(doc);
      }
    }
  }

  private InterpolatedValueList getLast24HoursEnergy(SensorGroup group) {
    return last24HoursEnergy.get(group);
  }

  private ArrayList<BasicDBObject> getLast24HoursEnergyAsDocs(SensorGroup group) {
    ArrayList<BasicDBObject> ret = new ArrayList<BasicDBObject>();
    InterpolatedValueList list = last24HoursEnergy.get(group);
    if (list != null) {
      for (InterpolatedValue value : list.getInterpolatedValues()) {
        ret.add(buildDBObject(value));
      }
    }
    return ret;
  }

  private ArrayList<BasicDBObject> getDailyEnergyAsDocs(SensorGroup group) {
    ArrayList<BasicDBObject> ret = new ArrayList<BasicDBObject>();
    InterpolatedValueList list = dailyEnergy.get(group);
    if (list != null) {
      for (InterpolatedValue value : list.getInterpolatedValues()) {
        ret.add(buildDBObject(value));
      }
    }
    return ret;
  }

  private ArrayList<BasicDBObject> getSensorStatusAsDocs(SensorGroup group) {
    ArrayList<BasicDBObject> ret = new ArrayList<BasicDBObject>();
    SensorStatusList statuses = sensorStatus.get(group);
    for (org.wattdepot.common.domainmodel.SensorStatus status : statuses.getStatuses()) {
      ret.add(buildDBObject(status));
    }
    return ret;
  }

  private BasicDBObject buildDBObject(InterpolatedValue value) {
    try {
      BasicDBObject ret = new BasicDBObject("tower", IdHelper.niceifyTowerId(value.getSensorId()))
          .append("value", value.getValue())
          .append("date", DateConvert.convertDate(value.getEnd()).toXMLFormat());
      return ret;
    }
    catch (DatatypeConfigurationException e) {
      e.printStackTrace();
    }
    return null;
  }

  private BasicDBObject buildDBObject(org.wattdepot.common.domainmodel.SensorStatus status) {
    BasicDBObject ret = new BasicDBObject("tower", IdHelper.niceifyTowerId(status.getSensorId()))
        .append("status", status.getStatus().getLabel())
        .append("sensorId", status.getSensorId())
        .append("date", status.getTimestamp().toXMLFormat());
    return ret;
  }

  public ArrayList<SensorGroup> getTowerList() {
    ArrayList<SensorGroup> ret = new ArrayList<SensorGroup>();
    for (SensorGroup g : towerList) {
      ret.add(g);
    }
    return ret;
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

  public void foo() {
    for (SensorGroup tower: towerList) {
      SensorStatusList list = client.getSensorStatuses(power, tower);
      for (org.wattdepot.common.domainmodel.SensorStatus status : list.getStatuses()) {
        System.out.println(buildDBObject(status));
      }
    }

//    System.out.println(towerList);
//    System.out.println(client.getLatestValue(power, towerList.get(1)));
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