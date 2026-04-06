# Follow-up questions (after answers in `question.md`)

These items are still **underspecified** for implementation or testing, even after the inline answers.

---

## 1. OHLC replay order (backtest)

You chose to use **four prices** per candle (open, high, low, close). The **sequence** still matters for signals and fills (e.g. whether **high** is seen before **low**).

- In what **strict order** should the engine process them: **O → H → L → C**, **O → L → H → C**, or another rule (e.g. worst-case ordering)?

Answer: Default **O → H → L → C** — process open, then high, then low, then close in that order for each candle; fixed assumed intrabar path so backtest and simulation fills are reproducible. **Optional:** **O → L → H → C** (open, then low, then high, then close) must be supported as a **configurable backtest mode** when you want to stress or compare results under the alternate intrabar path; run one full backtest per selected order (do not mix sequences within the same run).

---

## 2. Balance reconciliation — pause trading?

You answered **exchange wins** when REST and local state diverge. It was not stated whether the system should **pause new orders** until state is overwritten and consistent.

- Should **trading pause** (or enter a safe mode) until reconciliation completes after a mismatch is detected?

Answer: **Yes** — pause new orders (safe mode) until local state is overwritten from the exchange and reconciliation completes; resume trading only when balances and risk state are consistent with REST. **Scope:** apply this behavior **only in live trading**; **simulate / paper trading** does not sync with the exchange, so this pause-and-reconcile flow does not apply there.

---

## 3. Position sizing — maximum cap

You confirmed there should be a **maximum** cap. The **numeric cap** (or rule: e.g. max % of balance, max USDT notional) was not specified.

- What is the **exact cap** (value + unit), or a formula tied to balance / symbol?

Answer: Maximum order size is a **configurable percentage of account balance** denominated in **USDT** (notional / margin semantics as implemented by risk and exchange rules). **Default:** **2%** of USDT balance. **Minimum notional / qty:** enforce **per-symbol** Binance Futures filters from **exchange info** (e.g. `MIN_NOTIONAL`, `minQty`, tick/step rules) — **no hard-coded 5 USDT**; validate **notional** (and fees impact) against the **current** symbol rules from REST. See `question3.md` §4.

---

## 4. Pivot points — parameters

You chose **pivot points** for peaks/troughs. Classic pivot implementations still use parameters (e.g. **number of bars** left/right, or a specific formula).

- Which **pivot definition** (library rule or formula) should be used, and are **any parameters** fixed or configurable?

Answer: **Normative definition:** **`peak-trough-spec.md`**. Load **N** most recent **closed** candles (default **N = 500**, configurable; max 1500 per `question.md` §4), then scan to enumerate **all** swing **peaks** (local highs on **`high`**) and **troughs** (local lows on **`low`**) using half-width **K** (default **2**, configurable). Do **not** duplicate the algorithm here — implement from that file. Strategy formulas (relative drawdown, averages, side, TP/SL) stay in **`question.md` §4** and consume the **peaks[]** / **troughs[]** lists from the spec. **Live:** confirmation and `markPrice` behavior — see **`peak-trough-spec.md` §6**; **2s** tick per `question.md`.

---

## 5. OCO / TP–SL on Binance **USDT-M Futures**

You asked for **OCO** with the same behavior in paper trading. On **Futures**, product behavior differs from **Spot OCO**; TP/SL are often expressed as **conditional orders** (e.g. `STOP_MARKET`, `TAKE_PROFIT_MARKET`) or **algo** endpoints, depending on API version.

- Confirm the **intended Binance Futures API surface** (order types and whether “OCO” means **spot-style OCO** or **a Futures-equivalent bracket**). If you are flexible, state “implement whatever Binance Futures supports for one position’s TP+SL with cancel-on-fill semantics.”

Answer: **Bracket semantics (not Spot-style OCO by name):** one open position is protected by **take-profit** and **stop-loss**; when **one** leg **fills**, the **other is cancelled** (cancel-on-fill). **Implement using whatever order types and endpoints Binance USDT-M Futures actually supports** (e.g. conditional / TP / SL styles as per current API docs), as long as behavior matches: **TP+SL for the same position, one triggers → cancel the other**. **Paper trading** must mirror the same lifecycle (fill one leg → cancel sibling → update position / PnL accordingly).

---

## 6. Drawdown comparison vs fee

Strategy compares **average drawdown** to **fee**. Fees are given as **taker 0.05%** and **maker 0.02%** (rates). Drawdown is **relative** (dimensionless ratio).

- For the check **“avg greater than fee”**, should **fee** be interpreted as the **taker rate** (e.g. market entry), and compared directly to **relative** drawdown values (same scale), or should fee be converted to a **price distance** per unit notional?

Answer: Compare **average relative drawdown** (dimensionless, same scale as price ratios) to **4 × taker_fee**, where `taker_fee` is the decimal rate (e.g. 0.05% → `0.0005`). The **4×** factor is the chosen budget to **cover open and close** (and small real-world deviation); **actual total fees may differ slightly**. Condition: **avg_relative_drawdown > 4 × taker_fee**. **Leverage** does not change this inequality when both sides are price-relative rates. **Maker rate** is not used for this gate (market-style entries).

---

## 7. Liquidation model (simulate)

You excluded **funding** simulation. It was not specified whether liquidation follows **full Binance USDT-M rules** or a **simplified** rule.

- **Full exchange formula** vs **simplified** (e.g. margin ratio threshold): which should v1 implement?

Answer: **Simplified model** for v1 (no full Binance liquidation formula). Positions use **isolated margin**. **Liquidation rule:** when **unrealized loss on that isolated position reaches 80% of the margin allocated to that position** (loss has “hit 80%” of that position’s backing margin), **liquidate that position** at the simulated mark price, then apply the existing simulate policy (e.g. account frozen until service reset). **Simulate statistics** must record **win count**, **lose count**, and **liquidation count** (classify each closed position: win vs loss from PnL sign / TP–SL outcome; **liquidation** as its own outcome when this rule fires).

---

## 8. Candle history — REST vs WebSocket (live)

- How should the strategy service **hydrate** and **update** closed candles without excessive REST calls?

Answer: **Bootstrap with REST once** (or on reconnect): `GET /fapi/v1/klines` to load enough history for the strategy window. **Thereafter**, subscribe to the Binance Futures **kline WebSocket** for the configured interval (default **`15m`** per `question5.md` §4; e.g. `<symbol>@kline_15m`) and **maintain the candle series in memory (or local store)**—update the forming candle, finalize on candle close, append new bars. **Do not** poll REST on a timer for klines in steady state; rely on WS for incremental updates. **`markPrice`** stream remains separate for the 2s strategy tick. See canonical spec in `question.md` / `question2.md` (Market data engine); `draft-idea.txt` is deprecated.

---

*Strike items once decided and optionally merge answers into the main spec.*