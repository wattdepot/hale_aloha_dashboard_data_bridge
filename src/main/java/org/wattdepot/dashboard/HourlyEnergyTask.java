package org.wattdepot.dashboard;

import java.util.TimerTask;

/**
 * HourlyEnergyTask - A TimerTask that updates the Hourly Energy data. Should be run hourly.
 *
 * @author Cam Moore
 */
public class HourlyEnergyTask extends TimerTask {
  private DataBridge dataBridge = DataBridge.getInstance();

  @Override
  public void run() {
    dataBridge.updateHourlyEnergy();
  }
}
