package com.emall.migration;

import java.time.Duration;
import java.util.List;
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
    private final MigrationBatchExecutor executor;

    public MigrationRunnerApplication(MigrationRunnerProperties properties, MigrationBatchExecutor executor) {
        this.properties = properties;
        this.executor = executor;
    }

    public static void main(String[] args) {
        SpringApplication.run(MigrationRunnerApplication.class, args);
    }

    @Override
    public void run(String... args) throws InterruptedException {
        List<List<MigrationTarget>> batches = properties.planBatches();
        log.info("Starting service-scoped migration service={} batches={} targets={} phase={} dryRun={} operator={}",
                properties.getService(), batches.size(), batches.stream().mapToInt(List::size).sum(),
                properties.getPhase(), properties.isDryRun(), properties.getOperator());
        for (int index = 0; index < batches.size(); index++) {
            List<MigrationTarget> batch = batches.get(index);
            log.info("Starting migration batch service={} batch={}/{} shards={}", properties.getService(), index + 1,
                    batches.size(), batch.stream().map(MigrationTarget::shard).toList());
            executor.execute(batch, properties.getMaxParallelism(), properties.getBatchTimeout());
            log.info("Completed migration batch service={} batch={}/{}", properties.getService(), index + 1,
                    batches.size());
            pauseBeforeNextBatch(index, batches.size(), properties.getPauseBetweenBatches());
        }
    }

    private void pauseBeforeNextBatch(int completedIndex, int batchCount, Duration pause) throws InterruptedException {
        if (completedIndex + 1 >= batchCount || pause.isZero()) {
            return;
        }
        log.info("Pausing service migration for observation service={} duration={}", properties.getService(), pause);
        Thread.sleep(pause.toMillis());
    }
}
