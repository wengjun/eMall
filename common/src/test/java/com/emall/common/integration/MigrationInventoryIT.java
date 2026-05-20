package com.emall.common.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MigrationInventoryIT {
    private static final Path REPOSITORY_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final String MIGRATION_DIRECTORY = "src/main/resources/db/migration";
    private static final Pattern FLYWAY_SQL = Pattern.compile("V(?<version>\\d+)__[-_a-z0-9]+\\.sql");

    @Test
    void shouldKeepFlywayMigrationVersionsUniqueAndContiguousByModule() {
        List<MigrationModule> modules = migrationModules();

        assertThat(modules).isNotEmpty();
        for (MigrationModule module : modules) {
            assertThat(module.fileNames()).allMatch(fileName -> FLYWAY_SQL.matcher(fileName).matches(),
                    "Flyway SQL names must use V{number}__lower_snake_case.sql");
            assertThat(module.versions()).doesNotHaveDuplicates();
            assertThat(module.versions())
                    .containsExactlyElementsOf(IntStream.rangeClosed(1, module.versions().size()).boxed().toList());
        }
    }

    @Test
    void shouldProvideFlywayMigrationsForEveryJdbcStorageModule() {
        List<String> missingMigrationModules = jdbcStorageModules().stream()
                .filter(moduleName -> Files.notExists(REPOSITORY_ROOT.resolve(moduleName).resolve(MIGRATION_DIRECTORY)))
                .toList();

        assertThat(missingMigrationModules).isEmpty();
    }

    private List<MigrationModule> migrationModules() {
        try (Stream<Path> modules = Files.list(REPOSITORY_ROOT)) {
            return modules.filter(path -> Files.isDirectory(path.resolve(MIGRATION_DIRECTORY)))
                    .map(this::migrationModule).sorted(Comparator.comparing(MigrationModule::moduleName)).toList();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to discover Flyway migration modules", ex);
        }
    }

    private MigrationModule migrationModule(Path modulePath) {
        try (Stream<Path> files = Files.list(modulePath.resolve(MIGRATION_DIRECTORY))) {
            List<String> fileNames = files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                    .sorted(Comparator.comparingInt(this::migrationVersion)).toList();
            return new MigrationModule(modulePath.getFileName().toString(), fileNames);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to inspect Flyway migrations for " + modulePath, ex);
        }
    }

    private List<String> jdbcStorageModules() {
        try (Stream<Path> modules = Files.list(REPOSITORY_ROOT)) {
            return modules.filter(path -> Files.isRegularFile(path.resolve("src/main/resources/application.yml")))
                    .filter(this::usesJdbcStorage).map(path -> path.getFileName().toString()).sorted().toList();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to discover JDBC storage modules", ex);
        }
    }

    private boolean usesJdbcStorage(Path modulePath) {
        try {
            return Files.readString(modulePath.resolve("src/main/resources/application.yml")).contains("storage: jdbc");
        } catch (IOException ex) {
            throw new IllegalStateException("failed to inspect application.yml for " + modulePath, ex);
        }
    }

    private int migrationVersion(String fileName) {
        Matcher matcher = FLYWAY_SQL.matcher(fileName);
        return matcher.matches() ? Integer.parseInt(matcher.group("version")) : Integer.MAX_VALUE;
    }

    private record MigrationModule(String moduleName, List<String> fileNames) {
        private List<Integer> versions() {
            return fileNames.stream().map(MigrationInventoryIT::migrationVersionStatic).toList();
        }
    }

    private static int migrationVersionStatic(String fileName) {
        Matcher matcher = FLYWAY_SQL.matcher(fileName);
        return matcher.matches() ? Integer.parseInt(matcher.group("version")) : Integer.MAX_VALUE;
    }
}
