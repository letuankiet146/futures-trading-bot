# Archived: question9 — backtest REST trigger + kline cache

**Status:** All answers are merged into **`implementation-spec-v1.md`** (primarily **§3.2** Backtest API, plus **§8.2**, **§9.1**). This file is kept for history and rationale.

---

# Open questions (backtest REST trigger + kline cache)

This file captures remaining decisions for the new backtest API workflow:

- trigger by `POST /api/v1/backtest/run` (async job + `GET /api/v1/backtest/jobs/{jobId}` for status)
- JSON body: `startDate` (optional unless `endDate` is set), `endDate` (optional). If **both** are omitted → replay **latest 1500** fully closed klines; if only `startDate` → end defaults to latest **fully closed** kline.
- load klines from DB cache first
- fetch missing klines from Binance REST in `limit=1500` pages
- persist for reuse to avoid API spam

---

## 1. Date format and timezone

- Required wire format for `startDate` / `endDate`:
  - ISO-8601 datetime (`2026-04-01T00:00:00Z`)?
  - epoch milliseconds?
  - date-only (`yyyy-MM-dd`)?
- Timezone basis for date-only input (UTC vs local)?

Answer: `startDate` and `endDate` accept **ISO-8601 datetime with explicit offset** (e.g. `2026-04-01T00:00:00Z`), **epoch milliseconds** (integer string, UTC instant), and optionally **date-only** `yyyy-MM-dd`. If the client sends date-only, interpret it as **start of that calendar day in the JVM default timezone of `strategy-service`** (host / `user.timezone` “local”); document `TZ` or `-Duser.timezone` in ops so behavior is predictable.

---

## 2. Range boundary semantics

- Are bounds inclusive on both sides (`[startDate, endDate]`)?
- Should `endDate` be clamped to the latest closed candle time for the configured interval?

Answer: `startDate` is **optional** if `endDate` is also omitted (**both omitted** → backtest uses the **1500 most recent fully closed** klines). If `endDate` is provided, `startDate` is **required**. If only `startDate` is provided, `endDate` defaults to the **latest fully closed kline** (not the in-progress bar). When both dates are provided, keep normal validation (`startDate` not after effective end, with `endDate` clamped to latest closed when needed).

---

## 3. HTTP method, side effects, and response style

The endpoint triggers data fetch + DB writes + replay run.

- Keep `GET` (as requested) even though it has side effects, or move to `POST` later?
- Should response be synchronous (wait until full run completes) or async (`202 Accepted` + `jobId`)?

Answer: Use **`POST /api/v1/backtest/run`** (side effects are not safe/idempotent as `GET`). Run is **asynchronous**: respond **`202 Accepted`** with a **`jobId`** (and optionally `Location` / `statusPath` pointing at the job resource). Clients poll **`GET /api/v1/backtest/jobs/{jobId}`** for `status` (`PENDING` → `RUNNING` → `SUCCEEDED` | `FAILED`), timestamps, effective date range, and on failure an error message; on success, a compact **result summary** (v1: e.g. candles replayed, correlation id if useful). Persist job rows in **`strategy`** schema so status survives restarts.

---

## 4. Concurrency and idempotency

- If the same `(symbol, interval, startDate, endDate)` is triggered concurrently, should we:
  - reject second request (`409`),
  - queue it,
  - or deduplicate and attach to current run?
- Is only one backtest run allowed globally per strategy instance?

Answer: **Deduplicate:** if a job for the same **configured symbol**, **configured interval**, and the **same request** `startDate` / `endDate` strings (both omitting `endDate` counts as one identity) is already **`PENDING`** or **`RUNNING`**, new `POST` returns **`202`** with the **same `jobId`** as the in-flight run—no duplicate worker. **Parallel runs** are allowed when any of those differ (different range, interval, or symbol config).

---

## 5. DB cache refresh policy

- If candles already exist in DB, do we always trust them?
- Should we support optional refresh for the most recent window (in case of exchange corrections)?

Answer: **Trust stored closed klines:** each cached row is a **closed** candle, treated as **immutable** in v1—**no** verify/replace/overwrite pass against the exchange for rows already persisted. **No** optional refresh for a “recent window” (no correction-replay hook in v1). REST is only used to **fill missing** intervals, then upsert new rows.

---

## 6. API protection

- Is this endpoint internal-only (private network) or needs authentication token?

Answer: **v1:** backtest `POST` / job `GET` **require no authentication** (defer tokens/API keys for these routes to a later version). Operators should still rely on **network exposure control** (e.g. private network / firewall) until auth lands.

---

## 7. Maximum allowed range

- Set max range per request (e.g., max days or max candles) to avoid extreme runs and API load spikes?

Answer: **v1:** **no** hard limit on requested range (no max days / max candles enforcement). Revisit caps in a later version if needed.
