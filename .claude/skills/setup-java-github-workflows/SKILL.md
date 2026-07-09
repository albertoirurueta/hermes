---
name: setup-java-github-workflows
description: Create or update the `develop.yml` and `main.yml` GitHub Actions workflows for a Java/Maven library repository — build and test, run Checkstyle/PMD/SpotBugs static analysis, generate JaCoCo coverage and the Maven `site` report, send results to SonarQube/SonarCloud, build the Antora documentation site, publish both the Antora docs and the Maven site report to GitHub Pages, and publish the release artifact to Maven Central. Invoke as `/setup-java-github-workflows`. Ships with generic example templates (derived from a real repository's workflows, genericized so they don't name any specific repository) embedded in this skill file, reusable across repositories. Creates both workflows from scratch if neither exists; if either already exists, asks the user whether to stop or attempt an update using the templates as reference. Accepts the six pipeline parameters (integration branch, Java version, publishing server id, extras/sign profile ids, settings file) pre-resolved via `args` (`key: value` lines) so an orchestrating skill like `setup-java-library-repository` can supply them without re-prompting. Use whenever a Java/Maven repository needs this CI/CD release pipeline bootstrapped or brought in line with this house pattern, instead of hand-writing the YAML.
---

# Setup Java GitHub Workflows

Scaffold (or update) two GitHub Actions workflows for a Java/Maven library:

- **`develop.yml`** — runs on every push to the integration branch. Build, test, static analysis, coverage, a
  SonarQube/SonarCloud scan, an Antora docs build, and a publish to GitHub Pages + Maven Central.
- **`main.yml`** — the same pipeline, but triggered when a GitHub Release is published rather than on every push.

This skill is designed for Maven projects — the templates in Step 3 assume Maven coordinates, profiles, and
`mvn` commands. If `pom.xml` is missing at the repository root, don't stop automatically: warn the user that this
skill's templates assume Maven and most of Step 1's survey won't resolve, then use `AskUserQuestion` to ask
whether to stop here or continue anyway. If they choose to continue, proceed through the remaining steps but treat
every Maven-derived placeholder in Step 3/4 (Java version, publishing server id, signing/extras profile ids,
`mvn` commands) as an open gap to ask the user about directly, and call this out prominently again in Step 8's
report.

## Step 1 — Survey the target repository

This skill can be invoked stand-alone (`/setup-java-github-workflows`) or as a step inside another skill, such as
`setup-java-library-repository`, which resolves the six pipeline parameters below itself and passes them through
`args` as `key: value` lines, one per line, e.g.:

```
integration-branch: develop
java-version: 17
publishing-server-id: central
extras-profile-id: build-extras
sign-profile-id: sign
settings-file: mvnsettings.xml
```

Parse any such lines from `args` first. For each of `integration-branch`, `java-version`,
`publishing-server-id`, `extras-profile-id`, `sign-profile-id`, and `settings-file` found there, use that value
directly — skip the corresponding fact-finding bullet below entirely for it, since it's already resolved. If
`args` is absent or doesn't look like this format, treat everything as unset and gather every fact below as usual.

Gather the remaining facts before writing anything; every placeholder in Step 3's templates must be resolved from
a real answer, not guessed. If `pom.xml` is missing and the user chose to continue anyway, skip the
`pom.xml`-derived facts below and ask the user for equivalent values directly instead:

- **Maven coordinates**: read `groupId`/`artifactId`/`version` from `pom.xml`.
- **Java version** (skip if supplied via `args`): read `maven.compiler.source`/`maven.compiler.target` (or
  `maven.compiler.release`) from `pom.xml` properties.
