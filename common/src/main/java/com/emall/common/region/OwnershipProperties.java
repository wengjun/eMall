package com.emall.common.region;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties("emall.ownership")
@Data
public class OwnershipProperties {
    private boolean enabled;
    private String currentRegion = "default-region";
    private String currentCell = "default-cell";
    private Map<String, RegionWriteStatus> regionStatuses = new LinkedHashMap<>();
    private Map<String, DomainOwnership> domains = new LinkedHashMap<>();

    public DomainOwnership domain(String domain) {
        DomainOwnership ownership = domains.get(domain.toLowerCase(Locale.ROOT));
        if (ownership != null) {
            return ownership;
        }
        DomainOwnership fallback = new DomainOwnership();
        fallback.setPrimaryRegion(currentRegion);
        fallback.setOwnerRegions(List.of(currentRegion));
        fallback.setOwnerCells(List.of(currentCell));
        return fallback;
    }

    @Data
    public static class DomainOwnership {
        private WriteOwnershipStrategy strategy = WriteOwnershipStrategy.GLOBAL_SINGLE_WRITER;
        private String primaryRegion = "default-region";
        private List<String> ownerRegions = List.of("default-region");
        private List<String> ownerCells = List.of("default-cell");
    }
}
