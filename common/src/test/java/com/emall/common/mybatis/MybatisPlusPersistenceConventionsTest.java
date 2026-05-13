package com.emall.common.mybatis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MybatisPlusPersistenceConventionsTest {
    private static final Path PROJECT_ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void mappersAndRepositoriesShouldNotUseWeakRowMaps() throws IOException {
        List<Path> violations = javaFiles().filter(MybatisPlusPersistenceConventionsTest::isMapperOrRepository)
                .filter(MybatisPlusPersistenceConventionsTest::usesWeakRowMaps)
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void mappersShouldUseExplicitColumnsInsteadOfSelectStar() throws IOException {
        List<Path> violations = javaFiles()
                .filter(path -> path.getFileName().toString().endsWith("Mapper.java"))
                .filter(path -> read(path).contains("SELECT *"))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void timestampEntitiesShouldDeclareMybatisPlusFillStrategy() throws IOException {
        List<Path> violations = javaFiles()
                .filter(path -> path.getFileName().toString().endsWith("Entity.java"))
                .filter(MybatisPlusPersistenceConventionsTest::missingFillStrategy)
                .toList();

        assertThat(violations).isEmpty();
    }

    private static Stream<Path> javaFiles() throws IOException {
        return Files.walk(PROJECT_ROOT)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.toString().contains("\\target\\"))
                .filter(path -> path.toString().contains("\\src\\main\\java\\"));
    }

    private static boolean isMapperOrRepository(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith("Mapper.java")
                || fileName.startsWith("MybatisPlus") && fileName.endsWith("Repository.java");
    }

    private static boolean usesWeakRowMaps(Path path) {
        String source = read(path);
        return source.contains("RowMaps") || source.contains("Map<String, Object>");
    }

    private static boolean missingFillStrategy(Path path) {
        String source = read(path);
        return source.contains("private LocalDateTime createdAt;")
                && !source.contains("@TableField(value = \"created_at\", fill = FieldFill.INSERT)")
                || source.contains("private LocalDateTime updatedAt;")
                        && !source.contains("@TableField(value = \"updated_at\", fill = FieldFill.INSERT_UPDATE)");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + path, ex);
        }
    }
}
