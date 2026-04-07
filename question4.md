# `draft-idea.txt` vs implementation docs (alignment note)

`draft-idea.txt` is the **core product idea**. `question*.md` and `peak-trough-spec.md` refine and operationalize ambiguous points for implementation.

## Reference order (authoritative precedence)

Use these files **in order**:

1. **`draft-idea.txt`** — core scope and intent.
2. **`question.md`** — base implementation decisions.
3. **`question2.md`** — follow-up decisions.
4. **`question3.md`** — edge-case decisions.
5. **`peak-trough-spec.md`** — normative peak/trough detection algorithm.
6. **`question5.md`**, **`question6.md`**, **`question7.md`**, **`question8.md`** — defaults and operational clarifications.

Implementation and design reviews should start from `draft-idea.txt`, then apply clarifications from the Q-files and `peak-trough-spec.md`.

---

## Known deltas between core idea and refinements

These are not open questions. They are places where later Q-file answers made an implementation-specific choice to refine the core idea.

| Topic | Core text in `draft-idea.txt` | Refined decision |
|--------|-------------------------------|------------------|
| Peaks / troughs / “avg” drawdown | Many peaks/troughs, plural “averages” | **`peak-trough-spec.md`** + `question.md` §4 (aggregation / drawdown) |
| Binance order API | “Use WS API for send request” | `question.md` §3 — **REST** for orders (connector compatibility and implementation stability) |
| Latency | “each service under 1 second” | `question.md` §9 + `question7.md` §6 — **end-to-end** rule with warn/block thresholds |

If you need one narrative spec, derive it from **`draft-idea.txt` + all question files + `peak-trough-spec.md`**.
