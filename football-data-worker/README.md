# ZORA FOOTBALL DATA — Cloudflare Worker

This folder is a Cloudflare Workers version of the ZORA FOOTBALL DATA API. It is separate from the Android app and from the original Express backend in ../football-data.

## Cloudflare setup

1. Import this GitHub repository in Cloudflare Workers & Pages.
2. Set the project root/build directory to `football-data-worker` if the dashboard offers that option.
3. Build command: `npm install` (or leave blank if dependencies are installed automatically).
4. Deploy command: `npx wrangler deploy`.
5. Add these Worker variables/secrets in Settings → Variables and Secrets:
   - `FOOTBALL_DATA_TOKEN` (optional fallback for supported competitions)
   - `APISPORTS_KEY` (needed for Liga 1 Indonesia)
   - `APISPORTS_INDONESIA_LEAGUE_ID` (default 274; verify with the provider)
   - `APISPORTS_SEASON` (set to the provider's season value)
   - `ADMIN_SYNC_TOKEN` (secret for the admin sync endpoint)

ESPN is attempted first for competitions other than Liga 1 Indonesia. football-data.org is fallback. API-Sports is used for Liga 1 Indonesia. If provider credentials are missing, affected requests return a 503 or partial results; the API does not invent match data.

## Endpoints

- GET /api/v1/health
- GET /api/v1/leagues
- GET /api/v1/matches/live
- GET /api/v1/matches?league=premier-league
- GET /api/v1/standings/premier-league
- POST /api/v1/admin/sync (requires x-zora-admin-token)

## Notes

- Cloudflare Cron Trigger runs the provider refresh task every five minutes. Cron does not persist a database; it verifies/warms provider fetches only.
- Responses use Cloudflare edge caching directives. Cache availability is edge-location dependent, not a durable database.
- Provider limits and coverage still apply. API-Sports credentials and a supported plan are needed for Liga 1 Indonesia.
- This Worker is not yet connected to the Android app. Add the deployed Worker URL to the Android app only after endpoint testing.
