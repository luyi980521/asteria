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
- `asteria-infrastructure`: database, messaging, and external integration implementations

## Build

```bash
mvn clean verify
```

## Run locally

```bash
mvn -pl asteria-bootstrap -am spring-boot:run
```
