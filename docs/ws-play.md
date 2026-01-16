# Play WebSocket (smoke test)

- Endpoint: `ws://localhost:8080/ws/play` (use `wss://` when on HTTPS)
- Server sends a `session` message immediately after connect.

## Quick manual test (websocat)

1. Start the app: `./mvnw quarkus:dev`
2. Connect: `websocat ws://localhost:8080/ws/play`
3. Request history:
   - Send: `{"type":"history_request","limit":100}`
   - Expect: `{"type":"history","messages":[]}`
4. Send a message:
   - Send: `{"type":"user_message","text":"hello"}`
   - Expect (order):
     - `user_echo`
     - `assistant_start`
     - one or more `assistant_delta`
     - `assistant_done`
