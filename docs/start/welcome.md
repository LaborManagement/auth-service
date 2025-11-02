# Welcome Aboard

**Optional Prelude:** After this story, begin the guided path at `../architecture/overview.md`.

You’ve joined the team that keeps payments safe. This guide is your first-week companion—no jargon, just the story of how everything fits together and what to read next.

## Day 1: Go From Map To Meaning

- **Skim the platform narrative** in `platform-tour.md`. Imagine the auth service as the main gate of a stadium: every fan, staff member, and delivery truck passes through it before reaching their seats.
- **Bookmark the setup scripts** under `../ONBOARDING/setup/`. Those SQL files are the assembly instructions for the gate—roles, policies, and RLS wiring—in the correct order.
- **Create a sandbox branch** in the codebase so you can experiment without risking production.

## Day 2: Meet The People Behind The Roles

- Read `role-stories.md` to understand each persona. Think of the platform roles like airport badges: security staff, pilots, ground crew, and VIP passengers all need different doors unlocked.
- Check out `foundations/access-control-101.md` for a plain-language tour of RBAC with analogies you can use when explaining it to others.
- Open `guides/local-environment.md` and confirm you have the prerequisites (Java, Docker, PostgreSQL access).

## Day 3: Hands On With Permissions

- Follow the “Paint Your First Capability” section in `guides/extend-access.md`. You’ll wire a tiny permission end-to-end so the concepts stick.
- Run the smoke tests from `guides/verify-permissions.md` to see how policies, endpoints, and RLS checks line up.
- If anything fails, hop into `playbooks/troubleshoot-auth.md`. It’s organized by symptom, not technology, so you can work from the error message backwards.

## Day 4: Explore The Data Guardrails

- Dive into `foundations/data-guardrails-101.md`. Picture it as a hotel elevator that only stops on floors your keycard allows—you can see the buttons, but pressing them does nothing without the right clearance.
- Review the quick reference in `reference/vpd-checklist.md` while stepping through `ONBOARDING/setup/08_configure_vpd.sql`.
- Note the difference between PostgreSQL roles and RBAC roles (they share a name but guard different doors).

## Day 5: Connect It To Your Project

- Use `guides/integrate-your-service.md` to plug the auth service into another application. It mirrors what you’ll do for real features.
- Compare your results with the examples in `reference/role-catalog.md` so you know you wired the right capabilities.
- Add a short note to the team’s knowledge base describing what you learned—teaching is the surest way to cement the model.

## Stay Curious

- The **Foundations** folder turns difficult topics into stories and diagrams.
- The **Guides** folder gives you repeatable recipes.
- The **Playbooks** folder exists for the days when things go sideways.
- The **Reference** folder is your quick lookup (with `reference/raw/` holding the original deep dives).

Take breaks, ask questions, and remember: every complex access decision boils down to a badge, a rulebook, and a guardrail. You’ll master them in no time.
