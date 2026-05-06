package com.emall.chaos;

public class ChaosDrillCatalogApplication {
    public static void main(String[] args) {
        ChaosSafetyGate safetyGate = new ChaosSafetyGate();
        for (ChaosDrill drill : ChaosDrillCatalog.p3Baseline()) {
            ChaosSafetyReport report = safetyGate.validate(drill);
            String status = report.approved() ? "APPROVED" : "REJECTED";
            System.out.println(status + " " + drill.code() + " " + drill.type() + " " + drill.duration());
        }
    }
}