- **Central/Nexus publishing plugin** (skip the `publishing-server-id` half if supplied via `args`): grep
  `pom.xml` for `central-publishing-maven-plugin` (the current Sonatype Central Publishing Portal plugin) or
  `nexus-staging-maven-plugin` (the older OSSRH plugin). Record the `publishingServerId`/`serverId` value it
  declares (e.g. `central`) — the workflow's `setup-java` `server-id` and the Maven settings file's `<server><id>`
  must both match this value. If neither plugin is configured, tell the user Maven Central publishing isn't
  wired up in `pom.xml` yet and ask whether to continue anyway (the generated deploy step will need adjustment
  once that's added).
- **GPG signing profile** (skip if `sign-profile-id` supplied via `args`): find the Maven profile that binds
  `maven-gpg-plugin`'s `sign` goal. Record its profile id (e.g. `sign`).
- **Extras profile** (skip if `extras-profile-id` supplied via `args`): find the profile — typically active by
  default — that attaches sources/javadoc jars (e.g. via `maven-source-plugin` + `maven-javadoc-plugin`). Record
  its id (e.g. `build-extras`). This is the profile the plain test/coverage run excludes (`-P '!<id>'`) so a quick
  `mvn test` doesn't rebuild javadoc every time, while the final `deploy` includes it.
- **Static analysis / coverage plugins**: confirm `jacoco-maven-plugin`, `maven-checkstyle-plugin`,
  `spotbugs-maven-plugin`, and `maven-pmd-plugin` are configured under `<reporting>` — these feed the `mvn site`
  report. Not required to proceed, but note any that are missing so the user knows that section of the site report
  will be empty.
- **SonarQube/SonarCloud config**: check for `sonar-project.properties` at the repository root, or `sonar.*`
  properties in `pom.xml`. If neither exists, this skill still wires up the `sonar-scanner` step (Step 3), but
  someone must supply `sonar.projectKey`/`sonar.organization`/`sonar.host.url` before it will run successfully —
  flag this and offer to create `sonar-project.properties` in Step 6 if the user can give you those values now.
- **Antora docs**: check for `docs/antora.yml` and `docs/antora-playbook.yml`. If either is missing, the docs step
  in Step 3 has nothing to build against — tell the user to run the `antora-setup` skill first, or confirm they'll
  do so before merging this workflow.
- **Maven settings file used for deploy** (skip if `settings-file` supplied via `args`): check for an XML file
  referenced by a `--settings` flag in any existing deploy step, defaulting to `mvnsettings.xml` at the repository
  root if none is referenced yet. If it doesn't exist, this skill creates it (Step 6).
- **Branch names** (skip the integration branch if `integration-branch` supplied via `args`): confirm the
  integration branch (commonly `develop`) and the stable branch (commonly `main`) actually match this
  repository's branching model — run `git branch -a` or ask, don't assume gitflow defaults.

## Step 2 — Decide how to proceed if workflows already exist

Check whether `.github/workflows/develop.yml` and `.github/workflows/main.yml` already exist.

- **Neither exists**: skip this step and go straight to Step 4 — create both from scratch.
- **Either exists**: use `AskUserQuestion` to ask whether to (a) stop here and leave both workflows untouched, or
  (b) continue and attempt to update the existing workflow(s) using this skill's templates as reference.
  - **Stop**: report which file(s) already exist and end here — make no changes.
  - **Continue**: proceed to Step 4, but treat each existing file as the base to edit, not as something to
    overwrite wholesale. Preserve any step that isn't one of the eight pipeline stages this skill owns (build/test,
    static analysis, coverage, site report, SonarQube, Antora build, GitHub Pages publish, Maven Central publish)
    — e.g. a Slack notification step or an extra test matrix stays untouched. Only add missing stages or correct
    outdated ones (wrong action version, wrong profile id, etc.), and don't reorder steps this skill doesn't own.

## Step 3 — Reference templates

These are genericized examples — based on a real Java/Maven repository's actual `develop.yml`/`main.yml` — showing
the full pipeline: checkout → JDK setup with signing configured → test + coverage + site report → SonarQube scan →
Antora docs build → merge docs with the site report → publish to GitHub Pages → deploy to Maven Central. Copy the
structure; resolve every `<placeholder>` using Step 1's survey before writing the real files in Step 4.

### `develop.yml` template

```yaml
name: Develop

on:
  push:
    branches: [ <integration-branch> ]

jobs:
  build:
    name: Build and execute tests
    runs-on: ubuntu-latest
    steps:
      - name: Check out code
        uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - name: Set up JDK <java-version>
        uses: actions/setup-java@v5
        with:
          distribution: adopt
          java-version: <java-version>
          server-id: <publishing-server-id> # Value of the distributionManagement/repository/id field of the pom.xml
          server-username: OSSRH_USERNAME # env variable for username in deploy
          server-password: OSSRH_PASSWORD # env variable for token in deploy
          gpg-private-key: ${{ secrets.SIGNING_KEY }} # Value of the GPG private key to import
          gpg-passphrase: SIGNING_PASSWORD # env variable for GPG private key passphrase

      - name: Run tests
        run: |
          mvn clean jacoco:prepare-agent install jacoco:report javadoc:jar source:jar -P '!<extras-profile-id>'
          mvn site -Djacoco.skip -DskipTests -P '!<extras-profile-id>'

      - name: Setup sonarqube
        uses: warchant/setup-sonar-scanner@v8

      - name: Send to sonarqube
        env:
          # to get access to secrets.SONAR_TOKEN, provide GITHUB_TOKEN
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: sonar-scanner
          -Dsonar.login=${{ secrets.SONAR_TOKEN }}

      - name: Install Node
        uses: actions/setup-node@v4
        with:
          node-version: 24

      - name: Install Antora
        run: |
          cd docs
          npm i -D -E antora
          npm i @antora/lunr-extension
          npm i @sntke/antora-mermaid-extension
          npm i @djencks/asciidoctor-mathjax

      - name: Build Antora docs
        run: cd docs && npx antora antora-playbook.yml

      - name: Merge documentation
        run: |
          touch ./docs/build/site/.nojekyll
          cp -r ./docs/build/site ./doc
          cp -r ./target/site ./doc/mvn-site

      - name: Deploy to GitHub Pages
        if: success()
        uses: crazy-max/ghaction-github-pages@v5
        with:
          target_branch: gh-pages
          build_dir: ./doc
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Deploy to maven central
        env:
          OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
          SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
          SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
          SONATYPE_STAGING_PROFILE_ID: ${{ secrets.SONATYPE_STAGING_PROFILE_ID }}
        run: mvn deploy -P <sign-profile-id>,<extras-profile-id> --settings <settings-file> -DskipTests
```

