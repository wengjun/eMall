package com.emall.loadtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

final class IdentityFixtureSource implements AutoCloseable {
    private final LoadTestOptions options;
    private final TrafficModel trafficModel;
    private final BufferedReader reader;

    IdentityFixtureSource(LoadTestOptions options, TrafficModel trafficModel) {
        this.options = options;
        this.trafficModel = trafficModel;
        if (options.identityFixtureFile() == null) {
            this.reader = null;
            return;
        }
        try {
            this.reader = Files.newBufferedReader(options.identityFixtureFile(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to open identity fixture " + options.identityFixtureFile(), ex);
        }
    }

    synchronized Credential next(long globalSequence) {
        if (reader == null) {
            return new Credential(trafficModel.userId(globalSequence), options.authToken());
        }
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.equalsIgnoreCase("userId,token")) {
                    continue;
                }
                String[] fields = trimmed.split(",", 2);
                if (fields.length != 2) {
                    throw new IllegalArgumentException("identity fixture lines must use userId,token format");
                }
                long userId = Long.parseLong(fields[0].trim());
                String token = fields[1].trim();
                if (userId <= 0L || token.isBlank()) {
                    throw new IllegalArgumentException("identity fixture contains an invalid user or token");
                }
                return new Credential(userId, token);
            }
            throw new IllegalStateException("identity fixture was exhausted before the load test completed");
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read identity fixture", ex);
        }
    }

    @Override
    public void close() {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to close identity fixture", ex);
        }
    }

    record Credential(long userId, String token) {
    }
}
