# Quality Check Configuration & Assistant Instructions

This document defines the mandatory Quality Check protocol for AI coding agents operating on this workspace (including Antigravity, Kiro, GitHub Copilot, Claude, Cursor, and others).

---

## Mandatory Post-Change Quality Check Protocol

After performing code modifications (such as implementing features, bug fixes, refactoring, or updating configurations), AI coding agents **MUST** execute the appropriate quality check commands before declaring a task completed.

---

## 1. Execution Priority Order

Quality checks **MUST** be executed in the following order:

### Priority 1: Build Verification & Unit Test Code Coverage (REQUIRED FIRST)
1. **Compilation / Build Check**: Verify that the code compiles without errors or warnings.
2. **Unit Test Suite & Code Coverage**: Run unit tests and generate coverage reports (JaCoCo, Jest coverage, Pytest cov, etc.). Verify that test coverage thresholds are maintained and no regressions were introduced.

### Priority 2: Typechecking & Static Analysis
1. **Type Safety**: Execute strict typechecking (e.g. `javac`, `tsc --noEmit`, `mypy`, `go vet`).
2. **Static Bug Detection**: Run static analysis tools (e.g. `SpotBugs`, `PMD`, `Clippy`).

### Priority 3: Linting & Code Formatting
1. Validate code style and linting rules (e.g. `Checkstyle`, `ESLint`, `Flake8`, `Prettier`).

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
  mvn test-compile -Dtest=EmailProcessorServiceTest,RFQBuilderServiceTest
  ```
- **Step 1: Build & Unit Test Coverage (FIRST)**:
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
- **Rule**: Prior to declaring full task completion to the user, the full **Step 1 (Build & Unit Test Coverage)** through **Step 4** check commands MUST be executed.
