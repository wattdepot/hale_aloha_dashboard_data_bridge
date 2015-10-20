/**
 * This file is a part of hale_aloha_dashboard_data_bridge.
 * <p/>
 * Created by Cam Moore on 10/19/15.
 * <p/>
 * Copyright (C) 2015 Cam Moore.
 * <p/>
 * The MIT License (MIT)
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy , modify, merge, publish, deistribute, sublicense, and/or sell
 * copies of the Software, and to permit person to whom the Software is
 * furnished to do so, subject to the following condtions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHOERS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETER IN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISIGN FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */
package org.wattdepot.dashboard;

import junit.framework.TestCase;

/**
 * Test cases for the DataBridge class.
 *
 * @author Cam Moore
 */
public class DataBridgeTest extends TestCase {

  public void testGetInstance() throws Exception {
    DataBridge dataBridge = DataBridge.getInstance();
    assertNotNull(dataBridge);

  }

  public void testUpdateDailyEnergy() throws Exception {
    DataBridge dataBridge = DataBridge.getInstance();
    dataBridge.updateDailyEnergy();
  }

  public void testUpdateHourlyEnergy() throws Exception {
    DataBridge dataBridge = DataBridge.getInstance();
    dataBridge.updateHourlyEnergy();

  }

}