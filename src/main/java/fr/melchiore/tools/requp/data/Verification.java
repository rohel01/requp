package fr.melchiore.tools.requp.data;

import java.util.HashMap;
import java.util.Map;

public enum Verification {
  INSPECTION("I"),
  ANALYSIS("A"),
  DESIGN_REVIEW("D"),
  TEST("T");

  private static Map<String, Verification> abrvToEnum;

  static {
    abrvToEnum = new HashMap<>();
    for (Verification value : Verification.values()) {
      abrvToEnum.put(value.abrv, value);
    }
  }

  // Required for FreeMarker
  public String getAbrv() {
    return abrv;
  }

  public final String abrv;

  Verification(String abrv) {
    this.abrv = abrv;
  }

  public static Verification fromAbrv(String abrv) {
    return abrvToEnum.get(abrv.trim());
  }
}
