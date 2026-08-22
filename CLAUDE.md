# Claude Code Quality & Testing Instructions

## 90% Unit Test Code Coverage Per File
- Every source file must reach at least **90% coverage** across ALL parameters:
  - Instructions / Statements (>= 90%)
  - Lines (>= 90%)
  - Branches (>= 90%)
  - Methods / Functions (>= 90%)
  - Cyclomatic Complexity (>= 90%)
  - Class (>= 90%)
- Zero exclusions: All source files must be tested.
- Global unit test timeout is configured (10s per test).

## Commands to Run
- Fast check:
  ```powershell
  mvn test -Dtest=<ModifiedTestClass>
  ```
- Full mandatory verification:
  ```powershell
  mvn clean test jacoco:report
  mvn checkstyle:check
  ```
