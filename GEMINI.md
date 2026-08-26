# Quality Check Configuration & Assistant Instructions

This document defines the mandatory Quality Check protocol for AI coding agents operating on this workspace (including Antigravity, Kiro, GitHub Copilot, Claude, Cursor, OpenAI Codex, and others).

---

## Mandatory Post-Change Quality Check Protocol

After performing code modifications (such as implementing features, bug fixes, refactoring, or updating configurations), AI coding agents **MUST** execute the appropriate quality check commands before declaring a task completed.

---

## Mandatory 90% Per-File Unit Test Code Coverage Benchmark

- **Strict 90% Unit Test Code Coverage Each File Wise**: Every single source file in the application MUST achieve at least **90% coverage across ALL parameters**:
  1. **Instructions / Statements** (minimum 90%)
  2. **Lines** (minimum 90%)
  3. **Branches** (minimum 90%)
  4. **Methods / Functions** (minimum 90%)
  5. **Cyclomatic Complexity** (minimum 90%)
  6. **Class** (minimum 90%)
- **Zero Exemptions**: Do NOT skip or exclude any source file. All classes must satisfy the 90% threshold.
- **Enforcement on Failure**: JaCoCo is configured with `<haltOnFailure>true</haltOnFailure>`. Any file falling below 90% on any parameter will immediately fail the build.
- **Global Test Timeout**: All unit tests must execute under the configured global timeout (`junit-platform.properties` 10s default per test and Maven Surefire process timeout).
- **Mandatory Verification on Every Change**: Whenever making ANY code change, AI agents MUST execute the unit test coverage suite and ensure 0 coverage violations and no regression below 90%.

---

## 1. Execution Priority Order

Quality checks **MUST** be executed in the following order:

### Priority 1: Build Verification & Unit Test Code Coverage (REQUIRED FIRST)
1. **Compilation / Build Check**: Verify that the code compiles without errors or warnings (`mvn test-compile`).
2. **Unit Test Suite & Code Coverage**: Run the full unit test suite and generate JaCoCo coverage reports (`mvn clean test jacoco:report`). Verify that the 90% per-file threshold is achieved across all parameters without skipping any file.

### Priority 2: Typechecking & Static Analysis
1. **Type Safety**: Execute strict typechecking (e.g. `javac`, `tsc --noEmit`, `mypy`, `go vet`).
2. **Static Bug Detection**: Run static analysis tools (e.g. `SpotBugs`, `PMD`, `Clippy`).

### Priority 3: Linting & Code Formatting
1. Validate code style and linting rules (e.g. `Checkstyle`, `ESLint`, `Flake8`, `Prettier`).
   ```powershell
   mvn checkstyle:check
   ```

### Priority 4: Pending Database Schema Migrations
1. Check for and apply any pending database schema migrations (e.g. `Flyway`, `Liquibase`, `Prisma`, `Alembic`, `Rails`).

---

## 2. Multi-Project & Global Workspace Scope

When working in a workspace with multiple projects, submodules, or microservices:
- AI agents **MUST NOT** only run checks in a single sub-directory if root or global commands exist.
- Use **global workspace commands** from the root to run build, test coverage, typecheck, and lint across **ALL** projects in the workspace.

---

## 3. Command Matrix by Technology Stack

### Java / Maven (Current Workspace Standard)
- **Fast Incremental Check (Dirty / Modified Files Only)**:
  ```powershell
  # Fast compilation check
  mvn test-compile
  
  # Fast test execution for modified test files only
  mvn test -Dtest=EmailProcessorServiceTest,RFQBuilderServiceTest
  ```
- **Step 1: Build & Unit Test Coverage with Strict 90% Per-File Enforcement (FIRST)**:
  ```powershell
  mvn clean test jacoco:report
  ```
- **Step 2 & 3: Typecheck, Static Analysis & Linting**:
  ```powershell
  mvn checkstyle:check
  ```
- **Step 4: Database Schema Migrations**:
  ```powershell
  mvn flyway:migrate
  ```

### JavaScript / TypeScript / Node.js
- **Fast Incremental Check**:
  ```bash
  npx jest --onlyChanged
  npm run typecheck -- --incremental
  ```
- **Step 1: Build & Unit Test Coverage (FIRST)**:
  ```bash
  npm run build
  npm test -- --coverage
  ```
- **Step 2 & 3: Typecheck & Linting**:
  ```bash
  npm run typecheck # tsc --noEmit
  npm run lint      # eslint .
  ```
- **Step 4: Database Schema Migrations**:
  ```bash
  npx prisma migrate dev # or npx typeorm migration:run
  ```

### Python
- **Fast Incremental Check**:
  ```bash
  pytest --picked
  flake8 $(git diff --name-only HEAD | grep '\.py$')
  ```
- **Step 1: Build & Unit Test Coverage (FIRST)**:
  ```bash
  pytest --cov=. --cov-report=term-missing
  ```
- **Step 2 & 3: Typecheck & Linting**:
  ```bash
  mypy .
  flake8 .
  ```
- **Step 4: Database Schema Migrations**:
  ```bash
  alembic upgrade head
  ```

---

## 4. Fast Check Mode for Iterative Development

During rapid iterative development (before running the complete workspace validation suite):
- Run the **Fast Incremental Check** command targeting modified files to quickly catch compilation errors or broken tests.
- **Rule**: Prior to declaring full task completion to the user, the full **Step 1 (Build & Unit Test Coverage - 90% per-file benchmark)** through **Step 4** check commands MUST be executed.
