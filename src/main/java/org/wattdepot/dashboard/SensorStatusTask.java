package org.wattdepot.dashboard;

import java.util.TimerTask;

/**
 * SensorStatusTask - Timer Task to update the Sensor Statuses. Should be run every 5 minutes.
 */
public class SensorStatusTask extends TimerTask{
  private DataBridge dataBridge = DataBridge.getInstance();

  @Override
  public void run() {
    dataBridge.updateSensorStatus();
  }
}
