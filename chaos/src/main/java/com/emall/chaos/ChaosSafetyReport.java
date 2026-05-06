package com.emall.chaos;

import java.util.List;

public record ChaosSafetyReport(
        boolean approved,
        List<String> violations
) {
}
