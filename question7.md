# Follow-up questions (implementation & operations)

Canonical stack: `question.md` → `question2.md` → `question3.md` → `peak-trough-spec.md` → `question5.md` → `question6.md` → **this file**.

The trading rules, risk cap, and swing logic are **specified enough to start building**. Items below close gaps that otherwise force **arbitrary choices** during integration (Kafka, persistence, operator workflows).

Answer inline or replace with your decisions.

---

## 1. Fee gate — which scalar is compared to `4 × taker_fee`?

`question2.md` §6 requires **average relative drawdown** > **4 × taker_fee**. You already have two numbers: **avg_top** and **avg_bottom** (`question.md` §4).

- Should the gate require **both** `avg_top > 4 × taker_fee` **and** `avg_bottom > 4 × taker_fee`, or **min(avg_top, avg_bottom)**, or **(avg_top + avg_bottom) / 2`, or another rule?

*Default suggestion if you do not care:* require **both** inequalities (conservative and matches “in the middle of the gap” intuition).

**Answer:** The **first** gate stays as in `question.md` §4: **|avg_top − avg_bottom| ≤ 0.001** (treat the two averages as “equivalent” within that tolerance). **Only after** that condition holds, evaluate the fee rule using the **larger** of the two values: require **max(avg_top, avg_bottom) > 4 × taker_fee** (with `taker_fee` as the decimal rate per `question2.md` §6). Intuition: once price is in the middle of the gap, the **stronger** of the two average relative drawdowns must still exceed the fee budget.

---

## 2. Manual risk reset (live) — how does the operator unblock?

After the **20% realized-loss** pause (`question6.md` §2, `question.md` §2), how should **manual unlock** work in v1?

- e.g. **REST endpoint** (authenticated) on execute/risk service, **environment flag** flip + restart, **DB row** toggled by admin SQL, or **dedicated small ops UI** later?

**Answer:** Provide a **REST endpoint** on the **execute / risk** service (or the service that owns risk state) that **clears the pause** and **allows new orders again** after successful invocation. The endpoint **must be authenticated** (exact mechanism—API key, mTLS, admin token—is an implementation detail; v1 must not expose an unauthenticated unlock). **Scope:** applies to **live** trading pause only; simulate mode is unchanged.

---

## 3. Kafka — topic names and minimal payloads (first pass)

You chose **environment-based** config (`question.md` §1). For v1, please confirm or fill in:

- **Topic names** (examples): `market.mark-price`, `market.kline`, `strategy.signal`, `execute.order`, `simulate.fill` — or a single prefix per symbol?
- **Key** partitioning: partition by **symbol** only (single symbol per deploy) or **none** for v1?
- **Required fields** (minimum) for: **strategy signal** (side, symbol, reference price, correlation id, timestamp), **execution request** after risk sizing (add qty, client order id), **simulate fill event**.

*It is fine to answer “use the suggested names; fields as implemented in code” if you want the implementer to define the JSON schema once in a shared module.*

**Answer:**

- **Topic naming:** Base names stay as in the examples (`market.mark-price`, `market.kline`, `strategy.signal`, `execute.order`, `simulate.fill`, etc.). A **symbol segment may be prepended or embedded** as a prefix (e.g. `BTCUSDT.strategy.signal`) so topics are namespaced per symbol when useful; exact pattern is **configurable via environment**.
- **Partitioning:** Use the **symbol** (e.g. `BTCUSDT`) as the **Kafka record key** for partitioning so all traffic for one symbol routes consistently (aligned with single-symbol-per-deployment).
- **Strategy signal — minimum payload:** `side`, `symbol`, `price` (reference / log price), `correlationId`, `timestamp` (ISO-8601 or epoch ms — choose one in code).
- **Live trading:** After a strategy signal, the **execute** path (risk + execution) **computes size** from risk rules and **sends** the sized order to the **exchange**. Downstream messages (e.g. toward execution) **must include** whatever the exchange and idempotency need—at minimum **quantity**, **client order id**, and bracket/TP-SL parameters as implemented; **JSON field names** are defined once in a shared contract module.
- **Simulate / paper trading:** The **paper-trade** service **performs its own risk sizing**, **simulates** order submission, and **evaluates fills** internally (no live exchange). It does **not** rely on execute for sizing in this mode; behavior matches `question.md` (one mode active at a time).

---

## 4. PostgreSQL — scope for v1

`question.md` §10: PostgreSQL for live and simulate, no retention policy.

- Which **entities** must persist in v1: **orders**, **positions**, **fills**, **risk bucket state**, **paper account snapshot**, **audit log** — all of the above or a minimal subset for the first milestone?

**Answer:** Persist **all** of the following in v1: **orders**, **positions**, **fills**, **risk bucket state**, **paper account snapshot**, and **audit log**. This is the full persistence scope for the first milestone (schema and table layout are implementation details).

---

## 5. Service split vs. repo layout

Original idea: **queue**, **execute** (risk + engine), **simulate**, **strategy** (`draft-idea.txt`, deprecated).

- Confirm **v1** is still **four deployable services** (or fewer / more), and whether you want a **monorepo** with one Docker Compose or separate repos later.

**Answer:** Use a **monorepo** for v1 (single codebase, easier coordination). Architecture remains **microservices**: **separate deployable services** (strategy, execute, simulate, plus **Kafka** and **PostgreSQL** as infrastructure in Compose—not application services). **Each application service is built as its own Docker image** (one Dockerfile or multi-stage target per service from the monorepo). **Orchestration** for local/single-machine deployment is **Docker Compose**, which wires images, env, and dependencies (Kafka, Postgres, etc.).

---

## 6. End-to-end latency < 1s — enforcement

`question.md` §9: measure **end-to-end** (market tick → order submitted).

- Is this a **design target** (optimize where possible) or a **hard SLO** (alert / reject if exceeded)? Any **excluded** segments (e.g. backtest-only path)?

**Answer:**

- **Soft band (1 second):** If measured end-to-end latency is **greater than 1 second**, **emit a warning** (structured log / metric / alert as implemented), but **still allow the trade path to execute** (no automatic block solely for crossing 1s).
- **Hard SLO (5 seconds):** If end-to-end latency is **≥ 5 seconds**, treat this as a **hard limit — do not submit the order** (block execution for that path). **Still emit a warning** when blocking.
- **Backtest / offline replay:** The **< 1s / 5s** rules apply to **live and simulate “real-time” paths** only. **Backtest** (as fast as possible per `question.md` §6) is **excluded** from these latency SLOs.

---

## 7. “Live mode” price for TP/SL reference

`question.md` §5: in live mode, “we check price at position” for TP/SL reference.

- Confirm v1 uses **mark price** (same family as strategy) vs **last trade** vs **index** for bracket distance / monitoring consistency with Binance order types you pick.

**Answer:** In v1, **bracket distances, monitoring, and TP/SL reference** in **live** mode use **mark price** (aligned with the strategy’s `markPrice` stream and USDT-M Futures conventions). **Last** and **index** prices are **not** the primary reference for this path unless Binance order parameters require a specific `workingType`—configure orders accordingly so triggers stay consistent with **mark**-based logic.

---

*Archive or merge into a single “implementation spec” once answered.*