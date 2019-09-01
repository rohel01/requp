package fr.melchiore.tools.requp;

import java.util.HashMap;
import java.util.Map;

public enum Compliance {
  UNSET("TBD"),
  NOT_APPLICABLE("NA"),
  NOT_COMPLIANT("NC"),
  COMPLIANT("C"),
  PARTIALLY_COMPLIANT("PC");

  private static Map<String, Compliance> abrvToEnum;

  static {
    abrvToEnum = new HashMap<>();
    for (Compliance value : Compliance.values()) {
      abrvToEnum.put(value.abrv, value);
    }
  }

  // Required for FreeMarker
  public String getAbrv() {
    return abrv;
  }

  public final String abrv;

  Compliance(String abrv) {
    this.abrv = abrv;
  }

  public static Compliance fromAbrv(String abrv) {
    return abrvToEnum.getOrDefault(abrv.trim(), Compliance.UNSET);
  }
}
