package org.wattdepot.dashboard;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import org.wattdepot.client.http.api.WattDepotClient;
import org.wattdepot.common.domainmodel.Depository;
import org.wattdepot.common.domainmodel.DescriptiveStats;
import org.wattdepot.common.domainmodel.InterpolatedValue;
import org.wattdepot.common.domainmodel.InterpolatedValueList;
import org.wattdepot.common.domainmodel.SensorGroup;
import org.wattdepot.common.domainmodel.SensorStatusList;
import org.wattdepot.common.exception.BadCredentialException;
import org.wattdepot.common.exception.IdNotFoundException;
import org.wattdepot.common.util.DateConvert;
import org.wattdepot.common.util.tstamp.Tstamp;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DataBridge - The bridge between WattDepot and the Hale Aloha Dashboard DataBridgeMain.
 *
 * @author Cam Moore
 */
public class DataBridge {
  /**
   * The host name of the Mongo db.
   */
  private static final String MONGODB_HOST = "localhost";
  /**
   * The port number of the Mongo db.
   */
  private static final int MONGODB_PORT = 27017;
  /**
   * The name of the Hale Aloha Dashboard database.
   */
  private static final String DB_NAME = "hale_aloha";
  /**
   * The power collection name.
   */
  private static final String POWER = "power";
  /**
   * The hourly energy collection name.
   */
  private static final String HOURLY = "hourly";
  /**
   * The daily energy collection name.
   */
  private static final String DAILY = "daily";
  /**
   * The predicted hourly energy collection name.
   */
  private static final String PREDICTED_HOURLY = "predictedHourly";
  /**
   * The predicted daily energy collection name.
   */
  private static final String PREDICTED_DAILY = "predictedDaily";
  /**
   * The sensor status collection name.
   */
  private static final String STATUS = "status";
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
   * The name of the energy depository.
   */
  private static final String ENERGY = "energy";
  /**
   * The singleton instance.
   */
  private static DataBridge instance;
  /**
   * The power collection.
   */
  private DBCollection powerCollection;
  /**
   * The hourly data collection.
   */
  private DBCollection hourlyCollection;
  /**
   * The daily data collection.
   */
  private DBCollection dailyCollection;
  /**
   * The predicted hourly energy collection.
   */
  private DBCollection predictedHourlyCollection;
  /**
   * The predicted daily energy collection.
   */
  private DBCollection predictedDailyCollection;
  /**
   * The sensor status collection.
   */
  private DBCollection statusCollection;
  /**
   * The connection to the Hale Aloha Dashboard MongoDB.
   */
  private MongoClient mongoClient;
  /**
   * The database holding the dashboard data.
   */
  private DB db;
  /**
   * The WattDepotClient.
   */
  private WattDepotClient client;
  /**
   * The power depository.
   */
  private Depository powerDepository;
  /**
   * The energy depository.
   */
  private Depository energyDepository;
  /**
   * A list of the tower SensorGroups.
   */
  private ArrayList<SensorGroup> towerList;
  /**
   * The houly power history for each tower. Should be updated hourly.
   */
  private Map<SensorGroup, DescriptiveStats> powerHistory;

  private static Logger logger = Logger.getLogger("data_bridge");

