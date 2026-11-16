# COC-LIO Supercell API Worker

This Worker provides a small, read-only gateway from COC-LIO to the official Clash of Clans API.

## Architecture

- Android app obtains a Firebase Authentication ID token from the signed-in COC-LIO user.
- Worker verifies the Firebase token against Google's Secure Token signing keys.
- Worker accepts only GET requests for player profiles, clan profiles, clan members, and public clan war logs.
- Worker forwards the request through RoyaleAPI's documented Clash of Clans proxy.
- The Supercell API token is stored only as a Cloudflare Worker secret.

## Cloudflare configuration

Worker name: `coc-lio-supercell-api`

Plain-text binding:
- `FIREBASE_PROJECT_ID` = `coc-lio-73de9`

Required secret:
- `SUPERCELL_API_TOKEN` = the API key created in the Supercell Developer Portal

Do not put the Supercell API key in this repository, Firebase Remote Config, Android source, APK, or a chat message.

## Supercell API key

1. Create a key at https://developer.clashofclans.com/.
2. In the allowed IP addresses, add `45.79.218.79` as documented by RoyaleAPI.
3. In Cloudflare Dashboard, open Workers & Pages > `coc-lio-supercell-api` > Settings > Variables and Secrets.
4. Add `SUPERCELL_API_TOKEN` as a Secret, then redeploy the Worker.

RoyaleAPI's documented Clash of Clans proxy is `https://cocproxy.royaleapi.dev`. It is a third-party proxy, not a Supercell-operated service.

## Supported routes

- `GET /health` — service configuration status.
- `GET /v1/players/{playerTag}` — official player profile.
- `GET /v1/clans/{clanTag}` — official clan profile.
- `GET /v1/clans/{clanTag}/members` — clan member list.
- `GET /v1/clans/{clanTag}/warlog` — public clan war log.

All data routes require a valid Firebase Authentication ID token in the Authorization Bearer header. The Worker is intentionally read-only and does not expose arbitrary upstream URLs.

## Free tier

Cloudflare Workers Free currently includes up to 100,000 requests per day, subject to Cloudflare's published plan limits. Supercell API usage is also subject to Supercell's own quotas and policies.
