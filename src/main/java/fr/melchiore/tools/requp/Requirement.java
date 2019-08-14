package fr.melchiore.tools.requp;

import java.util.*;

public class Requirement {
    private String ref;
    private String version;
    private String type;
    private String summary;
    private List<String> satisfies;
    private List<String> subsystems;
    private Verification verification;
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
                       Verification verification,
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
                       Verification verification,
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

    public Verification getVerification() {
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

    public enum Verification {
        UNSET("TBD"),
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

        public final String abrv;

        Verification(String abrv) {
            this.abrv = abrv;
        }

        public static Verification fromAbrv(String abrv) {
            return abrvToEnum.getOrDefault(abrv.trim(), Verification.UNSET);
        }
    }

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

        public final String abrv;

        Compliance(String abrv) {
            this.abrv = abrv;
        }

        public static Compliance fromAbrv(String abrv) {
            return abrvToEnum.getOrDefault(abrv.trim(), Compliance.UNSET);
        }
    }
}
