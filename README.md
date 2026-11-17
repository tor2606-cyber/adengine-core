# Ad Engine core

A consent-aware ad server for websites in the EU.

> **Status: early development.** Nothing here is ready for production yet. Progress is tracked in the
> [milestones](../../milestones) and [issues](../../issues).

## What it does

- **Decides** which ad to show in each slot on a page, in a few milliseconds, from an in-memory snapshot of campaigns.
- **Respects consent first.** Every request is classified as personalised, contextual or no-ad before anything
  else happens. Without consent, nothing is read from or stored on the visitor's device.
- **Serves** house, direct and affiliate ads, and falls back to AdSense or a static house ad when it has nothing better
  or is unavailable.
- **Counts** impressions and clicks without storing full IP addresses, raw consent strings or fingerprints.
- **Reports** results side by side for consented and non-consented traffic.

Built for one operator running their own sites, with tenant isolation in place from day one so that other
publishers and advertisers can be added later.

## Tech stack

Java 25, Spring Boot 4, PostgreSQL 18, Valkey, Kafka, jOOQ, Flyway; React + TypeScript admin UI;
a dependency-free TypeScript tag (≤ 5 KB gzipped). One container image, deployed with Docker Compose.

## Documentation

- Requirements: `docs/spec/requirements.md`
- Architecture (arc42 + C4): `docs/architecture/architecture.md`
- Roadmap and tasks: `docs/roadmap.md`
- Architecture decision records: `docs/adr/`

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Contributions require agreeing to the [CLA](CLA.md).
Security issues: see [SECURITY.md](SECURITY.md).

## Licence

[GNU Affero General Public License v3.0](LICENSE). If you run a modified version as a network service,
you must offer its source code to the users of that service.
