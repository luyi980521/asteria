# Asteria

Asteria is a modular monolith for financial infrastructure.

## Stack

- Java 21
- Maven
- Spring Boot

## Architecture

Current architecture: modular monolith.

## Modules

- `asteria-bootstrap`: the only Spring Boot startup module; contains `AsteriaApplication`
- `asteria-common`: shared stable base types only
- `asteria-account`: account domain module
- `asteria-ledger`: double-entry ledger domain module
- `asteria-payment`: payment orchestration domain module
- `asteria-reconciliation`: [Reconciliation v1 domain models and PostgreSQL design](asteria-reconciliation/README.md)
- `asteria-infrastructure`: database, messaging, and external integration implementations

## Build

```bash
mvn clean verify
```

The real HTTP/Feign + PostgreSQL + Kafka Payment/Balance/Ledger/Settlement E2E is opt-in.
See [Core Payment Flow E2E](docs/core-payment-flow-e2e.md) for the command, prerequisites,
database evidence and the current Settlement accounting rule.

## Run locally

```bash
mvn -pl asteria-bootstrap -am spring-boot:run
```
