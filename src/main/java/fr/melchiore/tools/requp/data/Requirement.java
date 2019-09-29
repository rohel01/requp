package fr.melchiore.tools.requp.data;

import java.util.*;

public class Requirement {

  private String ref;
  private String version;
  private String type;
  private String summary;
  private List<String> satisfies;
  private List<String> subsystems;
  private List<Verification> verification;
  private Compliance compliance;
  private String target;
  private String body;
  private String note;

  public Requirement(String ref,
      String version,
      String type,
      String summary,
      List<String> satisfies,
      List<String> subsystems,
      List<Verification> verification,
      Compliance compliance,
      String target,
      String body,
      String note) {
    this.satisfies = new ArrayList<>();
    this.subsystems = new ArrayList<>();

    this.ref = ref.trim();
    this.version = version.trim();
    this.type = type.trim();
    this.summary = summary.trim();
    this.satisfies.addAll(satisfies);
    this.subsystems.addAll(subsystems);
    this.verification = verification;
    this.compliance = compliance;
    this.target = target.trim();
    this.body = body;
    this.note = note;
  }

  public Requirement(String ref,
      String version,
      String type,
      String summary,
      List<Verification> verification,
      Compliance compliance,
      String target,
      String body) {
    this(ref, version, type, summary,
        Collections.emptyList(), Collections.emptyList(),
        verification, compliance, target, body, "");
  }

  public String getRef() {
    return ref;
  }

  public String getVersion() {
    return version;
  }

  public String getType() {
    return type;
  }

  public String getSummary() {
    return summary;
  }

  public List<String> getSatisfies() {
    return satisfies;
  }

  public List<String> getSubsystems() {
    return subsystems;
  }

  public List<Verification> getVerification() {
    return verification;
  }

  public Compliance getCompliance() {
    return compliance;
  }

  public String getTarget() {
    return target;
  }

  public String getBody() {
    return body;
  }

  public String getNote() {
    return note;
  }

  @Override
  public String toString() {
    return "Requirement{" +
        "ref='" + ref + '\'' +
        ", version='" + version + '\'' +
        ", type='" + type + '\'' +
        ", summary='" + summary + '\'' +
        ", satisfies=" + satisfies +
        ", subsystems=" + subsystems +
        ", verification=" + verification +
        ", compliance=" + compliance +
        ", target='" + target + '\'' +
        ", body='" + body + '\'' +
        ", note='" + note + '\'' +
        '}';
  }

}
