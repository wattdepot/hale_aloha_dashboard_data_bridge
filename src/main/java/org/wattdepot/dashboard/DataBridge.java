package org.wattdepot.dashboard;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import org.wattdepot.client.http.api.WattDepotClient;
import org.wattdepot.common.domainmodel.Depository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * DataBridge - The bridge between WattDepot and the Hale Aloha Dashboard App.
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
   * The sensor status collection name.
   */
  private static final String STATUS = "status";
  /**
   * The singleton instance.
   */
  private static DataBridge instance;
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
   * The connection to the Hale Aloha Dashboard MongoDB.
   */
  private MongoClient mongoClient;
  /**
   * The database holding the dashboard data.
   */
  private DB db;
  /**
   * The power collection.
   */
  DBCollection powerCollection;
  /**
   * The hourly data collection.
   */
  DBCollection hourlyCollection;
  /**
   * The daily data collection.
   */
  DBCollection dailyCollection;
  /**
   * The sensor status collection.
   */
  DBCollection statusCollection;
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
   * cache of the hourly energy data for each tower.
   */
  private Map<SensorGroup, InterpolatedValueList> last24HoursEnergy;
  /**
   * cache of the daily energy data for each tower.
   */
  private Map<SensorGroup, InterpolatedValueList> dailyEnergy;
  /**
   * cache of the sensor statuses for each tower.
   */
  private Map<SensorGroup, SensorStatusList> sensorStatus;
  /**
   * The timestamp of the last hourly energy update.
   */
  private XMLGregorianCalendar lastHourlyEnergyUpdate;
  /**
   * The timestamp of the last daily energy update.
   */
  private XMLGregorianCalendar lastDailyEnergyUpdate;
  /**
   * The timestamp of the last sensor status update.
   */
  private XMLGregorianCalendar lastSensorStatusUpdate;

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
    this.client = new WattDepotClient(SERVER_URL, CLIENT_NAME, CLIENT_ORG, CLIENT_PASSWORD);
    this.powerDepository = client.getDepository(POWER);
    this.energyDepository = client.getDepository(ENERGY);
    this.towerList = new ArrayList<SensorGroup>();
    this.last24HoursEnergy = new HashMap<SensorGroup, InterpolatedValueList>();
    this.dailyEnergy = new HashMap<SensorGroup, InterpolatedValueList>();
    this.sensorStatus = new HashMap<SensorGroup, SensorStatusList>();
    // Initialize the tower list. We only need to do this once.
    for (SensorGroup g : client.getSensorGroups().getGroups()) {
      if (g.getId().endsWith("-total")) {
        towerList.add(g);
      }
    }
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
   * Updates the hourly energy, returning the number of entries added to the Hale Aloha Dashboard.
   *
   * @return The number of entries added to the Hale Aloha Dashboard.
   */
  public Integer updateHourlyEnergy() {
    Integer ret = 0;
    XMLGregorianCalendar now = Tstamp.makeTimestamp();
    for (SensorGroup group : towerList) {
      InterpolatedValueList data = null;
      ArrayList<BasicDBObject> objects = null;
      if (lastHourlyEnergyUpdate == null) {
        XMLGregorianCalendar oneDayAgo = Tstamp.incrementDays(now, -1);
        data = getHourlyEnergyData(group, oneDayAgo, now);
      }
      else {
        data = getHourlyEnergyData(group, lastHourlyEnergyUpdate, now);
      }
      objects = buildDBFromInterpolatedValues(data);
      ret += objects.size();
      for (BasicDBObject doc : objects) {
        this.hourlyCollection.insert(doc);
      }
    }
    lastHourlyEnergyUpdate = now;
    return ret;
  }

  public Integer updateDailyEnergy() {
    Integer ret = 0;
    XMLGregorianCalendar now = Tstamp.makeTimestamp();
    for (SensorGroup group : towerList) {
      InterpolatedValueList data = null;
      ArrayList<BasicDBObject> objects = null;
      if (lastDailyEnergyUpdate == null) {
        XMLGregorianCalendar thirtyDaysAgo = Tstamp.incrementDays(now, -30);
        data = getDailyEnergyData(group, thirtyDaysAgo, now);
      }
      else {
        data = getDailyEnergyData(group, lastDailyEnergyUpdate, now);
      }
      objects = buildDBFromInterpolatedValues(data);
      ret += objects.size();
      for (BasicDBObject doc : objects) {
        this.dailyCollection.insert(doc);
      }
    }
    lastDailyEnergyUpdate = now;
    return ret;
  }

  private InterpolatedValueList getDailyEnergyData(SensorGroup group, XMLGregorianCalendar start, XMLGregorianCalendar end) {
    InterpolatedValueList dailyData = this.client.getDailyValues(this.energyDepository, group,
        DateConvert.convertXMLCal(start), DateConvert.convertXMLCal(end), false);
    return dailyData;
  }

  private InterpolatedValueList getHourlyEnergyData(SensorGroup group, XMLGregorianCalendar start,
                                                    XMLGregorianCalendar end) {
    InterpolatedValueList hourlyData = this.client.getHourlyValues(this.energyDepository, group,
        DateConvert.convertXMLCal(start), DateConvert.convertXMLCal(end), false);
    return hourlyData;
  }

  private ArrayList<BasicDBObject> buildDBFromInterpolatedValues(InterpolatedValueList list) {
    ArrayList<BasicDBObject> ret = new ArrayList<BasicDBObject>();
    assert (list != null);
    for (InterpolatedValue value : list.getInterpolatedValues()) {
      ret.add(buildDBObject(value));
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

}