  /**
   * Creates a new DataBridge initializing the MongoDB and WattDepot clients.
   *
   * @throws UnknownHostException   if there is a problem with the MongoDB client.
   * @throws BadCredentialException if there is a problem with the WattDepot client.
   */
  private DataBridge() throws UnknownHostException, BadCredentialException, IdNotFoundException {
    this.mongoClient = new MongoClient(MONGODB_HOST, MONGODB_PORT);
    this.db = mongoClient.getDB(DB_NAME);
    this.powerCollection = db.getCollection(POWER);
    this.hourlyCollection = db.getCollection(HOURLY);
    this.dailyCollection = db.getCollection(DAILY);
    this.statusCollection = db.getCollection(STATUS);
    this.predictedHourlyCollection = db.getCollection(PREDICTED_HOURLY);
    this.predictedDailyCollection = db.getCollection(PREDICTED_DAILY);
    this.client = new WattDepotClient(SERVER_URL, CLIENT_NAME, CLIENT_ORG, CLIENT_PASSWORD);
    this.powerDepository = client.getDepository(POWER);
    this.energyDepository = client.getDepository(ENERGY);
    this.towerList = new ArrayList<SensorGroup>();
    // Initialize the tower list. We only need to do this once.
    for (SensorGroup g : client.getSensorGroups().getGroups()) {
      if (g.getId().endsWith("-total")) {
        towerList.add(g);
      }
    }
    this.powerHistory = new HashMap<SensorGroup, DescriptiveStats>();
    ConsoleHandler handler = new ConsoleHandler();
    logger.addHandler(handler);
    logger.setLevel(Level.FINEST);
    initializeDailyEnergy();
    initializeHourlyEnergy();
  }

  /**
   * @return The singleton instance or null if there is a problem with the clients.
   */
  public static DataBridge getInstance() {
    if (instance == null) {
      try {
        instance = new DataBridge();
      }
      catch (UnknownHostException e) {
        e.printStackTrace();
      }
      catch (BadCredentialException e) {
        e.printStackTrace();
      }
      catch (IdNotFoundException e) {
        e.printStackTrace();
      }
    }
    return instance;
  }

  /**
   * Updates the Hale Aloha current power. Removes any old power information before inserting the latest value.
   *
   * @return The number of power updates sent to the dashboard.
   */
  public Integer updateCurrentPower() {
    Integer ret = 0;
    for (SensorGroup group : towerList) {
      InterpolatedValue value = client.getLatestValue(powerDepository, group);
      BasicDBObject doc = buildPowerDBObject(value);
      BasicDBObject remove = new BasicDBObject("tower", IdHelper.niceifyTowerId(group.getId()));
      powerCollection.remove(remove);
      powerCollection.insert(doc);
      ret++;
    }
    return ret;
  }

  /**
   * Clears the database of all daily energy data and inserts 30 days worth of usage data and 7 days of predicted.
   */
  public void initializeDailyEnergy() {
    clearDailyEnergy();
    XMLGregorianCalendar now = Tstamp.makeTimestamp();
    XMLGregorianCalendar thirtyDaysAgo = Tstamp.incrementDays(now, -30);
    XMLGregorianCalendar nextSeven = Tstamp.incrementDays(now, 6); // going to get historical data for today also.
    InterpolatedValueList data = null;
    ArrayList<BasicDBObject> objects = null;
    for (SensorGroup group : towerList) {
      logger.info("loading daily energy data for tower " + IdHelper.niceifyTowerId(group.getId()));
      // 30 days of historical usage
      data = getDailyEnergyData(group, thirtyDaysAgo, now);
      objects = buildDBFromInterpolatedValues(data);
      for (BasicDBObject doc : objects) {
        this.dailyCollection.insert(doc);
      }
      // Get next 7 day predictions
      List<XMLGregorianCalendar> times = Tstamp.getTimestampList(now, nextSeven, 24 * 60);
      if (times != null) {
        for (int i = 0; i < times.size(); i++) {
          XMLGregorianCalendar time = times.get(i);
          data = getHistoricalDailyEnergyData(group, time, 7);
          BasicDBObject histObj = buildDailyHistoryDBObject(data);
          this.predictedDailyCollection.insert(histObj);
        }
      }
    }
  }

