# AI Chat — scaffold

A Claude-style Android chat app: minimal UI, no login, everything configurable
tucked behind Settings. Two custom systems power it:

## 1. Multi-provider AI (`data/provider/`)

Instead of one class per vendor, there are **3 adapters** for the 3 request
shapes almost every LLM API uses:

- `OpenAICompatibleProvider.kt` — covers Groq, DeepSeek, Mistral, Together AI,
  OpenRouter, Cerebras, xAI/Grok, Ollama, OpenAI itself, and most others
- `GeminiProvider.kt` — Google's own shape
- `AnthropicProvider.kt` — Claude's own shape

`ProviderTemplates.kt` is the "add any key" catalog — pick a template
(prefilled base URL + format) or use "Custom" for any other OpenAI-compatible
endpoint, paste a key, done.

`ProviderRepository.kt` is where the self-healing logic lives:
- Rate limits / network errors / server errors → retried automatically with backoff
- Wrong model name → auto-corrected by fetching the provider's model list
- A provider that keeps failing → silently fails over to the next enabled provider
- Only an **invalid key** or **out of quota** ever gets surfaced to you — those
  are the two things no amount of retrying can fix

## 2. MCP connectors (`data/mcp/`)

`McpClient.kt` is a generic JSON-RPC client for any MCP server — GitHub isn't
special-cased in code, it's just `McpServerTemplates.github(token)`, a preset
pointing at GitHub's official remote MCP endpoint (`api.githubcopilot.com/mcp`)
using a personal access token as the bearer token. Add any other MCP server
the same way: name + URL + optional token.

**Currently stubbed** (parses the HTTP response shape but not the full
`tools/list` result body) — the next real step is finishing that parse so
tool schemas actually reach the active AI provider as callable functions.

## 3. Storage (`data/store/SecureStore.kt`)

No login, no cloud sync — provider configs and MCP tokens are stored in
`EncryptedSharedPreferences`, backed by the Android Keystore, entirely
on-device. Nothing leaves the phone except the direct calls to whichever
providers/MCP servers you configured.

## 4. UI (`ui/`)

`ChatScreen.kt` — message list + input bar, settings icon top-right, no other
chrome. `SettingsScreen.kt` — provider list and MCP server list with on/off
switches. The "add provider" / "add MCP server" flows are marked `TODO` in
`MainActivity.kt` — next logical step, since they're just a template picker +
text field + save.

## Building

```
chmod +x gradlew   # you'll need to run `gradle wrapper` once locally/in CI
                    # to generate gradlew + gradle-wrapper.jar, since those
                    # binary files aren't included in this scaffold
./gradlew assembleDebug
```

`.github/workflows/build-apk.yml` builds a debug APK on push to `main`, same
pattern as your other projects — but it needs the Gradle wrapper files
committed first (`gradle wrapper --gradle-version 8.7` from a machine with
Gradle installed, or let GitHub Actions itself generate it with a `setup-gradle`
step instead of `gradlew`).

## Not yet built (next steps, roughly in order)

1. Add/edit provider dialog (template picker → key input → save)
2. Add MCP server dialog
3. Finish MCP `tools/list` / `tools/call` parsing and wire tool results into
   the active provider's function-calling
4. Chat history persistence (Room entities are in the Gradle deps but no
   DAO/entity classes yet — currently chat is in-memory only, lost on restart)
5. Streaming responses instead of wait-for-full-reply
6. Per-conversation provider/model switching (right now it's global)
