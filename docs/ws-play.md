# Play WebSocket (smoke test)

- Endpoint: `ws://localhost:8080/ws/play/{gameId}` (use `wss://` when on HTTPS)
- Server sends a `session` message immediately after connect.

You must create a game first (via `/game` → “+ New Game”) so the `gameId` exists.

## Quick manual test (websocat)

1. Start the app: `./mvnw quarkus:dev`
2. Create a game in the browser: `http://localhost:8080/game`
3. Connect (replace `{gameId}`): `websocat ws://localhost:8080/ws/play/{gameId}`
4. Request history:
   - Send: `{"type":"history_request","limit":100}`
   - Expect: `{"type":"history","messages":[]}`
5. Send a message:
   - Send: `{"type":"user_message","text":"hello"}`
   - Expect (order):
     - `user_echo`
     - `assistant_start`
     - one or more `assistant_delta`
     - `assistant_done`
