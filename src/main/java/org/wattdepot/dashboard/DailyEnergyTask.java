package org.wattdepot.dashboard;

import java.util.TimerTask;

/**
 * DailyEnergyTask - A TimerTask that updates the Daily Energy data. Should be run daily.
 * @author Cam Moore
 */
public class DailyEnergyTask extends TimerTask {
  private DataBridge dataBridge = DataBridge.getInstance();
  @Override
  public void run() {
    dataBridge.updateDailyEnergy();
  }
}
