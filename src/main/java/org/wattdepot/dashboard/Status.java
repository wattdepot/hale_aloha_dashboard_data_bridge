package org.wattdepot.dashboard;

/**
 * Created by carletonmoore on 8/11/15.
 */
public enum Status {
  BLACK("black"), RED("red"), YELLOW("yellow"), GREEN("green");

  private String label;
  Status(String str) {
    this.label = str;
  }

  String getLabel() {
    return label;
  }
}
