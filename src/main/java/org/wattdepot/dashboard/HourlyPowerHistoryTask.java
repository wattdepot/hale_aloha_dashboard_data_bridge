package org.wattdepot.dashboard;

import java.util.TimerTask;

/**
 * HourlyPowerHistoryTask - TimerTask that updates the power history. Should be run every hour.
 *
 * @author Cam Moore
 */
public class HourlyPowerHistoryTask extends TimerTask {
  private DataBridge dataBridge = DataBridge.getInstance();

  @Override
  public void run() {
    dataBridge.updatePowerHistory();
  }
}
