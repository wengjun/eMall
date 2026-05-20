package com.emall.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MigrationRunnerProperties.class)
public class MigrationRunnerApplication implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(MigrationRunnerApplication.class);

    private final MigrationRunnerProperties properties;
    private final MigrationTargetExecutor executor;

    public MigrationRunnerApplication(MigrationRunnerProperties properties, MigrationTargetExecutor executor) {
        this.properties = properties;
        this.executor = executor;
    }

    public static void main(String[] args) {
        SpringApplication.run(MigrationRunnerApplication.class, args);
    }

    @Override
    public void run(String... args) {
        var targets = properties.expandTargets();
        log.info("Starting schema migration targets={}, dryRun={}, operator={}", targets.size(), properties.isDryRun(),
                properties.getOperator());
        for (MigrationTarget target : targets) {
            executor.execute(target);
        }
    }
}
