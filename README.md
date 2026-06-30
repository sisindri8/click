# Click — AI-powered product discovery from trusted YouTube channels

Current status: **2 of 6 services built** (Query Expansion, YouTube Search).
This README covers running just these two locally.

---

## Prerequisites (install these first)

1. **JDK 21** — https://adoptium.net/temurin/releases/?version=21
   Verify: `java -version` → should show `21.x.x`

2. **Maven** — https://maven.apache.org/download.cgi
   Verify: `mvn -version`

3. **IntelliJ IDEA Community** (free) — https://www.jetbrains.com/idea/download/

4. **Postman** — https://www.postman.com/downloads/ (for testing endpoints)

---

## Step 1 — Get your API keys ready

You said you already have:
- **Claude API key** — from https://console.anthropic.com/settings/keys
- **YouTube Data API v3 key** — from Google Cloud Console (enable "YouTube Data API v3" on a project, then create an API key under Credentials)

Keep both handy — you'll set them as environment variables, never hardcoded in code.

---

## Step 2 — Set environment variables

### On Windows (PowerShell):
```powershell
$env:CLAUDE_API_KEY="your-claude-key-here"
$env:YOUTUBE_API_KEY="your-youtube-key-here"
```

### On Mac/Linux (Terminal):
```bash
export CLAUDE_API_KEY="your-claude-key-here"
export YOUTUBE_API_KEY="your-youtube-key-here"
```

These only last for the current terminal session. To persist them, add the
export lines to your `~/.zshrc` / `~/.bashrc` (Mac/Linux) or set them as
permanent System Environment Variables (Windows).

**IntelliJ tip:** if you run the app via IntelliJ's Run button instead of
terminal, set these in Run Configuration → Environment Variables instead —
terminal exports won't be visible to IntelliJ's own process otherwise.

---

## Step 3 — IMPORTANT: Update YouTube channel IDs

Open `youtube-search-service/src/main/resources/application.yml`.
The `trusted-channels` list currently has placeholder IDs like
`UC_TECHBURNER_PLACEHOLDER`. Replace these with real YouTube channel IDs.

**How to find a channel ID:**
1. Go to the channel's YouTube page
2. View Page Source (Ctrl+U / Cmd+U)
3. Search for `"channelId"` — copy the value starting with `UC...`

Or simpler: use a free tool like https://commentpicker.com/youtube-channel-id.php

---

## Step 4 — Run Query Expansion Service

```bash
cd query-expansion-service
mvn spring-boot:run
```

Wait for: `Started QueryExpansionServiceApplication in X seconds`
Runs on: **http://localhost:8081**

### Test it (Postman or curl):
```bash
curl -X POST http://localhost:8081/api/v1/query-expansion/expand \
  -H "Content-Type: application/json" \
  -d '{"query": "best phones under 30k"}'
```

Expected response:
```json
{
  "originalQuery": "best phones under 30k",
  "expandedQueries": [
    "best smartphone under 30000 2024",
    "top mobile phones 30k budget India",
    ...
  ],
  "detectedCategory": "mobiles"
}
```

---

## Step 5 — Run YouTube Search Service

This service needs Redis running (for caching). For now you can run it
without Redis connected — it'll just fail to start if Redis isn't there
once we wire in actual caching. For this first run, that's expected —
we haven't added the caching code yet, only the config.

```bash
cd youtube-search-service
mvn spring-boot:run
```

Runs on: **http://localhost:8082**

### Test it:
```bash
curl -X POST http://localhost:8082/api/v1/youtube-search/search \
  -H "Content-Type: application/json" \
  -d '{"queries": ["best smartphone under 30000 2024", "top mobile phones 30k"]}'
```

---

## Step 6 — Run API Gateway (orchestrates everything)

```bash
cd api-gateway
mvn spring-boot:run
```
Runs on: **http://localhost:8080**

Test the full pipeline with ONE call:
```bash
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{"query": "best phones under 30k"}'
```

This single call chains: Query Expansion -> YouTube Search -> Transcript
Service (top 2 ranked videos only - see below).

---

## Step 7 — Run Transcript Service

Transcript Service has its own mock mode, same pattern as Query Expansion.

### Quick test (no setup needed)
```bash
cd transcript-service
mvn spring-boot:run
```
Runs on: **http://localhost:8083**, `transcript.mock-mode: true` by default
- returns fake transcript text instantly, no Python/Whisper setup required.

### Real transcription (Whisper) setup
This is the heaviest setup in the project. Only do this once the rest of
the pipeline is confirmed working in mock mode.

1. **Install ffmpeg** (required by Whisper, not a pip package):
   - Windows: https://www.gyan.dev/ffmpeg/builds/ (add the `bin/` folder to PATH)
   - Mac: `brew install ffmpeg`
   - Linux: `sudo apt install ffmpeg`
   - Verify: `ffmpeg -version`

2. **Install Python dependencies:**
   ```bash
   cd transcript-service/python
   pip install -r requirements.txt --break-system-packages
   ```
   This downloads `openai-whisper` and `yt-dlp`. The Whisper model itself
   (~150MB for "base") downloads automatically on first real transcription.

3. **Test the Python script standalone first** (isolates Python problems
   from Java problems):
   ```bash
   cd transcript-service/python
   python3 fetch_transcript.py dQw4w9WgXcQ
   ```
   Expect this to take 10-90 seconds. Output should be one line of JSON.

4. **Flip mock mode off** in `transcript-service/src/main/resources/application.yml`:
   ```yaml
   transcript:
     mock-mode: false
   ```

5. Re-run `mvn spring-boot:run` and test via the gateway again.

### Important honest note on transcription approach
This service is **Whisper-only by design** - it does NOT scrape YouTube
captions. YouTube actively blocks caption-scraping requests from
cloud/datacenter IPs (confirmed during this project's development), which
would make a captions-first approach unreliable on any real deployment.
Whisper is slower per video (10-90s vs ~1-2s for captions) but doesn't
depend on YouTube not blocking the request, and has solid Telugu/Hindi
accuracy - relevant since the configured trusted channels post in Telugu.

---

## Updated Troubleshooting

| Problem | Likely cause |
|---|---|
| `CLAUDE_API_KEY` placeholder error on startup | Env var not set in the terminal/IDE you're running from |
| 401 Unauthorized from Claude | Bad or expired API key |
| 403 Forbidden from YouTube | API key restrictions in Google Cloud Console, or quota exceeded |
| Empty video results | Placeholder channel IDs not replaced with real ones |
| Port already in use | Another process on 8080/8081/8082/8083 - change `server.port` in application.yml |
| Gateway returns "Could not reach X Service" | That downstream service isn't running - start it first |
| Transcript fetch times out | Whisper genuinely takes 10-90s/video on CPU - this can be normal, not a bug |
| `python3: command not found` | Try `python` instead, or set `PYTHON_EXECUTABLE` env var |
| Whisper/yt-dlp import errors | Run `pip install -r requirements.txt --break-system-packages` in `transcript-service/python/` |

---

## What's NOT built yet (by design — next phase)
- Summarization Service (Claude extracts products per video)
- Curation Service (groups into 5 categories)
- Redis/PostgreSQL actual wiring
- Docker Compose
- React frontend
