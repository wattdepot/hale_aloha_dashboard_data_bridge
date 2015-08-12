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

    try {
      MongoClient mongoClient = new MongoClient("localhost", 27017);
      DB db = mongoClient.getDB("hale_aloha");


      DBCollection powerCollection = db.getCollection("power");
      ValueFactory factory = ValueFactory.getInstance();

      factory.updateCurrentPower();

      CurrentPower power = factory.getCurrentPower("mokihana-4th-telco");
      System.out.println(factory.getCurrentStatus("mokihana-4th-telco"));
      System.out.println(power);
      power = factory.getCurrentPower("lehua-4th-telco");
      System.out.println(factory.getCurrentStatus("lehua-4th-telco"));
      System.out.println(power);
    }
    catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }
}
