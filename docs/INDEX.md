# enterprisewebagent Documentation Index

## Architecture & Status
- [ARCHITECTURE.md](./ARCHITECTURE.md) — current (hosted) runtime architecture, module inventory, data flows (Phase 4 complete)
- [PARITY_VERIFICATION.md](./PARITY_VERIFICATION.md) — Step 17 parity checklist (14/14 PASS)
- [DISTRIBUTED_ARCHITECTURE.md](./DISTRIBUTED_ARCHITECTURE.md) — target: fat Go agent owns the runtime; Spring server is an LLM proxy + control plane + system-of-record; CLI/web are dummy clients
- [AGENT_AND_LICENSING.md](./AGENT_AND_LICENSING.md) — the Go agent, license/entitlement model, install flow, and the LLM proxy + config-distribution contracts
- [DISTRIBUTED_MIGRATION_PLAN.md](./DISTRIBUTED_MIGRATION_PLAN.md) — numbered 18-step plan (4 milestones) to reach the distributed topology

## Product & Requirements
- [PRODUCT.md](./PRODUCT.md)
- [PRD.md](./PRD.md)
- [fsd-enterprisewebagent.md](./fsd-enterprisewebagent.md)
- [trd-enterprisewebagent.md](./trd-enterprisewebagent.md)
- [SCENARIOS.md](./SCENARIOS.md)
- [NON_GOALS.md](./NON_GOALS.md)

## Technical Design
- [lld-core-parity.md](./lld-core-parity.md) — low-level parity contract
- [reference-current-core-prompt-system.md](./reference-current-core-prompt-system.md) — behavioral reference
- [EXECUTION_PIPELINE.md](./EXECUTION_PIPELINE.md)

## Implementation
- [IMPLEMENTATION_HANDOFF.md](./IMPLEMENTATION_HANDOFF.md) — 17-step guide (all steps complete)
- [prompts/](./prompts) — 12 prompt surface definitions (001–012)

## Recommended Reading Order

1. `PRODUCT.md` — what this is
2. `PRD.md` — what it needs to do
3. `ARCHITECTURE.md` — how it's built (start here if you're a developer)
4. `trd-enterprisewebagent.md` — technical requirements
5. `lld-core-parity.md` — what must be preserved from the original core
6. `IMPLEMENTATION_HANDOFF.md` — how it was built (17 steps)
7. `PARITY_VERIFICATION.md` — verification results
8. `prompts/` — the 12 prompt surfaces
