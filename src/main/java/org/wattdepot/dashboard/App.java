package org.wattdepot.dashboard;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.BulkWriteResult;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ParallelScanOptions;
import com.mongodb.ServerAddress;
import org.wattdepot.client.http.api.WattDepotClient;
import org.wattdepot.common.domainmodel.Depository;
import org.wattdepot.common.domainmodel.DepositoryList;
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

import javax.xml.datatype.DatatypeConfigurationException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Runs the data bridge between WattDepot and the Hale Aloha Dashboard.
 */
public class App {
  /**
   * Main method.
   * @param args command line arguments, ignored.
   */
  public static void main(String[] args)  {
    // ensure there is history.
    DataBridge bridge = DataBridge.getInstance();
    System.out.println("Initializing values.");
    System.out.print("power history, ");
    bridge.updatePowerHistory();
    System.out.print("hourly energy, ");
    bridge.updateHourlyEnergy();
    System.out.println("daily energy, ");
    bridge.updateDailyEnergy();
    System.out.println("sensor status.");
    bridge.updateSensorStatus();
    System.out.println("Starting tasks.");
    Timer timer = new Timer();
    timer.schedule(new CurrentPowerTask(), 100, 15000);
    timer.schedule(new HourlyEnergyTask(), 500, 60 * 60 * 1000);
    timer.schedule(new SensorStatusTask(), 750, 60 * 60 * 1000);
    timer.schedule(new DailyEnergyTask(), 1500, 24 * 60 * 60 * 1000);
  }
}
