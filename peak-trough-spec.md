# Peak and trough identification (normative reference)

This document is the **single source of truth** for how **đỉnh (peaks)** and **đáy (troughs)** are detected for the trading system. Any module that needs peaks or troughs — **strategy engine** (signals, drawdowns, TP/SL distances), **backtest** replay, and **documentation** that describes price structure — must follow this spec.

**Related (not duplicated here):** order sizing, risk, Kafka, execution — see `question.md`, `question2.md`, `question3.md`. **Aggregating** peaks/troughs into **avg_top** / **avg_bottom**: **mean** of relative drawdowns over **all** swings — see **`question.md` §4** (aggregation bullet) and **`question6.md` §1**. Use the **lists** produced by this document as inputs.

---

## 1. Idea (matches chart intuition)

- Price moves in **waves**. A **peak** is a **local high** where price stops rising and turns down (swing high). A **trough** is a **local low** where price stops falling and turns up (swing low).
- Use **OHLC candles** on the strategy **interval** (default **`15m`**, configurable — see `question5.md` §4).
- **Peaks** are derived from **high** (râu trên / wick top). **Troughs** are derived from **low** (râu dưới).

---

## 2. Parameters

| Parameter | Meaning | Default | Notes |
|-----------|---------|---------|--------|
| **N** | Number of most recent **closed** candles **loaded** into memory before scanning | **500** | Configurable; align with kline history fetch (see `question.md` §4, max 1500). |
| **K** | Half-width of the swing: each candidate peak/trough must be strictly higher/lower than **K** candles on the **left** and **K** candles on the **right** | **2** | Configurable. Requires **2K+1** bars to center one swing; larger **K** → fewer, “larger” swings. |

Indices below use **closed** candles only, ordered oldest → newest within the buffer of length **N**.

---

## 3. Swing high (peak / đỉnh)

Consider candle at index **i** in the buffer (0 … N−1). It is a **swing high** if:

1. **K** ≤ **i** ≤ **N − 1 − K** (enough bars on both sides).  
2. **Strict local maximum on `high`:**  
   `high[i] > max( high[i−K] … high[i−1] )` **and** `high[i] > max( high[i+1] … high[i+K] )`  
   (maximums taken over the **K** bars strictly to the left and **K** bars strictly to the right of **i**).

Output each swing high as at least: **index i**, **time**, **`high[i]`** (peak price).

---

## 4. Swing low (trough / đáy)

Candle at index **i** is a **swing low** if:

1. **K** ≤ **i** ≤ **N − 1 − K**.  
2. **Strict local minimum on `low`:**  
   `low[i] < min( low[i−K] … low[i−1] )` **and** `low[i] < min( low[i+1] … low[i+K] )`.

Output each swing low as at least: **index i**, **time**, **`low[i]`** (trough price).

---

## 5. Algorithm (batch scan)

1. Load the **last N closed** candles for the symbol and interval (REST bootstrap + WS kline updates — see `question2.md` §8).  
2. For **i** from **K** to **N − 1 − K**, test the conditions in §3 and §4; append to **peaks[]** and **troughs[]**.  
3. Sort or keep **chronological order** by time or index.

**Backtest:** same scan on the rolling or expanding history for each step (per `question2.md` §1 OHLC path).

---

## 6. Live trading — confirmation (no look-ahead)

A candle at **i** can only be validated as a swing high/low after candles **i+1 … i+K** exist and close — i.e. the **right** side of the pattern is **fully known**. In practice:

- When processing **live** streams, only treat index **i** as a confirmed peak/trough after the **(i+K)-th** candle after **i** has **closed**.  
- The **2s** `markPrice` tick uses **current price** for drawdown math; the **set of confirmed** swings updates when new candles close.

This matches “chỉ có thể nhìn lại nến khi đã hình thành (trong quá khứ).”

---

## 7. Strategy consumption

- Pass **peaks[]** and **troughs[]** and **mark** into **`question.md` §4**: **avg_top** = mean of **(P − mark) / P** over all **P** in **peaks[]**; **avg_bottom** = mean of **(mark − T) / T** over all **T** in **troughs[]**; empty list → **0** for that average. Then apply gates, side, TP/SL per **`question.md`**.

---

## 8. Document map (must reference this file)

| Document | What to do |
|----------|------------|
| `question.md` §4 | Strategy thresholds; point **peak/trough detection** here. |
| `question2.md` §4 | Short summary + **link to this file** (replace duplicate algorithm text). |
| `question3.md` §2 | Non-repainting / live: **link to §6** here. |
| `question4.md` | Stale `draft-idea.txt` row: canonical peaks/troughs = **this file** + `question.md` §4. |
| `question5.md` §1 | Resolved (see **`question6.md` §1** + **`question.md` §4** aggregation). |

---

*Version: aligned with user chart intent (multiple local peaks/troughs in an N-candle buffer).*
