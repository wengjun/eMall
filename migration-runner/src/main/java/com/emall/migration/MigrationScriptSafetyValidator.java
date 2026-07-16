package com.emall.migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

final class MigrationScriptSafetyValidator {
    private static final Pattern DESTRUCTIVE_DDL = Pattern.compile(
            "(?is)\\b(?:DROP\\s+(?:DATABASE|SCHEMA|TABLE|COLUMN|INDEX)|TRUNCATE\\s+TABLE|RENAME\\s+(?:TABLE|COLUMN))\\b"
                    + "|\\bALTER\\s+TABLE\\b[^;]*\\bDROP\\b");

    private final PathMatchingResourcePatternResolver resources = new PathMatchingResourcePatternResolver();

    void validate(MigrationTarget target) {
        List<String> destructiveScripts = scripts(target.locations()).stream()
                .filter(script -> DESTRUCTIVE_DDL.matcher(script.contents()).find()).map(Script::name).toList();
        if (destructiveScripts.isEmpty()) {
            return;
        }
        if (target.phase() == MigrationPhase.EXPAND) {
            throw new IllegalStateException("expand migration contains destructive DDL: " + destructiveScripts);
        }
        if (!target.allowDestructiveChanges() || !StringUtils.hasText(target.minimumCompatibleVersion())
                || !StringUtils.hasText(target.approvalReference())) {
            throw new IllegalStateException("contract migration requires destructive-change approval, "
                    + "minimum compatible version, and approval reference");
        }
    }

    private List<Script> scripts(List<String> locations) {
        List<Script> result = new ArrayList<>();
        for (String location : locations) {
            if (location.toLowerCase(Locale.ROOT).startsWith("filesystem:")) {
                addFilesystemScripts(result, Path.of(location.substring("filesystem:".length())));
            } else if (location.toLowerCase(Locale.ROOT).startsWith("classpath:")) {
                addClasspathScripts(result, location.substring("classpath:".length()));
            }
        }
        return result;
    }

    private void addFilesystemScripts(List<Script> target, Path location) {
        if (!Files.isDirectory(location)) {
            return;
        }
        try (var files = Files.walk(location)) {
            for (Path script : files.filter(Files::isRegularFile).filter(this::isSql).sorted().toList()) {
                target.add(new Script(script.toString(), Files.readString(script, StandardCharsets.UTF_8)));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to inspect migration scripts in " + location, exception);
        }
    }

    private void addClasspathScripts(List<Script> target, String location) {
        String normalized = location.startsWith("/") ? location.substring(1) : location;
        try {
            for (Resource resource : resources.getResources("classpath*:" + normalized + "/**/*.sql")) {
                target.add(new Script(resource.getDescription(), resource.getContentAsString(StandardCharsets.UTF_8)));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to inspect classpath migration scripts in " + location, exception);
        }
    }

    private boolean isSql(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sql");
    }

    private record Script(String name, String contents) {
    }
}