  /**
   * Updates the daily energy and history for each tower.
   *
   * @return The number of entries sent to the Hale Aloha Dashboard.
   */
  public void updateDailyEnergy() {
    XMLGregorianCalendar now = Tstamp.makeTimestamp();
    InterpolatedValueList data = null;
    ArrayList<BasicDBObject> objects = null;
    for (SensorGroup group : towerList) {
      BasicDBObject find = new BasicDBObject("tower", IdHelper.niceifyTowerId(group.getId()));
      DBObject mostRecent = null;
      for (DBObject rec : this.dailyCollection.find(find)) {
        if (mostRecent == null) {
          mostRecent = rec;
        }
        else if (((Date) mostRecent.get("date")).getTime() < ((Date) rec.get("date")).getTime()) {
          mostRecent = rec;
        }
      }
      try {
        XMLGregorianCalendar last = DateConvert.convertDate(((Date) mostRecent.get("date")));
        data = getDailyEnergyData(group, last, now);
        objects = buildDBFromInterpolatedValues(data);
        logger.info(IdHelper.niceifyTowerId(group.getId()) + ": " + objects.size() + " records added");
        for (BasicDBObject doc : objects) {
          this.dailyCollection.insert(doc);
        }
        // do we need to create prediction?
        for (InterpolatedValue value : data.getInterpolatedValues()) {
          XMLGregorianCalendar time = DateConvert.convertDate(value.getStart());
          logger.info("prediction for " + time + " made");
          InterpolatedValueList histData = getHistoricalDailyEnergyData(group, time, 7);
          BasicDBObject histObj = buildDailyHistoryDBObject(histData);
          this.predictedDailyCollection.insert(histObj);
        }
      }
      catch (DatatypeConfigurationException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Removes all the daily energy data from the database.
   *
   * @return The number of entries removed.
   */
  private Integer clearDailyEnergy() {
    Integer integer = null;
    for (SensorGroup group : towerList) {
      logger.info("clearing daily data for " + IdHelper.niceifyTowerId(group.getId()));
      BasicDBObject remove = new BasicDBObject("tower", IdHelper.niceifyTowerId(group.getId()));
      WriteResult result = this.dailyCollection.remove(remove);
      if (integer == null) {
        integer = new Integer(0);
      }
      integer += result.getN();
      this.predictedDailyCollection.remove(remove);
    }
    return integer;
  }

  /**
   * Initializes the hourly energy data, both the historical usage and the predicted usage for 24 hours.
   */
  public void initializeHourlyEnergy() {
    clearHourlyEnergy();
    XMLGregorianCalendar now = Tstamp.makeTimestamp();
    XMLGregorianCalendar oneDayAgo = Tstamp.incrementDays(now, -1);
    XMLGregorianCalendar nextDay = Tstamp.incrementDays(now, 1);
    InterpolatedValueList data = null;
    ArrayList<BasicDBObject> objects = null;
    for (SensorGroup group : towerList) {
      logger.info("loading hourly energy data for tower " + IdHelper.niceifyTowerId(group.getId()));
      data = getHourlyEnergyData(group, oneDayAgo, now);
      objects = buildDBFromInterpolatedValues(data);
      for (BasicDBObject doc : objects) {
        this.hourlyCollection.insert(doc);
      }
      // get the prediction for the next 24 hours
      List<XMLGregorianCalendar> times = Tstamp.getTimestampList(now, nextDay, 60);
      if (times != null) {
        for (int i = 1; i < times.size(); i++) {
          XMLGregorianCalendar time = times.get(i);
          data = getHistoricalHourlyEnergyData(group, time, 7);
          BasicDBObject histObj = buildHourlyHistoryDBObject(data);
          this.predictedHourlyCollection.insert(histObj);
        }
      }
    }
  }

  /**
   * Updates the hourly energy, returning the number of entries added to the Hale Aloha Dashboard.
   *
   * @return The number of entries added to the Hale Aloha Dashboard.
   */
  public void updateHourlyEnergy() {
    XMLGregorianCalendar now = Tstamp.makeTimestamp();
    InterpolatedValueList data = null;
    ArrayList<BasicDBObject> objects = null;
    for (SensorGroup group : towerList) {
      BasicDBObject find = new BasicDBObject("tower", IdHelper.niceifyTowerId(group.getId()));
      DBObject mostRecent = null;
      for (DBObject rec : this.hourlyCollection.find(find)) {
        if (mostRecent == null) {
          mostRecent = rec;
        }
        else if (((Date) mostRecent.get("date")).getTime() < ((Date) rec.get("date")).getTime()) {
          mostRecent = rec;
        }
      }
      try {
        XMLGregorianCalendar last = DateConvert.convertDate(((Date) mostRecent.get("date")));
        data = getHourlyEnergyData(group, last, now);
        objects = buildDBFromInterpolatedValues(data);
        logger.info(IdHelper.niceifyTowerId(group.getId()) + ": " + objects.size() + " records added");
        for (BasicDBObject doc : objects) {
          this.hourlyCollection.insert(doc);
        }
        // do we need to create prediction?
        for (InterpolatedValue value : data.getInterpolatedValues()) {
          XMLGregorianCalendar time = DateConvert.convertDate(value.getStart());
          logger.info("prediction for " + time + " made");
          InterpolatedValueList histData = getHistoricalHourlyEnergyData(group, time, 7);
          BasicDBObject histObj = buildHourlyHistoryDBObject(histData);
          this.predictedHourlyCollection.insert(histObj);
        }
      }
      catch (DatatypeConfigurationException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * Removes all the hourly energy data from the database.
   *
   * @return The number of entries removed.
   */
  private Integer clearHourlyEnergy() {
    Integer integer = null;
    for (SensorGroup group : towerList) {
      logger.info("clearing hourly data for " + IdHelper.niceifyTowerId(group.getId()));
      BasicDBObject remove = new BasicDBObject("tower", IdHelper.niceifyTowerId(group.getId()));
      WriteResult result = this.hourlyCollection.remove(remove);
      if (integer == null) {
        integer = new Integer(0);
      }
      integer += result.getN();
      this.predictedHourlyCollection.remove(remove);
    }
    return integer;
  }

  /**
   * Updates the hourly power history for each tower. Should be run once an hour.
   */
  public void updatePowerHistory() {
    for (SensorGroup tower : towerList) {
      logger.info("update power history for " + IdHelper.niceifyTowerId(tower.getId()));
      DescriptiveStats values = client.getDescriptiveStats(powerDepository, tower, new Date(), false, 5, true);
      powerHistory.put(tower, values);
    }
  }

  /**
   * Updates the Hale Aloha Sensor Status.
   *
   * @return The number statues sent to the dashboard.
   */
  public Integer updateSensorStatus() {
    Integer ret = 0;
    for (SensorGroup group : towerList) {
      logger.info("update sensor status for " + IdHelper.niceifyTowerId(group.getId()));
      SensorStatusList statusList = client.getSensorStatuses(powerDepository, group);
      ArrayList<BasicDBObject> objects = buildDBFromSensorStatusList(statusList);
      ret += objects.size();
      for (BasicDBObject doc : objects) {
        statusCollection.insert(doc);
      }
    }
    return ret;
  }

  private XMLGregorianCalendar beginningOfHour(XMLGregorianCalendar when) {
    XMLGregorianCalendar begin = Tstamp.makeTimestamp(when.toGregorianCalendar().getTimeInMillis());
    begin.setMinute(0);
    begin.setSecond(0);
    begin.setMillisecond(0);
    return begin;
  }

  /**
   * Builds BasicDBObjects representing the InterpolatedValues.
   *
   * @param list the list of InterpolatedValues.
   * @return the list of BasicDBObjects.
   */
  private ArrayList<BasicDBObject> buildDBFromInterpolatedValues(InterpolatedValueList list) {
    ArrayList<BasicDBObject> ret = new ArrayList<BasicDBObject>();
    assert (list != null);
    for (InterpolatedValue value : list.getInterpolatedValues()) {
      ret.add(buildIvDbObject(value));
    }
    return ret;
  }

  /**
   * Builds BasicDBObjects representing the SensorStatuses.
   *
   * @param list the list of SensorStatuses.
   * @return the list of BasicDBObjects.
   */
  private ArrayList<BasicDBObject> buildDBFromSensorStatusList(SensorStatusList list) {
    ArrayList<BasicDBObject> ret = new ArrayList<BasicDBObject>();
    assert (list != null);
    for (org.wattdepot.common.domainmodel.SensorStatus status : list.getStatuses()) {
      ret.add(buildStatusDbObject(status));
    }
    return ret;
  }

  /**
   * Builds a BasicDBObject from the SensorStatus.
   *
   * @param status the SensorStatus.
   * @return the BasicDBObject.
   */
  private BasicDBObject buildStatusDbObject(org.wattdepot.common.domainmodel.SensorStatus status) {
    BasicDBObject ret = new BasicDBObject("tower", IdHelper.niceifyTowerId(status.getSensorId()))
        .append("status", status.getStatus().getLabel())
        .append("sensorId", status.getSensorId())
        .append("date", status.getTimestamp().toXMLFormat())
        .append("createdAt", new Date());
    return ret;
  }

  /**
   * Builds a BasicDBObject from the InterpolatedValue.
   *
   * @param value the InterpolatedValue.
   * @return the BasicDBObject.
   */
  private BasicDBObject buildIvDbObject(InterpolatedValue value) {
    BasicDBObject ret = new BasicDBObject("tower", IdHelper.niceifyTowerId(value.getSensorId()))
        .append("value", value.getValue())
        .append("date", value.getEnd())
        .append("createdAt", new Date());
    return ret;
  }

  private BasicDBObject buildDailyHistoryDBObject(InterpolatedValueList interpolatedValueList) {
    if (interpolatedValueList.getInterpolatedValues().size() > 0) {
      SimpleDateFormat format = new SimpleDateFormat("MM/dd");
      InterpolatedValue first = interpolatedValueList.getInterpolatedValues().get(0);
      XMLGregorianCalendar time = Tstamp.makeTimestamp(first.getStart().getTime());
      time = Tstamp.incrementDays(time, 7);
      BasicDBObject ret = new BasicDBObject("tower", IdHelper.niceifyTowerId(first.getSensorId()))
          .append("date", first.getStart())
          .append("label", format.format(DateConvert.convertXMLCal(time)))
          .append("createdAt", new Date());
      BasicDBList values = new BasicDBList();
      for (InterpolatedValue v : interpolatedValueList.getInterpolatedValues()) {
        values.add(v.getValue() / 1000.0); // put into kW instead of W.
      }
      ret.append("values", values);
      return ret;
    }
    return null;
  }

  private BasicDBObject buildHourlyHistoryDBObject(InterpolatedValueList interpolatedValueList) {
    if (interpolatedValueList.getInterpolatedValues().size() > 0) {
      SimpleDateFormat format = new SimpleDateFormat("hha");
      InterpolatedValue first = interpolatedValueList.getInterpolatedValues().get(0);
      XMLGregorianCalendar time = Tstamp.makeTimestamp(first.getStart().getTime());
      time = Tstamp.incrementDays(time, 7);
      BasicDBObject ret = new BasicDBObject("tower", IdHelper.niceifyTowerId(first.getSensorId()))
          .append("date", first.getStart())
          .append("label", format.format(DateConvert.convertXMLCal(time)))
          .append("createdAt", new Date());
      BasicDBList values = new BasicDBList();
      for (InterpolatedValue v : interpolatedValueList.getInterpolatedValues()) {
        values.add(v.getValue() / 1000.0); // put into kW instead of W.
      }
      ret.append("values", values);
      return ret;
    }
    return null;
  }

  private BasicDBObject buildPowerDBObject(InterpolatedValue value) {
    SensorGroup group = null;
    for (SensorGroup g : towerList) {
      if (g.getId().equals(value.getSensorId())) {
        group = g;
      }
    }
    Double currentPower = value.getValue();
    BasicDBObject ret = null;
    DescriptiveStats val = powerHistory.get(group);
    if (val != null) {
      Double min = val.getMinimum();
      if (min.isNaN()) {
        min = value.getValue() / 2;
      }
      if (min > currentPower) {
        min = currentPower - 0.1 * currentPower;
      }
      Double max = val.getMaximum();
      if (max.isNaN()) {
        max = value.getValue() * 1.5;
      }
      if (max < currentPower) {
        max = currentPower + 0.1 * currentPower;
      }
      Double ave = val.getAverage();
      if (ave.isNaN()) {
        ave = currentPower;
      }
      ret = new BasicDBObject("tower", IdHelper.niceifyTowerId(value.getSensorId()))
          .append("value", currentPower)
          .append("minimum", min)
          .append("maximum", max)
          .append("average", ave)
          .append("meters", value.getDefinedSensors().size())
          .append("reporting", value.getReportingSensors().size())
          .append("timestamp", value.getEnd())
          .append("createdAt", new Date());
    }
    else {
      currentPower = value.getValue();
      Double min = currentPower - 0.5 * currentPower;
      Double max = currentPower + 0.5 * currentPower;
      ret = new BasicDBObject("tower", IdHelper.niceifyTowerId(value.getSensorId()))
          .append("value", currentPower)
          .append("minimum", min)
          .append("maximum", max)
          .append("average", 7500.0)
          .append("meters", value.getDefinedSensors().size())
          .append("reporting", value.getReportingSensors().size())
          .append("timestamp", value.getEnd())
          .append("createdAt", new Date());
    }
    return ret;
  }

  private XMLGregorianCalendar endingOfHour(XMLGregorianCalendar when) {
    XMLGregorianCalendar end = Tstamp.incrementHours(when, 1);
    end.setMinute(0);
    end.setSecond(0);
    end.setMillisecond(0);
    return end;
  }

  /**
   * Gets the daily energy data.
   *
   * @param group the group to get the data for.
   * @param start The beginning datetime.
   * @param end   The ending datetime.
   * @return An InterpolatedValueList of the daily energy data.
   */
  private InterpolatedValueList getDailyEnergyData(SensorGroup group, XMLGregorianCalendar start, XMLGregorianCalendar end) {
    InterpolatedValueList dailyData = this.client.getDailyValues(this.energyDepository, group,
        DateConvert.convertXMLCal(start), DateConvert.convertXMLCal(end), false);
    return dailyData;
  }

  /**
   * Gets the hourly energy data.
   *
   * @param group the group to get the data for.
   * @param start The beginning datetime.
   * @param end   The ending datetime.
   * @return An InterpolatedValueList of the hourly energy data.
   */
  private InterpolatedValueList getHourlyEnergyData(SensorGroup group, XMLGregorianCalendar start,
                                                    XMLGregorianCalendar end) {
    InterpolatedValueList hourlyData = this.client.getHourlyValues(this.energyDepository, group,
        DateConvert.convertXMLCal(start), DateConvert.convertXMLCal(end), false);
    return hourlyData;
  }

  /**
   * Gets the hourly historical data.
   *
   * @param group      the group to get the data for.
   * @param time       the start time for the historical data. Historical data is weekly one week before.
   * @param numSamples the number of weeks to go back.
   * @return An InterpolatedValueList of the historical data.
   */
  private InterpolatedValueList getHistoricalHourlyEnergyData(SensorGroup group, XMLGregorianCalendar time, Integer numSamples) {
    InterpolatedValueList hourlyData = this.client.getHistoricalValues(this.energyDepository, group, DateConvert.convertXMLCal(time), false, numSamples, false);
    return hourlyData;
  }

  /**
   * Gets the daily historical data.
   *
   * @param group      the group to get the data for.
   * @param time       the start time for the historical data. Historical data is weekly one week before.
   * @param numSamples the number of weeks to go back.
   * @return An InterpolatedValueList of the historical data.
   */
  private InterpolatedValueList getHistoricalDailyEnergyData(SensorGroup group, XMLGregorianCalendar time, Integer numSamples) {
    InterpolatedValueList dailyData = this.client.getHistoricalValues(this.energyDepository, group, DateConvert.convertXMLCal(time), true, numSamples, false);
    return dailyData;
  }
}
