# Open questions (post–`question5.md`)

Canonical stack: `question.md` → `question2.md` → `question3.md` → `peak-trough-spec.md` → `**question5.md` (answered)**.  
**§1 drawdown aggregation** is **answered** below and merged into `**question.md` §4**. **§2** (max-loss cap) is **answered** below.

---

## 1. “Average” drawdown — aggregation over swing lists

**Context:** `peak-trough-spec.md` yields **peaks[]** and **troughs[]** (swing highs/lows) over the last **N** closes. `question.md` §4 still requires **avg drawdown of top**, **avg drawdown of bottom**, the gate **|avg_top − avg_bottom| ≤ 0.001**, relative drawdown vs **4 × taker_fee**, and TP/SL distances tied to those drawdowns.

**Open:** Define **exact formulas**:

- How to compute **avg_top** from **peaks[]** (e.g. mean of `(Pⱼ − mark) / Pⱼ` for all peaks **Pⱼ > mark**, or only the **last M** peaks, or the **nearest** peak above mark)?
- How to compute **avg_bottom** from **troughs[]** (symmetric with **lows** and **mark**)?
- If the lists are **empty** or no peak above / trough below mark, should the strategy **skip** the tick or use a **fallback**?

Answer: The **count** of peaks and troughs is already **bounded** by the **N**-candle buffer in `**peak-trough-spec.md`**. Let **mark** be current mark price (same as elsewhere in the strategy). Use **relative** drawdowns (`**question.md` §4**).  

- **avg_top** = arithmetic **mean** over **every** swing high price **P** in **peaks[]** of **(P − mark) / P**.  
- **avg_bottom** = arithmetic **mean** over **every** swing low price **T** in **troughs[]** of **(mark − T) / T**.  
- If **peaks[]** is **empty**, set **avg_top = 0**. If **troughs[]** is **empty**, set **avg_bottom = 0**.  
Merged into `**question.md` §4**; `**peak-trough-spec.md` §7** points here.

---

## 2. **Max loss** per rolling 24h — numeric limit (optional)

`question.md` §2: **max loss** and **current loss** are **per day** (rolling **24h from first trade**, per `question5.md` §7), **realized** only.

**Open (if risk v1 should enforce a cap):** What **numeric** **max_loss** (USDT or % of balance) **blocks new orders** when **current_loss** in the bucket reaches it? Or confirm **“no hard cap in v1”** — only persistence fields until custom logic is added.

**Answer:** **Yes — enforce a hard cap in v1.** When **cumulative realized loss** in the current risk bucket reaches **20%** of **account equity at the start of that bucket** (the opening equity for the period — i.e. “start of the day” in the sense of the bucket’s anchor, consistent with `question5.md` §7 and `question.md` §2), the system **must pause all new orders** and **remain halted until the operator performs a manual risk reset / explicit unlock** (trading does not resume automatically).

---

*Archive or delete this file once §2 is resolved or explicitly deferred.*