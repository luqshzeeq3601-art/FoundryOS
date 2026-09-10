# Contributing

## Setup

See the [README](README.md#local-development) for local dev instructions.

Quick version:

```bash
cp .env.example .env
docker compose up --build
```

Or run backend and frontend independently — see README for details.

## Workflow

1. Create a branch off `main`: `feat/description` or `fix/description`
2. Use [conventional commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`)
3. Make sure CI passes before opening a PR:
   - `cd backend && ./mvnw test`
   - `cd frontend && npm run typecheck`
4. Open a PR against `main` with a clear description of what changed and why
5. No hardcoded secrets or credentials in commits

## License

Contributions are licensed under [MIT](LICENSE).
