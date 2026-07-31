---
name: complete-change
description: >
  Make a cross-cutting change across the codebase and verify it compiles.
  Use when the user asks to rename a type across the project,
  update a naming convention throughout the codebase,
  or apply any change that touches both source code and documentation.
  Also use when the user asks to "generate a plan" and you want to validate it.
---

# Complete Change

A procedure for making a change that touches multiple files, ensuring both source code and documentation stay in sync, then verifying the result compiles.

## Steps

### Step 1 — Scope

Identify every category of file the change affects:

- Source code (`.java`)
- Build/Config (`.xml`, `.yml`, `.properties`)
- Documentation (`.md` in `docs/`)
- Tests (`.java` in `src/test/`)

Use `grep` / `rg` to find all references before touching anything. Record the count per category so you know when you're done.

**Completion criterion:** a list of every file that needs a change, grouped by category, with a reference count.

---

### Step 2 — Code first

Apply changes to source files first. Use the most mechanical tool available:

- **Rename refactoring**: `mv` files first, then `sed` bulk-replace across all `.java` files
- **Structural change**: edit each file individually

Do NOT touch documentation yet — code changes might reveal edge cases that change what the docs should say.

**Completion criterion:** every `.java` file identified in Step 1 has been updated.

---

### Step 3 — Build verify

Run the project's build command to confirm compilation.

```
./mvnw compile -q
```

If compilation fails, go back to Step 2 and fix. The error messages tell you exactly what's still broken.

**Completion criterion:** `mvn compile` exits with code 0.

---

### Step 4 — Documentation sync

Update every `.md` file identified in Step 1.

Common doc types in this project:

| Doc | Typical content |
|-----|----------------|
| `CODING_STANDARDS.md` | Naming conventions, project structure |
| `DDD.md` | Package definitions, module boundaries |
| `admin-user-management.md` | Feature specs |
| `admin-role-hierarchy.md` | Permission model |

For each doc, search for the old term and replace. If a naming rule changed, update the rule text and all examples that reference it.

**Completion criterion:** every `.md` file identified in Step 1 has been updated, and there are no stale references to the old term.

---

### Step 5 — Final verify

Run the build again to confirm documentation-only changes didn't break anything (they shouldn't, but verify anyway).

```
./mvnw compile -q
```

If tests exist and can run without external services:

```
./mvnw test -q
```

**Completion criterion:** `mvn compile` exits with code 0.
