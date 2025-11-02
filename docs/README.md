# Auth Service Knowledge Base

Welcome to the reworked docs hub. Everything here is rewritten for newcomers who want to understand the “why” before the “how”. Follow the path below to build context, wire the platform into your project, and keep the guardrails sharp.

## Choose Your Adventure

- **Just joined?** Start with `start/welcome.md` for a narrated first week, then take the `start/platform-tour.md` walkthrough to meet every moving part.
- **Need plain-language concepts?** Read the `foundations/` guides. They explain RBAC, data guardrails, and PostgreSQL roles with everyday metaphors.
- **Ready to build something?** Use the `guides/` playbooks. They connect the concepts to hands-on steps like wiring a new role or checking permissions.
- **Stuck or debugging?** Jump into `playbooks/troubleshoot-auth.md` for symptom-based fixes.
- **Looking for hard data?** The curated summaries live in `reference/`. Legacy deep dives remain in `reference/raw/` when you need full tables or historical notes.

## Map Of The Folder

| Folder | What You’ll Find | When To Visit |
| --- | --- | --- |
| `start/` | Welcome story, platform tour, role personas | First day context |
| `foundations/` | Concepts explained with analogies and diagrams | Before changing code |
| `guides/` | Step-by-step recipes tied to the setup scripts | When doing the work |
| `playbooks/` | Troubleshooting and checklists | When something feels off |
| `reference/` | Role catalogues, policy summaries, operational notes | Need facts or numbers |
| `reference/raw/` | Original phase documents, exhaustive matrices | Historical or exhaustive lookup |
| `ONBOARDING/setup/` | SQL bootstrap scripts (left untouched) | Database seeding |

## How To Use The Docs

1. **Build intuition** – Read from `start/` to `foundations/` so the architecture feels familiar.
2. **Follow the recipes** – Use `guides/` while executing the scripts in `ONBOARDING/setup/`.
3. **Verify your work** – Run the quick checks in `guides/verify-permissions.md` and keep `playbooks/` handy for surprises.
4. **Dive deeper when needed** – Leverage `reference/` for concise tables and hop into `reference/raw/` if you need the original phase-by-phase detail.

## Keeping Things Fresh

- Add or update the narrative guides first; link to raw data instead of copying tables into multiple files.
- When new roles, capabilities, or tables appear, update the summaries in `reference/` and cross-link the relevant guide.
- Prefer examples rooted in real-world analogies—assuming the reader has never built an auth service before keeps the docs approachable.
