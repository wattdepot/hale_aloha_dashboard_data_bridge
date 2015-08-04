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

import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Hello world!
 */
public class App {
  public static void main(String[] args) {
    System.out.println("Hello World!");
    try {
      MongoClient mongoClient = new MongoClient("localhost", 27017);
      DB db = mongoClient.getDB("hale_aloha");
      Set<String> names = db.getCollectionNames();
      System.out.println("Collection names:");
      for (String name : names) {
        System.out.println(name);
      }
      DBCollection coll = db.getCollection("monthly");
      System.out.println("There are " + coll.getCount() + " docs in " + coll.getFullName());
      DBObject myDoc = coll.findOne();
      System.out.println(myDoc);
      coll = db.getCollection("power");
      BasicDBObject doc = new BasicDBObject("tower", "ilima").append("value", Math.random() * 1000.0).append("createdAt", new Date());
      coll.insert(doc);
    }
    catch (UnknownHostException e) {
      e.printStackTrace();
    }

  }
}
