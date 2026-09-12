package io.asteria.payment.infrastructure.config;

import io.asteria.payment.infrastructure.client.LedgerClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = LedgerClient.class)
public class PaymentFeignConfiguration {
}
