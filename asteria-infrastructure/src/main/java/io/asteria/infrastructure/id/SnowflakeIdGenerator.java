package io.asteria.infrastructure.id;

import io.asteria.common.application.port.DistributedIdGenerator;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator implements DistributedIdGenerator {

    private static final long EPOCH = 1_704_067_200_000L;
    private static final long SEQUENCE_MASK = 4_095L;
    private static final int WORKER_ID_SHIFT = 12;
    private static final int DATACENTER_ID_SHIFT = 17;
    private static final int TIMESTAMP_SHIFT = 22;

    private final long workerId;
    private final long datacenterId;

    private long lastTimestamp = -1L;
    private long sequence;

    public SnowflakeIdGenerator(IdGeneratorProperties properties) {
        properties.validate();
        this.workerId = properties.getWorkerId();
        this.datacenterId = properties.getDatacenterId();
    }

    @Override
    public synchronized Long nextId() {
        long timestamp = currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "Clock moved backwards; refusing to generate an ID");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private long waitUntilNextMillis(long previousTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= previousTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }
}
