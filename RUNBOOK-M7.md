# M7 Operational Runbook

This runbook covers latency policy, reconciliation safe mode, and quick operator actions for v1.

## 1) Latency policy

Applied to realtime LIVE/SIMULATE consume paths (not backtest):

- If end-to-end latency `> LATENCY_WARN_MS` (default 1000ms): warn, continue.
- If end-to-end latency `>= LATENCY_BLOCK_MS` (default 5000ms): warn and block execution for that signal.

Signal timestamp source: `StrategySignalEvent.timestamp` (ISO-8601).

## 2) Reconciliation safety guard (LIVE)

`execute-service` runs scheduled reconciliation every `RECONCILIATION_SYNC_MS` (default 30000ms):

- Compare local cached balance vs exchange balance.
- If drift `> RECONCILIATION_DRIFT_USDT`, enter safe mode, overwrite local from exchange, then clear safe mode.
- If reconciliation fails (API/network), safe mode stays active and execution is blocked until next successful sync.

When safe mode is active, `LiveExecutionService` blocks new live execution.

## 3) Operational checks

- Health:
  - `GET /actuator/health` on strategy/execute/simulate
- Risk status (live):
  - `GET /api/v1/risk/state`
  - `POST /api/v1/risk/unblock` with `X-Admin-Token`
- Simulate status:
  - `GET /api/v1/simulate/state`
  - `POST /api/v1/simulate/reset` with `X-Admin-Token`

## 4) Incident playbook

- **High latency spikes**:
  1. Check logs for `LATENCY_BLOCK` and threshold values.
  2. Verify Kafka and service CPU/memory saturation.
  3. Tune `LATENCY_WARN_MS` and `LATENCY_BLOCK_MS` only with explicit approval.

- **Reconciliation safe mode stuck**:
  1. Validate Binance key/secret and network reachability.
  2. Confirm exchange balance endpoint responds.
  3. After recovery, ensure logs show `RECONCILE_RECOVERED`.

- **Risk pause active**:
  1. Confirm state via `GET /api/v1/risk/state`.
  2. Investigate realized-loss updates.
  3. Use manual unblock endpoint only after operator review.
