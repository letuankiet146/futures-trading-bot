# `draft-idea.txt` vs canonical spec (archive)

**`draft-idea.txt` is outdated.** It was the original brainstorm and **must not** be used as the reference for requirements or implementation.

## Canonical specification

Use these files **in order** (answers inline):

1. **`question.md`** — base decisions (stack, Kafka, risk, strategy parameters, modes, DB, etc.)
2. **`question2.md`** — follow-up decisions (OHLC replay, sizing, liquidation, REST vs WS for candles, …)
3. **`question3.md`** — edge cases (counters, margin parity, `MIN_NOTIONAL`, …)
4. **`peak-trough-spec.md`** — **normative** peak/trough (swing) detection; referenced from `question.md` §4 and `question2.md` §4.
5. **`question5.md`** — defaults (leverage, interval, exchange info, paper balance, risk day, position cycle).
6. **`question6.md`** — §1–§2 answered (see **`question.md`**).

Implementation and design reviews should cite the Q-files and **`peak-trough-spec.md`**, not `draft-idea.txt`.

---

## What `draft-idea.txt` disagrees with (historical record only)

These are **not** open questions — they are **spots where the old file still says something different** from the locked answers above. **Ignore `draft-idea.txt`** for these topics; the Q-files win.

| Topic | Stale text in `draft-idea.txt` | Locked elsewhere |
|--------|-------------------------------|------------------|
| Peaks / troughs / “avg” drawdown | Many peaks/troughs, plural “averages” | **`peak-trough-spec.md`** + `question.md` §4 (aggregation / drawdown) |
| Binance order API | “Use WS API for send request” | `question.md` §3 — **REST** for orders |
| Latency | “each service under 1 second” | `question.md` §9 — **end-to-end** under 1 second |

*(Optional)* If you ever need a single narrative document again, **derive a new spec from the Q-files**; do not resurrect `draft-idea.txt` as source of truth.
