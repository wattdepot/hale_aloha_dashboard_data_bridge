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
import org.wattdepot.common.domainmodel.Sensor;
import org.wattdepot.common.domainmodel.SensorGroup;
import org.wattdepot.common.domainmodel.SensorGroupList;
import org.wattdepot.common.domainmodel.SensorList;
import org.wattdepot.common.exception.BadCredentialException;
import org.wattdepot.common.exception.IdNotFoundException;
import org.wattdepot.common.exception.NoMeasurementException;
import org.wattdepot.common.util.DateConvert;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Hello world!
 */
public class App {
  public static void main(String[] args) {
//    try {
//      Set<String> names = db.getCollectionNames();
//      System.out.println("Collection names:");
//      for (String name : names) {
//        System.out.println(name);
//      }
//      DBCollection coll = db.getCollection("monthly");
//      System.out.println("There are " + coll.getCount() + " docs in " + coll.getFullName());
//      DBObject myDoc = coll.findOne();
//      System.out.println(myDoc);
//      coll = db.getCollection("power");
//      BasicDBObject doc = new BasicDBObject("tower", "ilima").append("value", Math.random() * 1000.0).append("createdAt", new Date());
//      coll.insert(doc);
//    }
//    catch (UnknownHostException e) {
//      e.printStackTrace();
//    }
    try {
      MongoClient mongoClient = new MongoClient("localhost", 27017);
      DB db = mongoClient.getDB("hale_aloha");
      WattDepotClient client = new WattDepotClient("http://mopsa.ics.hawaii.edu:8192/", "cmoore", "uhm", "secret1");
      DepositoryList depositories = client.getDepositories();
      Depository power = null;
      Depository energy = null;
      for (Depository d : depositories.getDepositories()) {
        if (d.getId().equals("power")) {
          power = d;
        }
        if (d.getId().equals("energy")) {
          energy = d;
        }
      }
      ArrayList<SensorGroup> towerTotalGroups = new ArrayList<SensorGroup>();
      SensorGroupList groups = client.getSensorGroups();
      for (SensorGroup g : groups.getGroups()) {
        if (g.getId().endsWith("total")) {
          towerTotalGroups.add(g);
        }
      }
      System.out.println(towerTotalGroups);
//      System.out.println(depositories.getDepositories());
      SensorList sensors = client.getSensors();
//      System.out.println(sensors.getSensors().size());
      sensors = client.getDepositorySensors(power.getId());
//      System.out.println(sensors.getSensors().size());
//      System.out.println(sensors.getSensors());
      DBCollection powerCollection = db.getCollection("power");
      ValueFactory factory = ValueFactory.getInstance();
      for (int i = 0; i < 10; i++) {
        for (SensorGroup g : towerTotalGroups) {
          TowerCurrentPower tcp = factory.getCurrentPower(power, g);
          System.out.println(tcp);
          String towerId = tcp.getTowerId();
          int index = towerId.indexOf('-');
          towerId = towerId.substring(0, index);
//        System.out.println(towerId);
          BasicDBObject doc = new BasicDBObject("tower", towerId)
              .append("value", tcp.getCurrentValue())
              .append("minimum", tcp.getHistoricalMin())
              .append("maximum", tcp.getHistoricalMax())
              .append("average", tcp.getHistoricalAve())
              .append("meters", tcp.getNumSensors())
              .append("reporting", tcp.getNumSensorsReporting())
              .append("timestamp", DateConvert.convertXMLCal(tcp.getTimestamp()))
              .append("createdAt", new Date());
          powerCollection.insert(doc);
        }
        //Pause for 4 seconds
        Thread.sleep(15000);
      }

    }
//    catch (NoMeasurementException e) {
//      e.printStackTrace();
//    }
    catch (IdNotFoundException e) {
      e.printStackTrace();
    }
    catch (BadCredentialException e) {
      e.printStackTrace();
    }
    catch (UnknownHostException e) {
      e.printStackTrace();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
