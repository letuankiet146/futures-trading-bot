# Residual questions (architecture & v1 defaults)

Canonical stack: `question.md` → `question2.md` → `question3.md` → `peak-trough-spec.md` → `question5.md` → `question6.md` → `question7.md` → **this file**.

The stack is **implementation-ready** for core trading logic, risk, Kafka shape, persistence scope, compose layout, latency rules, and live TP/SL reference. Items below are **optional refinements** that avoid arbitrary choices during build; answer inline or defer to "implementer's choice" in code.

---

## 1. Paper / simulate — TP–SL reference price

`question7.md` §7 locks **mark price** for **live** bracket reference. For **simulate**, fills use the **last pushed mark** (`question.md` §7).

- Confirm **paper TP/SL distance and trigger checks** also use **mark price** (internal stream), so **live vs simulate** stay aligned aside from exchange vs simulated execution.

*Default if unanswered:* **Yes — mark price for paper bracket logic too.**

**Answer:** **Yes.** Use **mark price** for paper/simulate TP-SL distance and trigger checks.

---

## 2. PostgreSQL topology (monorepo microservices)

`question7.md` §4 lists entities to persist; **ownership** across services is not fixed.

- **One shared PostgreSQL instance** with **one database** and **schema per service** (or table prefixes), vs **one database / shared tables** with clear owners, vs **separate DBs** on the same server?

*Default if unanswered:* **single Postgres container, one database, schemas or prefixes per owning service** (execute owns orders/positions/fills for live; simulate owns paper snapshot; shared audit/risk state by convention).

**Answer:** Use one PostgreSQL **instance** with **schema per service**.

---

## 3. Backtest runner packaging

Backtest uses REST klines + OHLC loop (`question.md` §6, `question2.md` §1).

- Should **backtest** run as a **mode inside the strategy service** (CLI / profile), as a **separate Spring Boot main** in the monorepo, or **deferred** until after live/simulate paths work?

**Answer:** Backtest runs **inside Strategy** and can be triggered in a **standalone mode**.

---

## 4. Binance **testnet** vs **mainnet**

- Is v1 expected to support **switching via environment** (e.g. base URL + keys for Binance Futures testnet) for safe integration testing, or **mainnet only** until a later milestone?

**Answer:** Use **mainnet**.

---

## 5. Rolling **24h** risk bucket — rollover

`question5.md` §7 and `question6.md` §2: realized loss vs equity **at bucket start**.

- When the **24h window rolls**, confirm the **next** bucket’s **opening equity** = **account equity at rollover time** (snapshot), and **realized loss** in the new bucket resets to **0**.

*Default if unanswered:* **yes** (standard rolling bucket).

**Answer:** **Yes.** On rollover, the next bucket snapshots opening equity at rollover time and resets realized loss to `0`.

---

## 6. Strategy **taker fee** for the `4 × taker_fee` gate

`question.md` §4 gives **0.05%** taker as the assumed rate.

- Should this rate be **configurable** (e.g. `.env` for VIP tiers) in v1, or **hard-coded** until ops needs it?

**Answer:** Taker fee should be **configurable**.

---

## 7. Kafka — failures and poison messages

- Does v1 require a **DLQ** (dead-letter topic) + retry policy for consumers, or **log-and-skip** / **manual replay** is acceptable for the first release?

**Answer:** v1 uses **log-and-skip**.

---

## 8. DB migrations tool

- Preference for **Flyway**, **Liquibase**, or **none** (DDL scripts by hand) in the monorepo?

**Answer:** Use **Flyway** for DB migrations in v1.

---

*Archive or merge answers into a single implementation guide once decided.*