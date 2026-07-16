package com.emall.migration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationScriptSafetyValidatorTest {
    @TempDir
    Path migrationDirectory;

    private final MigrationScriptSafetyValidator validator = new MigrationScriptSafetyValidator();

    @Test
    void allowsBackwardCompatibleExpandMigration() throws IOException {
        Files.writeString(migrationDirectory.resolve("V1__expand.sql"),
                "ALTER TABLE customer ADD COLUMN nickname VARCHAR(64) NULL;");

        assertThatCode(() -> validator.validate(target(MigrationPhase.EXPAND, false, "", "")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDestructiveDdlDuringExpandPhase() throws IOException {
        Files.writeString(migrationDirectory.resolve("V2__drop.sql"), "ALTER TABLE customer DROP COLUMN nickname;");

        assertThatThrownBy(() -> validator.validate(target(MigrationPhase.EXPAND, false, "", "")))
                .hasMessageContaining("expand migration contains destructive DDL");
    }

    @Test
    void requiresCompatibilityAndApprovalGatesForContractPhase() throws IOException {
        Files.writeString(migrationDirectory.resolve("V3__contract.sql"), "DROP TABLE legacy_customer;");

        assertThatThrownBy(() -> validator.validate(target(MigrationPhase.CONTRACT, true, "", "CHG-123")))
                .hasMessageContaining("minimum compatible version");
        assertThatCode(() -> validator.validate(target(MigrationPhase.CONTRACT, true, "2.4.0", "CHG-123")))
                .doesNotThrowAnyException();
    }

    private MigrationTarget target(MigrationPhase phase, boolean allowDestructive, String minimumVersion,
            String approval) {
        return new MigrationTarget("user", "default", 0, "emall_user", "jdbc:mysql://mysql:3306/emall_user",
                "user_migration", "secret", List.of("filesystem:" + migrationDirectory), "flyway_schema_history",
                "test", "batch-1", false, false, false, List.of(), phase, allowDestructive, minimumVersion, approval);
    }
}
