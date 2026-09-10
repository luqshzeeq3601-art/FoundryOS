# Contributing to FoundryOS

Thank you for your interest in contributing to FoundryOS! This document provides guidelines and instructions for contributing.

## Code of Conduct

Please maintain professional and respectful communication in all project interactions.

## Development Setup

### Prerequisites
- **Node.js**: v20+
- **JDK**: Java 21 (Eclipse Temurin recommended)
- **PostgreSQL**: 16+ (or use Docker Compose)
- **Docker & Docker Compose**: v2+

### Running with Docker Compose (Recommended)
```bash
cp .env.example .env
docker compose up --build
```
- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Local Development

#### Backend (Spring Boot 3.3 / Java 21)
```bash
cd backend
./mvnw clean test
./mvnw spring-boot:run
```

#### Frontend (React 18 / TypeScript / Vite)
```bash
cd frontend
npm install
npm run typecheck
npm run lint
npm run dev
```

## Branching & Commit Conventions

- Create feature branches off `main` or `develop`: `feat/short-description` or `fix/issue-description`
- Use [Conventional Commits](https://www.conventionalcommits.org/):
  - `feat:` New feature
  - `fix:` Bug fix
  - `docs:` Documentation changes
  - `style:` Formatting / UI refinement
  - `refactor:` Code restructuring
  - `test:` Adding or modifying tests
  - `chore:` Maintenance, build, dependencies

## Pull Request Process

1. Ensure all tests pass (`./mvnw test` in backend, `npm run typecheck` & `npm test` in frontend).
2. Ensure no hardcoded secrets or environment credentials are submitted.
3. Open a Pull Request against `main` with a clear description of the changes and motivation.
4. Verify that the GitHub Actions CI workflow passes.

## License

By contributing to FoundryOS, you agree that your contributions will be licensed under the [MIT License](LICENSE).
