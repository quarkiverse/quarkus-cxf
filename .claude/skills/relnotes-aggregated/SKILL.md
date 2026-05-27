---
name: relnotes-aggregated
description: Create release notes aggregated over several Quarkus CXF release versions, typically for the sake of upgrading between LTS streams. 
Usage: /relnotes-aggregated <current-lts-version> <previous-lts-version> (e.g., /relnotes-aggregated 3.33.1 3.27.3)
---

# Release Notes Skill

Create release aggregated over several Quarkus CXF releases.

The interval of versions to consider is given by the first and the second argument.

## Steps

### 1. Determine the release versions included in the interval

Use 

```bash
git log <previous-lts-version>..<current-lts-version> --simplify-by-decoration --pretty=%D | grep -oP 'tag: \K[^,]+' | grep -v 'CR[0-9]$' | tac
```

### 2. Gather release notes for each version

Each of the release versions from the previous step has a release notes document in `docs/modules/ROOT/pages/release-notes`.
Read each of those documents, collect the entries and aggregate them using the format described in the next step.

### 3. Check existing aggregated release notes for style and structure of the document

Read the following release note documents to learn the style:

* `docs/modules/ROOT/pages/release-notes/3.20.2-aggregated.adoc`
* `docs/modules/ROOT/pages/release-notes/3.27.1-aggregated.adoc`

The format follows these conventions:

- **Title**: `= Aggregated {quarkus-cxf-project-name} release notes <previous-lts-version> LTS -> <current-lts-version> LTS` 
- **Sections** (include only those that apply):
  - `== Important dependency upgrades` — bullet list of upgraded dependencies. 
  - `== Enhancements` — for new features or enhancements
  - `== Bugfixes` — bug fixes, each as a `===` subsection with issue links
  - `== Deprecations` — deprecated features
  - `== Breaking changes` — if any
- **Footer**: Always end with:
  ```
  == Full changelog

  https://github.com/quarkiverse/quarkus-cxf/compare/<previous-version>+++...+++<version>
  ```

- Use `{quarkus-cxf-project-name}` attribute instead of writing "Quarkus CXF" literally

### 4. Write the release notes file

Write the file to `docs/modules/ROOT/pages/release-notes/<current-lts-version>-aggregated.adoc`.
Do not ask the user whether the file can be created or updated, just create and/or update the file however you need.

### 5. Update nav.adoc

Edit `docs/modules/ROOT/nav.adoc`. Add a new entry in the release notes section, maintaining version-descending order. 
The entry goes after the `ifeval::[{doc-is-main} == true]` line, among the other `** xref:release-notes/...` entries.

Format: `** xref:release-notes/<version>.adoc[<version>]` (add ` LTS` suffix if applicable).

Insert it in the correct position to maintain descending version order.

### 6. Update index.adoc

Edit `docs/modules/ROOT/pages/release-notes/index.adoc`. Add a new row in the table, maintaining version-descending order within the appropriate minor version group.

Format: `| xref:release-notes/<version>.adoc[<version>] | <date in YYYY-MM-DD format, when the release was tagged> | | <cxf-version>`

- `<cxf-version>` is the value of `cxf.version` property in the project's `pom.xml` top level directory.
- Add `LTS` suffix to the version label if applicable
- Leave the Quarkus Platform column empty initially — ask the user if they know the values, otherwise leave them blank for now
- Group the entry with other releases of the same minor version (e.g., 3.33.x entries are together)

### 7. Commit the changes

Commit the changes in a new topic branch using:

```bash
git checkout -b "$(date +%y%m%d)-release-notes-aggregated<version>"
git add -A
git commit -m "Release notes aggregated <previous-lts-version> LTS -> <current-lts-version> LTS"
```
