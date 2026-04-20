---
name: relnotes
description: Create release notes for a specific Quarkus CXF release version. 
Usage: /relnotes <version> (e.g., /release-notes 3.33.2)
  or /relnotes <version> <previous-version> (e.g., /release-notes 3.35.0 3.33.1)
---

# Release Notes Skill

Create release notes for a Quarkus CXF release.

The version number is passed as the first non-optional argument (e.g., `3.33.2`).

## Steps

### 1. Parse the version

Extract the version number from the first (required) argument. It must be a semantic version like `3.33.2` following the naming scheme `<major>.<minor>.<micro or patch>`.

### 1.1 Determine the previous version 

If previous version is specified via the second command argument, use that one.

Otherwise determine the previous version by looking at existing git tags on the branch of the release notes version.
The name of the branch is either `main` for release notes versions ending with `.0` or `<major>.<minor>` for Long Term 
Support (LTS) branches).
Only tags matching `<major>.<minor>.<micro>`, where all of `<major>`, `<minor>` and `<micro>` must be numeric, 
are relevant for release notes other tags can be ignored.

If the requested release notes version does not end with `.0`, the previous version is the tag immediately before 
the requested version in sorted order.

Use:

```bash
git tag --list '<major>.<minor>.*' | sort -V
```

If the requested version ends with `.0`, the previous version is the highest patch of the previous minor,
e.g., for release notes version `3.34.0`, find the latest `3.33.x` tag, or the latest `3.32.x` and so on.

Verify that the requested version tag exists in git:

```bash
git tag --list '<version>'
```

If you cannot find and suitable previous version, abort and inform the user.

### 2. Gather changes

Run these commands to understand what changed:

```bash
git log --oneline <previous-version>...<version>
```

Look at the commit messages for:
- Dependency upgrades (Quarkus, CXF, XJC plugins, etc.)
- New features
- Bug fixes
- Deprecations
- Breaking changes
- References to GitHub issues/PRs (patterns like `#1234`, `fixes #1234`, etc.)

### 3. Consult GitHub issues and PRs

For each GitHub issue or PR number referenced in commits, fetch details using:

```bash
gh issue view <number> --repo quarkiverse/quarkus-cxf
gh pr view <number> --repo quarkiverse/quarkus-cxf
```

Also list PRs merged between the two tags:

```bash
gh pr list --repo quarkiverse/quarkus-cxf --state merged --search "merged:$(git log -1 --format=%ci <previous-version> | cut -d' ' -f1)..$(git log -1 --format=%ci <version> | cut -d' ' -f1)" --limit 100
```

### 4. Check existing release notes for style

Read the 2-3 most recent release notes files in `docs/modules/ROOT/pages/release-notes/` to match their style. The format follows these conventions:

- **Title**: `= {quarkus-cxf-project-name} <version> release notes` (add `LTS` suffix for LTS releases — versions in LTS streams like 3.8.x, 3.15.x, 3.20.x, 3.27.x, 3.33.x)
- **Sections** (include only those that apply):
  - `== Important dependency upgrades` — bullet list of upgraded dependencies with links to their release notes
  - `== Enhancements` — for new features or enhancements, each as a `===` subsection. Link GitHub issues in the heading like `=== https://github.com/quarkiverse/quarkus-cxf/issues/NNN[#NNN] Title`
  - `== Bugfixes` — bug fixes, each as a `===` subsection with issue links
  - `== Deprecations` — deprecated features
  - `== Breaking changes` — if any
- **Footer**: Always end with:
  ```
  == Full changelog

  https://github.com/quarkiverse/quarkus-cxf/compare/<previous-version>+++...+++<version>
  ```

- If there are no user-facing changes, use a minimal format:
  ```
  = {quarkus-cxf-project-name} <version> release notes

  There are no end user facing changes in this release.

  == Full changelog

  https://github.com/quarkiverse/quarkus-cxf/compare/<previous-version>+++...+++<version>
  ```

- Use `{quarkus-cxf-project-name}` attribute instead of writing "Quarkus CXF" literally
- When describing behavior changes, use the pattern: "Before {quarkus-cxf-project-name} <version>, ... Since {quarkus-cxf-project-name} <version>, ..."
- Link configuration options using xref syntax: `xref:reference/extensions/quarkus-cxf.adoc#quarkus-cxf_quarkus-cxf-...[quarkus.cxf....]`
- Credit contributors with `Special thanks to https://github.com/<user>[@<user>]`

### 5. Write the release notes file

Write the file to `docs/modules/ROOT/pages/release-notes/<version>.adoc`.

Before writing, show the user a draft of the release notes content and ask for approval or edits.

### 6. Update nav.adoc

Edit `docs/modules/ROOT/nav.adoc`. Add a new entry in the release notes section, maintaining version-descending order. The entry goes after the `ifeval::[{doc-is-main} == true]` line, among the other `** xref:release-notes/...` entries.

Format: `** xref:release-notes/<version>.adoc[<version>]` (add ` LTS` suffix if applicable).

Insert it in the correct position to maintain descending version order.

### 7. Update index.adoc

Edit `docs/modules/ROOT/pages/release-notes/index.adoc`. Add a new row in the table, maintaining version-descending order within the appropriate minor version group.

Format: `| xref:release-notes/<version>.adoc[<version>] | <today's date YYYY-MM-DD> | | <cxf-version>`

- `<cxf-version>` is the value of `cxf.version` property in the project's `pom.xml` top level directory.
- Add `LTS` suffix to the version label if applicable
- Leave the Quarkus Platform column empty initially — ask the user if they know the values, otherwise leave them blank for now
- Group the entry with other releases of the same minor version (e.g., 3.33.x entries are together)

### 8. Summary

Report what was created and updated. Remind the user to:
- Review the generated release notes for accuracy
- Fill in the Quarkus Platform version column in index.adoc if left empty
