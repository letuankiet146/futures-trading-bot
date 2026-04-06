# Remaining clarifications (canonical spec: `question.md` → `question2.md` → `question3.md` → `peak-trough-spec.md`)

All items below are **answered**. Drawdown aggregation: **`question6.md` §1** + **`question.md` §4** (aggregation bullet).

---

## 1. “Average” drawdown — aggregation over swing lists

**Resolved** — see **`question6.md` §1** and **`question.md` §4**.

---

## 2. Binance **position mode**: one-way vs hedge

Isolated margin is fixed; Binance still requires **one-way** or **hedge** (dual-side) for order parameters (`positionSide`, etc.).

- Which mode must **v1** use for **live** and **simulate** (should match)?

Answer: **One-way** mode for **v1** on both **live** and **simulate** (same net position semantics; orders use the one-way position model — no dual `LONG`/`SHORT` sides on the same symbol).

---

## 3. **Leverage** default

No global **leverage** default is specified (it affects isolated margin, liquidation distance, and simulate PnL).

- **Default leverage** in config (e.g. 5×, 10×, 20×) vs **no default** (must set explicitly in `.env` before trade).

Answer: **Default leverage = 20×** for both **simulate** and **live**; **configurable** via environment / config (e.g. `.env`). Implementation must read the value at startup and apply it consistently to margin, notional, and simulate liquidation logic.

---

## 4. **Kline interval** default

Candle **history length N** is defined in **`peak-trough-spec.md`** (default 500). `question.md` says the candle **interval** is configurable.

- **Default interval** for live/backtest (e.g. **1m**, **5m**) so kline WS + REST bootstrap align with the same series used for swing detection; **`markPrice`** remains on its own 2s tick.

Answer: **Default interval = `15m`** (15-minute candles). **Configurable** (e.g. `1m`, `5m`, `15m`, …). REST klines bootstrap, kline WebSocket (`<symbol>@kline_15m` when using default), and swing scan in **`peak-trough-spec.md`** must use the **same** interval setting.

---

## 5. **Exchange info** refresh (filters / `MIN_NOTIONAL`)

`question3.md` §4 says load rules from REST; risk sync is **30s** for balance.

- Should **symbol filters** (min notional, steps) refresh **only at startup**, or on a **schedule** (e.g. daily / with balance sync), or on **order reject**?

Answer: Load **exchange info** (symbol filters: `MIN_NOTIONAL`, steps, etc.) **at service initialization** (and on **manual restart** / redeploy). **No** periodic refresh in v1; **optional** future enhancement: refresh on order reject if a filter error is detected.

---

## 6. **Simulate** starting balance

Paper account needs an initial **USDT** balance for margin simulation.

- **Default starting balance** (e.g. 10_000 USDT) or **config-only** with no code default.

Answer: **Default starting balance = 500 USDT** for paper / simulate mode; **configurable** (e.g. `.env`).

---

## 7. Risk **“per day”** boundary

`question.md` §2: max loss / current loss **per day**, **realized** PnL.

- **Day** definition: **UTC** calendar day, **exchange** day, or **rolling 24h** from first trade?

Answer: **Rolling 24 hours from first trade** — the risk “day” is a **24-hour window anchored at the timestamp of the first trade** in that window (not UTC midnight or exchange calendar day). **Max loss** / **current loss** (realized) roll against this bucket.

---

## 8. **Pyramiding** / multiple positions

Single **symbol** per deployment is set; not always explicit whether **at most one open position** (net) per symbol at a time.

- **v1:** only **one** open position per symbol (no add-on / no second leg until flat), or allow **multiple** orders building size?

Answer: **v1 — single active trade cycle.** At most **one entry order** may be **working** (open, not yet filled). After it **fills**, there is **exactly one** position with **one TP** and **one SL** (bracket). **Do not** place another entry or add size until **both** TP and SL legs are **resolved** (one fills, the other is cancelled per bracket rules) and the position is **flat**. No pyramiding, no second entry while TP/SL are live.

---

*Archive or delete this file once answered.*