# Config & Environments

## Environments

- `local`
    - Base URL: `http://localhost:8080`
    - Usage: local development only
- `staging`
    - Base URL: `https://staging.api.moodmatch.app`
    - Usage: pre-prod testing
- `prod`
    - Base URL: `https://api.moodmatch.app`
    - Usage: live mobile apps

## Environment variables

- `APP_ENV`
    - `local` | `staging` | `prod`
    - Default: `local` (see `application.conf`)

- `PORT`
    - HTTP port Ktor listens on.
    - Default: `8080` (overridden by env var on hosting platforms).

- `JWT_SECRET` (or `TOKEN_SECRET`)
    - Used to sign/validate auth tokens.
    - **Must not be committed to Git.**

- `DB_URL` (future)
    - Database connection string.

- `ALLOWED_ORIGINS`
    - Allowed origins for CORS when you add a web client.
## Transport Security (TLS)

- All external traffic MUST use:
    - `https://api.moodmatch.app` (HTTP API)
    - `wss://api.moodmatch.app/ws` (WebSocket)
- Ktor runs on HTTP internally (port 8080); TLS is terminated at the hosting layer.
- Mobile clients (Android / iOS) must NEVER talk to `http://` URLs in production.
- Future: add certificate pinning on Android/iOS for extra MITM protection.