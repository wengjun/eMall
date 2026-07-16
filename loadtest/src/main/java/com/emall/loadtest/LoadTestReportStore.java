package com.emall.loadtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

final class LoadTestReportStore {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path directory;

    LoadTestReportStore(Path directory) {
        this.directory = directory;
    }

    Path writeWorker(WorkerReport report) {
        return writeJson(report.runId() + ".worker-" + report.workerIndex() + ".json", report);
    }

    void writeCapacity(CapacityReport report) {
        writeJson(report.runId() + ".capacity.json", report);
        writeText(report.runId() + ".capacity.md", CapacityReportMarkdown.render(report));
    }

    void writeEvidence(CapacityEvidence evidence) {
        writeJson("capacity-evidence.json", evidence);
        writeText("capacity-evidence.md", CapacityReportMarkdown.render(evidence));
    }

    List<WorkerReport> readWorkers() {
        ensureDirectory();
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("[a-z0-9._-]+\\.worker-[0-9]+\\.json"))
                    .sorted().map(this::readWorker).toList();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to list load-test worker reports", ex);
        }
    }

    private WorkerReport readWorker(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), WorkerReport.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read worker report " + path, ex);
        }
    }

    private Path writeJson(String fileName, Object value) {
        ensureDirectory();
        Path target = directory.resolve(fileName);
        Path temporary = directory.resolve(fileName + ".tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), value);
            moveAtomically(temporary, target);
            return target;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to write load-test report " + target, ex);
        }
    }

    private void writeText(String fileName, String value) {
        ensureDirectory();
        Path target = directory.resolve(fileName);
        Path temporary = directory.resolve(fileName + ".tmp");
        try {
            Files.writeString(temporary, value, StandardCharsets.UTF_8);
            moveAtomically(temporary, target);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to write load-test report " + target, ex);
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to create load-test report directory", ex);
        }
    }
}
