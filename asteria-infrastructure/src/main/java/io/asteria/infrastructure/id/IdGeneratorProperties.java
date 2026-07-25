package io.asteria.infrastructure.id;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "asteria.id-generator")
public class IdGeneratorProperties {

    private Long workerId;

    private Long datacenterId;

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public Long getDatacenterId() {
        return datacenterId;
    }

    public void setDatacenterId(Long datacenterId) {
        this.datacenterId = datacenterId;
    }

    public void validate() {
        if (workerId == null || datacenterId == null
                || workerId < 0 || workerId > 31
                || datacenterId < 0 || datacenterId > 31) {
            throw new IllegalArgumentException(
                    "workerId and datacenterId must be between 0 and 31");
        }
    }
}
