package io.asteria.payment.infrastructure.config;

import io.asteria.payment.infrastructure.client.LedgerClient;
import io.asteria.payment.infrastructure.client.BalanceClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {LedgerClient.class, BalanceClient.class})
public class PaymentFeignConfiguration {
}
