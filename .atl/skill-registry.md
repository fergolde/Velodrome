# Skill Registry

**Generated**: Mon Apr 13 2026
**Project**: Velodrome

## User-Level Skills (from ~/.config/opencode/skills/)

| Trigger | Skill Name | Path |
|---------|-----------|------|
| Writing Go tests, Bubbletea TUI testing | go-testing | ~/.config/opencode/skills/go-testing/SKILL.md |
| Creating PR, opening PR, preparing changes for review | branch-pr | ~/.config/opencode/skills/branch-pr/SKILL.md |
| Creating GitHub issue, reporting bug, requesting feature | issue-creation | ~/.config/opencode/skills/issue-creation/SKILL.md |
| Creating new AI skills, adding agent instructions | skill-creator | ~/.config/opencode/skills/skill-creator/SKILL.md |
| Judgment day, dual review, adversarial review | judgment-day | ~/.config/opencode/skills/judgment-day/SKILL.md |
| SDD phases (explore, propose, spec, design, tasks, apply, verify, archive) | sdd-* | ~/.config/opencode/skills/sdd-*/SKILL.md |

## Project-Level Skills

None detected.

## Compact Rules

### go-testing
- Use `teatest` library for Bubbletea TUI testing
- Prefer table-driven tests for multiple input/output cases
- Use `t.Log` / `t.Errorf` for test output
- Mock external dependencies (database, HTTP) with interfaces

### branch-pr
- Follow issue-first enforcement system
- Include PR description with summary and testing notes
- Request review from appropriate reviewers
- Ensure all CI checks pass before merging

### issue-creation
- Follow issue-first enforcement system
- Include detailed description, reproduction steps, expected behavior
- Attach screenshots/logs where relevant
- Use appropriate labels

### skill-creator
- Write SKILL.md with frontmatter (name, description, license, metadata)
- Include skill_files section for bundled resources
- Document triggers in description
- Follow skill-creator template structure

### judgment-day
- Launch two independent blind judge sub-agents simultaneously
- Synthesize findings, apply fixes
- Re-judge until both pass or escalate after 2 iterations

### sdd-init
- Detect tech stack, testing capabilities, conventions
- Default to engram mode (no openspec directory)
- Persist testing capabilities to engram
- Create skill registry in .atl/

### sdd-explore
- Explore codebase to understand existing patterns
- Identify affected modules/packages
- Provide findings as structured output

### sdd-propose
- Create change proposal with intent, scope, approach
- Include affected modules, risks, rollback plan
- Follow proposal template

### sdd-spec
- Write specifications with requirements and scenarios
- Use Given/When/Then format for scenarios
- Use RFC 2119 keywords (MUST, SHALL, SHOULD, MAY)

### sdd-design
- Create technical design document
- Include architecture decisions, sequence diagrams
- Document rationale for choices

### sdd-tasks
- Break down change into implementation task checklist
- Group by phase (infrastructure, implementation, testing)
- Use hierarchical numbering

### sdd-apply
- Implement tasks following specs and design
- Follow existing code patterns
- Load relevant coding skills

### sdd-verify
- Run tests if infrastructure exists
- Compare implementation against every spec scenario
- Report discrepancies

### sdd-archive
- Sync delta specs to main specs
- Archive completed change
- Warn before destructive deltas

## Project Conventions

No convention files detected in project root.

## Notes

- This is an Android/Kotlin project with Jetpack Compose
- No Go, Python, or other non-JVM stacks detected
- SDD workflow uses engram for persistence (no openspec directory)