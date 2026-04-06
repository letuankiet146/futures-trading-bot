# Clarifying questions (draft trading system)

Questions below aim to remove ambiguity before design and implementation. Answer inline or replace with your decisions.

---

## 1. Message queue and contracts

- Which broker do you want: **RabbitMQ**, **Apache Kafka**, **Redis Streams**, or another?  
Answer: Apache Kafka
- What is the **exact payload schema** (JSON fields) for: execution requests, market price for simulation, strategy signals (side, symbol, price — any order id, client id, mode flag)?  
Answer: Json format. fields name it is up to you.
- How should services **discover** queue names / topics (convention vs config per environment)?  
Answer: Use environment

---

## 2. Risk engine (execute service)

- Define **max loss** and **current loss** precisely: per day, per open position, lifetime account, or something else? How is current loss updated (realized only, or unrealized)?  
Answer: per day, realzied pnl
- **Max loss enforcement:** When **realized loss** in the rolling **24h** bucket (see `question5.md` §7) reaches **20%** of **account equity at bucket start**, **pause** new orders until **manual restart** / **risk reset** — **`question6.md` §2**.
- On **balance sync every 30s**: if REST and local state diverge, which source wins? Should trading pause until reconciled?  
Answer: Exchange source win
- Default values and ranges for **position sizing**: percent of account vs fixed notional — defaults per symbol? Minimum order size from exchange is clear; is there a **maximum** cap you want?  
Answer: Yes.
- **Risk allow/deny** is “default yes” for now — do you still want a **kill switch** (env flag) to block all orders immediately?  
Answer: No

---

## 3. Execute engine — Binance

- You specified **WebSocket for send request** — confirm the intended API: **WebSocket API** (e.g. `order.place` style) vs **REST** for orders. The Java connector may differ; should we align strictly with what the official `binance-futures-connector-java` supports?  
Answer: Use REST
- **OKX placeholder**: any requirement for **shared interfaces** now (same DTOs for both exchanges) or is a stub class enough?  
Answer: No shared

---

## 4. Strategy — peak / trough detection

- **Algorithm**: How should peaks and troughs be identified? Examples: local max/min over a sliding window of **K** candles; zigzag with a **deviation** threshold; pivot points. What are **K** or threshold values (or should this be configurable)?  
Answer: **Normative algorithm:** **`peak-trough-spec.md`** (load **N** closed candles default **500**, scan all swing peaks/troughs with parameter **K**). Strategy uses those lists for drawdown / signals per bullets below.
- `**N` recent candlesticks**: What is **N** (default)? Is `<interval>` fixed (e.g. `1m`, `5m`) or configurable per run?  
Answer: N is default 500, max 1500. iinterval is configurable.
- **“Almost the same”** (rounded to 2 decimals): What is the **maximum allowed difference** between “avg drawdown of top” and “avg drawdown of bottom” to still treat price as “middle of the gap”? (Rounding alone can hide small differences.)  
Answer: |avg_top − avg_bottom| ≤ 0.001
- **Fee check**: Which **fee rate** (maker/taker, VIP tier) should the strategy assume so that “avg must greater than fee” is well-defined?  
Answer: taker: 0.05%, maker: 0.02%
- **Drawdown definition**: Is “drawdown from peak” = `(peak - current) / peak` or absolute price distance, and same style for troughs? Confirm formulas.  
Answer: **relative**
- **Aggregation (“avg” over swing lists):** How to compute **avg_top** and **avg_bottom** from **peaks[]** / **troughs[]**?  
Answer: **avg_top** = **mean** over **all** swing highs **P** in **peaks[]** of **(P − mark) / P** where **mark** is current mark price. **avg_bottom** = **mean** over **all** swing lows **T** in **troughs[]** of **(mark − T) / T**. If **peaks[]** is empty, **avg_top = 0**; if **troughs[]** is empty, **avg_bottom = 0**. (Lists are already bounded by the **N**-candle window — see **`peak-trough-spec.md`** and **`question6.md` §1**.)

---

## 5. Strategy — signals, TP/SL, and execution

- Strategy emits **side, symbol, price** without quantity — who **always** attaches size: risk engine only, or simulate service as well (duplicate logic)?  
Answer: risk engine if live trading. simulate if not in mode live trading. we enable one of both mode. not both
- For **take profit** and **stop loss**: Should the system send **bracket / OCO** orders on Binance, or **monitor price** in a service and close manually? Paper trading must mirror the same behavior — which model do you want first?  
Answer: **use OCO. Paper trade have logic the same.**
- **Market order** signal: Confirm **no limit price**; is the `price` field only for logging / TP-SL reference, or used elsewhere?  
Answer: Yes no limit price, allow slippage. Yes need to logg current price at this time (if simulate mode) for TP-SL reference. if live mode we check price at position.

---

## 6. Backtest loop (REST klines)

- When looping **OHLC** for one candle, in what **order** should the strategy see prices: **O → H → L → C**, **O → L → H → C**, or only **open and close**? This affects signals and simulation fills.  
Answer: use 4 prices: open, high, low and close.
- Should backtest **replay speed** be configurable (e.g. as fast as possible vs real-time delay)?  
Answer: as fast as posible.

---

## 7. Simulate trading service

- **Fill model** for market orders: fill at **last pushed mark price** instantly, or add **slippage** model?  
Answer: fill at last pushed marke price
- **Liquidation**: Full **Binance USDT-M isolated/cross** formula, or a simplified rule (e.g. margin ratio below X)? Any **funding** simulation?  
Answer: No funding simulation
- After **liquidation**, should the **paper account reset** automatically or require manual restart? You said no further trading — confirm **process exit** vs **account frozen until reset**.  
Answer: account frozen util reset (restart service)

---

## 8. Modes and configuration

- How is **Simulate vs live execute** selected: **single global config**, per-strategy flag, or message field on each signal?  
Answer: global config (simulate mode as default)
- **Symbols**: Single symbol per deployment or **multi-symbol** with separate queues / routing keys?  
Answer: single symbol (configurable - BTCUSDT as default)

---

## 9. Observability and ops

- Required **logging**: structured JSON, correlation id across services?  
Answer: Yes.
- **Latency under 1 second**: Measured as **p95 per service HTTP/gRPC**, **queue delay**, or **end-to-end** from market tick to order? Any priority path (e.g. execute only)?  
Answer: end to end.

---

## 10. Data persistence (Spring Boot)

- Preferred **database** for balances, orders, history (PostgreSQL, MySQL, in-memory for paper only)? Any **retention** policy for trade history?  
Answer: use PostgreSQL for live mode and simulate mode. No retention, I will clean up manually.

---

## 11. Security and deployment

- **API keys**: Futures permissions — **trade only** vs **withdraw disabled**; IP whitelist in Binance — will Docker hosts have **stable IPs**?  
Answer: Not for now. I will config later.
- **Environment**: Single machine (docker-compose) only, or future **Kubernetes** — any constraint on the compose file structure now?  
Answer: Signle machine.

---

*Add or strike questions as you refine the spec.*