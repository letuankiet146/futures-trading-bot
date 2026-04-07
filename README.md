# draft-idea-trading (M1 + M2 Scaffold)

This repository is scaffolded for `implementation-spec-v1.md` milestones M1 and M2.

## Modules

- `shared-contracts`: shared Kafka contract DTOs.
- `strategy-service`: publishes `strategy.signal`.
- `execute-service`: consumes `strategy.signal` when `TRADING_MODE=LIVE`.
- `simulate-service`: consumes `strategy.signal` when `TRADING_MODE=SIMULATE` (default).

## Quick start

1. Copy environment template:
   - `copy .env.example .env`
2. Start stack:
   - `docker compose up --build`
3. Verify health:
   - `http://localhost:8081/actuator/health`
   - `http://localhost:8082/actuator/health`
   - `http://localhost:8083/actuator/health`
4. Publish a test signal:
   - `POST http://localhost:8081/api/v1/signals/test`
   - Body:
     ```json
     {
       "symbol": "BTCUSDT",
       "side": "BUY",
       "price": 65000.0
     }
     ```

Expected behavior:
- SIMULATE mode: `simulate-service` logs signal consumption.
- LIVE mode: `execute-service` logs signal consumption.

## M3 additions (strategy pipeline)

- Strategy service now includes:
  - REST kline bootstrap (`/fapi/v1/klines` once at startup)
  - WebSocket listeners for:
    - `<symbol>@markPrice`
    - `<symbol>@kline_<interval>`
  - Peak/trough scanning over closed candles (`N`, `K`)
  - Signal gate checks:
    - `|avg_top - avg_bottom| <= similarity_threshold`
    - `max(avg_top, avg_bottom) > 4 * taker_fee`

### Backtest API mode

- Preconditions:
  - `BACKTEST_ENABLED=true`
  - `TRADING_MODE=SIMULATE`
  - optional `BACKTEST_OHLC_ORDER=OHLC|OLHC`
- Trigger:
  - `POST /api/v1/backtest/run` with JSON body `{ "startDate": "<...>", "endDate": "<...>" }` (both optional; if **both omitted** or empty body `{}`, replays the **latest 1500** fully closed klines; if only `startDate`, end defaults to latest closed; `startDate` is **required** when `endDate` is set; values: **ISO-8601**, epoch ms string, or **date-only** per `implementation-spec-v1.md`).
  - Response `202` + `jobId`; poll `GET /api/v1/backtest/jobs/{jobId}` for status and result.
- Behavior:
  - Strategy checks DB kline cache first for requested symbol/interval/date range.
  - If data is missing, strategy calls Binance REST klines with paged batches (max `1500` rows per request), anchored near `endDate`, and continues loading until `startDate` is covered.
  - Newly loaded klines are persisted in DB for future runs (to avoid repeated Binance calls).
  - Backtest replays each candle with 4-price path.
  - For each OHLC price: runs strategy engine, then publishes `MARK` (and `SIGNAL` when fired) to Kafka topic `TOPIC_SIMULATE_REPLAY` so `simulate-service` can update mark and TP/SL/liquidation in order.
  - Live mode still uses `strategy.signal` only; backtest uses the replay topic for ordered paper feed.

## M4 additions (execute live path)

- `execute-service` now includes:
  - Exchange filter bootstrap from Binance `exchangeInfo`.
  - Risk sizing by account percent/fixed notional with minQty/minNotional and precision checks.
  - Rolling 24h realized-loss bucket with pause on cap (`20%` default).
  - Authenticated unblock endpoint:
    - `POST /api/v1/risk/unblock` with header `X-Admin-Token`.
  - Live order flow using Binance Futures REST:
    - Market entry
    - TP and SL reduce-only conditional orders (`MARK_PRICE` working type)

### Risk utility endpoints

- `GET /api/v1/risk/state`
- `POST /api/v1/risk/realized-pnl` (admin token required) to update realized loss in current bucket.

## M5 additions (simulate path)

- `simulate-service` now includes paper trade lifecycle:
  - Risk sizing in simulate mode (percent/fixed notional).
  - Market fill at last mark price.
  - TP/SL lifecycle using mark-price checks.
  - Simplified liquidation rule (unrealized loss threshold over isolated margin).
  - Freeze account on liquidation until reset.
  - Stats tracking:
    - `winCount`, `loseCount`, `liquidationCount`, `totalTrades`, `totalPnl`, `totalFees`
    - liquidation count is mutually exclusive from win/lose.

### Simulate utility endpoints

- `GET /api/v1/simulate/state`
- `POST /api/v1/simulate/mark` with body `{ \"price\": <mark> }`
- `POST /api/v1/simulate/reset` with header `X-Admin-Token`

## M6 additions (persistence + Flyway + audit)

- Added JPA + PostgreSQL + Flyway to `execute-service` and `simulate-service`.
- Schema ownership:
  - `execute` schema: live orders, positions, fills, risk bucket state, audit log.
  - `simulate` schema: paper account snapshot, paper orders, positions, fills, audit log.
- Flyway baseline migrations:
  - `execute-service/src/main/resources/db/migration/V1_0_0__baseline_execute.sql`
  - `simulate-service/src/main/resources/db/migration/V1_0_0__baseline_simulate.sql`
- Added audit services and persistence write points in M4/M5 flows.
- Added integration-test baseline for both services (Spring context + repository wiring under `test` profile).

## M7 additions (operational hardening)

- Latency policy enforcement in realtime consumers:
  - warn over `LATENCY_WARN_MS`
  - block at/over `LATENCY_BLOCK_MS`
- Live reconciliation safety guard:
  - scheduled balance reconciliation every `RECONCILIATION_SYNC_MS`
  - safe mode when drift exceeds `RECONCILIATION_DRIFT_USDT` or reconciliation fails
  - live execution blocked while safe mode is active
- Added runbook: `RUNBOOK-M7.md`

## M2 contract notes

- `strategy.signal` contains required fields:
  - `schemaVersion`, `side`, `symbol`, `price`, `correlationId`, `timestamp`
- Kafka key is `symbol`.
- Consumers apply `log-and-skip` for malformed payloads.
