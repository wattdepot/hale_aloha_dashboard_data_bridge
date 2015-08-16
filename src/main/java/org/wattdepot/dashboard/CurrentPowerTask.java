package org.wattdepot.dashboard;

import java.util.TimerTask;

/**
 * CurrentPowerTask - TimerTask that updates the current power for each tower. Should be run about every 15 seconds.
 */
public class CurrentPowerTask extends TimerTask {
  private DataBridge dataBridge = DataBridge.getInstance();

  @Override
  public void run() {
    dataBridge.updateCurrentPower();
  }
}
