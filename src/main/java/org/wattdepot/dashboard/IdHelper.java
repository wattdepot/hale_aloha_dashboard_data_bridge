package org.wattdepot.dashboard;

import org.apache.commons.lang.WordUtils;

/**
 * Created by Carleton on 8/14/2015.
 */
public class IdHelper {
  public static String niceifyTowerId(String towerId) {
    int index = towerId.indexOf("-");
    return /*WordUtils.capitalize(*/towerId.substring(0, index)/*)*/;
  }
}
