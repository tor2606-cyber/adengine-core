# Contributing

## Before you start

- Every change starts from an issue. Comment on it before starting larger work.
- Contributions require agreeing to the [CLA](CLA.md) (one comment on your first pull request).

## Workflow

- **Branch:** `<task-id>-<short-name>`, for example `m2-06-decision-engine`.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/) with the area as scope,
  for example `feat(dec): rank candidates by tier and weight`.
- **Pull request:** one task per pull request; link the issue with `Closes #<n>`.
- **Areas:** `plat` platform/build, `dom` domain, `api` admin API, `ui` admin UI, `dec` decisioning,
  `cons` consent/privacy, `tag` JS tag and kits, `evt` events, `rep` reporting, `val` value model,
  `ops` infrastructure, `qa` testing/quality, `doc` docs.

## Definition of done

1. CI is green: build, unit and integration tests, ArchUnit, licence report, SBOM, vulnerability scan,
   denylist and tag size check.
2. Tests at the right level: domain logic has unit or property tests; adapters have Testcontainers tests;
   UI flows that touch consent or rendering have Playwright tests.
3. The OpenAPI spec (and AsyncAPI for events) is updated if an interface changed, and the generated
   TypeScript client builds.
4. No personal data at rest: no full IP address, raw TC string, or identifier derived from IP or user agent.
5. Every new table, event, cache key and metric carries `tenant_id`. New endpoints are covered by the
   tenant isolation suite.
6. If the change alters a decision, an ADR is added or updated in `docs/adr/`.

## Dependencies

Core modules may only use dependencies under permissive licences (Apache-2.0, MIT, BSD, EPL-2.0 or similar).
CI fails on anything else.
