---
name: Infrastructure Manager
description: Manages the Docker-based infrastructure for the project, including starting, stopping, and restarting services with health checks.
---

# Infrastructure Manager

This skill helps you manage the Docker environment for the Spring Coroutines Playground project. It ensures that all services (PostgreSQL, MVC App, WebFlux Apps) are running and ready before you proceed with tests.

## Available Commands

### Start Infrastructure
Starts all containers and waits for them to be healthy.

```bash
./scripts/manage_infrastructure.sh start
```

### Stop Infrastructure
Stops all containers and removes volumes to ensure a clean state.

```bash
./scripts/manage_infrastructure.sh stop
```

### Restart Infrastructure
Performs a full restart (Stop + Start) to reset the environment.

```bash
./scripts/manage_infrastructure.sh restart
```

## Requirements
- Docker and Docker Compose must be installed and running.
- `nc` (netcat) must be available for port checking (usually available in Git Bash or standard Linux distros).
