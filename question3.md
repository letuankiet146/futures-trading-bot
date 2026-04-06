# Residual questions (after `question2.md`)

`question2.md` is **largely sufficient** to start architecture and implementation. The items below are **edge cases** that can still cause inconsistent behavior between services or ambiguous metrics unless decided.

---

## 1. Win / lose / liquidation counters (simulate)

When a position **closes via liquidation**, should statistics increment:

- **only** `liquidation_count`, or  
- **`liquidation_count` and** `lose_count`, or  
- **`liquidation_count` only**, and **win/lose** apply **only** to TP/SL closes?

Clarify whether counts are **mutually exclusive** per close and how **win rate** is defined (denominator includes liquidations or not).

Answer: Increment **`liquidation_count` only** when a position closes via **liquidation** — do **not** increment **`lose_count`** for that event. **`win_count`** and **`lose_count`** apply **only** to positions closed by **TP or SL** (bracket exits). Each closed position increments **exactly one** of `win_count`, `lose_count`, or `liquidation_count` (mutually exclusive). **Win rate** (if shown) = **`win_count / (win_count + lose_count)`** — **liquidations are excluded** from both numerator and denominator.

---

## 2. Pivot confirmation vs live streaming (no look-ahead)

Peaks/troughs on **close** with window **K** are only **fully knowable** at bar `i` after **K** future closes exist. In **live** mode, `markPrice` updates every **2s**, while **candle close** is fixed at bar boundaries.

- Should the strategy use **only confirmed** swings (no repaint: a peak at bar `i` is used only after bar `i+K` closes), or  
- Allow **provisional** pivots that may disappear (not recommended without explicit spec)?

State the **non-repainting rule** for live and how it interacts with the **2s** trigger.

Answer: See **`peak-trough-spec.md` §6** (confirmation, no look-ahead). Peak/trough lists come from **`peak-trough-spec.md`** over the last **N** closed candles. The **2s** `markPrice` stream provides **current price** for signals; swings are **confirmed** only after the right-side bars exist, per that file.

---

## 3. Margin mode parity (live vs simulate)

Simulate uses **isolated** margin and an **80%-of-margin** liquidation rule.

- Must **live** trading also use **isolated** margin on Binance for the same symbol, or can live be **cross** while simulate stays isolated?

If modes differ, document expected **behavioral differences** (risk, liquidation).

Answer: **Yes — parity.** **Live** trading must use **isolated** margin for the same symbol (and position mode aligned with simulate) so liquidation and margin behavior stay comparable to paper. **Do not** run live **cross** while simulate is isolated.

---

## 4. Minimum notional vs exchange `MIN_NOTIONAL`

`question2` §3 previously mentioned a **> 5 USDT** style floor; Binance **per-symbol** filters (e.g. `MIN_NOTIONAL`, `minQty`) are authoritative.

- Confirm: enforce **exchange info from REST** as **source of truth**, with **5 USDT** as a **documentation default** only when the API value is absent, or **hard** 5 USDT everywhere?

Answer: **Use the exchange as source of truth.** Load and apply **per-symbol** rules from Binance **Futures exchange info** (e.g. `MIN_NOTIONAL`, quantity/price filters, step sizes) via REST **at initialization** (see `question5.md` §5 — no periodic refresh in v1). **Do not** enforce a **hard-coded 5 USDT** in application logic. Many USDT-M symbols use a ~5 USDT notional floor in practice, but **values can differ** — validate against **loaded filters**. `question2.md` §3 is updated to match.

---

*If you answer inline here, merge into the main spec and archive or delete this file.*