### `main.yml` template

Identical pipeline; the only difference is the trigger — a GitHub Release being published instead of a push:

```yaml
name: Release

on:
  release:
    # Runs this workflow when a new GitHub release is created
    types: [released]

jobs:
  build:
    name: Build and execute tests
    runs-on: ubuntu-latest
    steps:
      # ... identical to every step under develop.yml's `build` job above, verbatim ...
```

Notes on placeholders that are genuinely project-specific and must come from Step 1, not from these templates:

- `<integration-branch>`: the branch `develop.yml` triggers on (e.g. `develop`).
- `<java-version>`: e.g. `17`.
- `<publishing-server-id>`: the id shared by the Central/Nexus plugin config and the settings file's `<server>`.
- `<sign-profile-id>` / `<extras-profile-id>`: the two profile ids found in Step 1.
- `<settings-file>`: the Maven settings XML path used for the deploy step (Step 6 creates it if absent).

## Step 4 — Fill the templates

Substitute every placeholder from Step 1's survey. If any required fact wasn't resolvable there (e.g. no signing
profile exists yet, or no Central/Nexus plugin is configured), don't invent a value — ask the user or note it as an
open gap in Step 8's report instead of silently guessing.

## Step 5 — Handle missing supporting files

Before writing the workflows, make sure the files they depend on exist:

- **`docs/antora.yml` / `docs/antora-playbook.yml` missing**: recommend running the `antora-setup` skill now; the
  Antora build step in the workflow will fail without them.
- **`sonar-project.properties` missing**: ask the user for `sonar.organization` (if using SonarCloud),
  `sonar.projectKey`, and `sonar.host.url` (default `https://sonarcloud.io` unless they run self-hosted
  SonarQube), then create it, e.g.:

  ```properties
  sonar.organization=<org>
  sonar.projectKey=<project-key>
  sonar.host.url=<sonar-host-url>
  sonar.sources=src/main/java
  sonar.language=java
  sonar.java.binaries=target/classes/**
  sonar.java.source=<java-version>
  ```

- **Maven settings file (`<settings-file>` from Step 3) missing**: create it, matching the `server-id` resolved in
  Step 1:

  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <settings>
    <servers>
      <server>
        <id><publishing-server-id></id>
        <username>${env.OSSRH_USERNAME}</username>
        <password>${env.OSSRH_PASSWORD}</password>
      </server>
    </servers>

    <profiles>
      <profile>
        <id>ossrh</id>
        <activation>
          <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
          <gpg.executable>gpg</gpg.executable>
          <gpg.keyname>${env.SIGNING_KEY_ID}</gpg.keyname>
          <gpg.passphrase>${env.SIGNING_PASSWORD}</gpg.passphrase>
        </properties>
      </profile>
    </profiles>

    <pluginGroups>
      <pluginGroup>org.sonatype.plugins</pluginGroup>
    </pluginGroups>
  </settings>
  ```

- **`.gitignore` missing an entry for `/docs/build/` and `/target/`**: add them if absent, so the build output the
  workflow generates never gets committed by accident.

## Step 6 — Write the workflow files

Write (or, per Step 2, carefully update) `.github/workflows/develop.yml` and `.github/workflows/main.yml` with the
filled-in templates from Step 4, plus any supporting files created in Step 5.

## Step 7 — List the required repository secrets

Report the secrets that must exist under the target repository's Settings → Secrets and variables → Actions —
this skill cannot create them itself:

| Secret | Purpose |
|---|---|
| `SONAR_TOKEN` | Auth token for the SonarQube/SonarCloud scan |
| `OSSRH_USERNAME` / `OSSRH_PASSWORD` | Maven Central (Sonatype) publishing credentials |
| `SIGNING_KEY` / `SIGNING_KEY_ID` / `SIGNING_PASSWORD` | GPG private key, its key id, and its passphrase, for artifact signing |
| `SONATYPE_STAGING_PROFILE_ID` | Only needed with the legacy `nexus-staging-maven-plugin`; if the target repo uses `central-publishing-maven-plugin` this secret is unused and can be left unset or removed from the deploy step |

`GITHUB_TOKEN` needs no setup — GitHub Actions provides it automatically.

## Step 8 — Report and warn

Summarize what happened: whether both workflows were created fresh, updated in place, or left untouched (Step 2's
stop path); which supporting files (`sonar-project.properties`, the settings XML) were created versus already
present; and any open gaps noted in Steps 1/4 (missing Central plugin, missing signing profile, missing Antora
setup, etc.).

Finish with an explicit warning: **the user must review the generated (or updated) workflow files before relying on
them.** Branch names, profile ids, the Java version, and the Maven Central publishing setup were inferred from this
repository's current state and may need correction — and because this pipeline handles signing keys and publishes
artifacts publicly, a bad assumption here has real consequences. Recommend a dry run (e.g. pushing to a throwaway
branch with the trigger temporarily adjusted, or manually triggering via `workflow_dispatch` if added) before
trusting it on the real `develop`/release flow.
