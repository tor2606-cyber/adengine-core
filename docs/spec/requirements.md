Last updated: 2026-11-13

# Ad Engine — Software Requirements Specification

Structure loosely follows ISO/IEC/IEEE 29148 (requirements specification). Quality requirements use the ISO/IEC 25010 categories.
Every requirement has an ID, a priority (**M** must, **S** should, **C** could), the milestone that delivers it and the decision it comes from.
Architecture: [../architecture/architecture.md](../architecture/architecture.md). Decisions: [docs/adr/](../adr/) (ADR-01–35).

---

## 1. Introduction

### 1.1 Purpose
Ad Engine decides which ad to show in a slot on a web page, serves it, and counts what happened. It is privacy-first: if a visitor refuses tracking, it still serves contextual ads without storing anything on the visitor's device.

### 1.2 Scope

| In scope (v0, own sites + portfolio) | Out of scope now |
|---|---|
| House, affiliate and direct-sold campaigns on the founder's sites | Video (model is ready, ADR-25) |
| Google AdSense as fallback demand | Header bidding / Prebid Server (port ready, ADR-11) |
| Consent-aware serving incl. contextual-only mode | Self-serve advertiser marketplace |
| Display and native formats | Billing, invoicing, payouts |
| Reporting incl. consented vs non-consented | Public sign-up for external publishers (ADR-34 package) |
| Multi-tenant data model and isolation | Building a CMP |
| Admin UI + REST API | Third-party ID integrations (ID5, Utiq, …) |

### 1.3 Definitions

| Term | Meaning |
|---|---|
| Tenant | The party whose sites and campaigns are managed; the data-isolation boundary. Today: the founder (+ a demo tenant) |
| Site / Placement | A website / one ad slot on it |
| Flight | A scheduled, targeted part of a campaign with a goal and pacing |
| Decision | The engine's answer for one slot in one page view |
| Consent mode | `PERSONALISED`, `CONTEXTUAL` or `NO_AD`, derived from the consent signal |
| EV | Expected value of showing a candidate once, in micro-units of currency |
| CMP | Consent management platform (the cookie banner) |
| TC string | The IAB TCF consent string produced by the CMP |
| Snapshot | Immutable in-memory copy of all serving data that the decision core reads |

---

## 2. Overall description

### 2.1 Actors and roles

| Actor | Role in the system | Main goals |
|---|---|---|
| Visitor | Anonymous end user | Reads pages; ads must not slow the page or track without consent |
| Owner | `OWNER` | Everything inside a tenant, incl. users and policies |
| Trafficker | `TRAFFICKER` | Sites, placements, campaigns, creatives |
| Analyst | `ANALYST` | Reports only |
| Advertiser | `ADVERTISER_VIEWER` or signed report link | Sees only their own campaign report |
| Operator | Server access (founder) | Deploy, monitor, back up, restore |

### 2.2 Constraints

| ID | Constraint | Source |
|---|---|---|
| C-01 | Only EU/CH-owned providers for anything that receives visitor request data (compute, DB, storage, CDN, DNS, logs, backups) | ADR-05 |
| C-02 | Core repo licence AGPL-3.0; only permissive-licensed libraries inside the core; GPL tools only as separate processes | ADR-01 |
| C-03 | Java 25 + Spring Boot 4.x; vanilla PostgreSQL, Valkey, Kafka, S3 API; no provider-specific managed features | ADR-04, ADR-18 |
| C-04 | One maintainer: operations ≤ ~2 h/month in steady state | ADR-14, ADR-26 |
| C-05 | Infrastructure ≤ €60–90/month at launch (Estimate) | ADR-26 |
| C-06 | German and EU law: TDDDG §25, GDPR, DDG/MStV/UWG labelling, DSA, TTPA, GlüStV | Regulation research (not published) |

### 2.3 Assumptions

| ID | Assumption | If wrong |
|---|---|---|
| A-01 | Traffic: hundreds of PV/day at launch, 10k–100k PV/day later | Above ~1M PV/day revisit ADR-16 scaling |
| A-02 | Sites on WordPress, Next.js/React or React + Java backend | Plain tag still works (FR-DEL-01) |
| A-03 | One Google-certified TCF CMP across all sites (ADR-33) | Consent source config per site (FR-PRV-01) |
| A-04 | Demand at launch: house, affiliate, AdSense | `DemandSourcePort` takes more later |

---

## 3. Functional requirements

### 3.1 Tenancy and access (TEN)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| TEN-01 | Every stored record, event, counter key and log line that refers to business data carries `tenant_id` | M | M1 | ADR-02 |
| TEN-02 | The tenant is resolved from the request host (site domain or CNAME) → site → tenant. Public requests never carry a tenant ID | M | M2 | ADR-02, ADR-34 |
| TEN-03 | Users have memberships in tenants with one role each: `OWNER`, `TRAFFICKER`, `ANALYST`, `ADVERTISER_VIEWER` (scoped to one advertiser) | M | M1 | ADR-34 G2 |
| TEN-04 | Login with password + TOTP, session timeout, lockout after repeated failures | M | M1 | ADR-34 G2 |
| TEN-05 | No API, report, export, counter, learned value, explain or replay result ever includes another tenant's data. Proven by a CI suite with two tenants | M | M1 | ADR-34 G1 |
| TEN-06 | Export and delete all data of a tenant | S | M3 | ADR-34 G7 |
| TEN-07 | Usage per tenant (decisions, impressions, stored bytes) available as metrics | S | M3 | ADR-34 G8 |

### 3.2 Inventory (INV)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| INV-01 | **Site**: domains, delivery route (`PROXY` / `CNAME`), locale, IANA time zone, label text, declared categories, minor-context flag, consent source, refusal policy, AdSense publisher ID | M | M1 | ADR-20, 23, 30 |
| INV-02 | **Placement**: site, name, allowed formats, sizes, format constraints, above/below fold, lazy flag, fallback chain, AdSense ad-unit ID (one per placement), floor price | M | M1 | ADR-22, 23, 25 |
| INV-03 | `ads.txt` generated per site from tenant data | M | M2 | ADR-23, 34 G3 |
| INV-04 | URL rules per site (path prefix → categories, keywords, minor-context) | M | M2 | ADR-09 |

### 3.3 Campaigns and creatives (CAM)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| CAM-01 | **Advertiser**: name, legal identity ("on whose behalf"), payer, regulated category, political declaration (default: refused), status | M | M1 | ADR-13 |
| CAM-02 | **Campaign**: advertiser, type `HOUSE` / `AFFILIATE` / `DIRECT`, status | M | M1 | ADR-22 |
| CAM-03 | **Flight**: start/end (in each site's time zone), priority tier `GUARANTEED` / `SPONSORSHIP` / `STANDARD` / `HOUSE`, goal type `NONE` / `IMPRESSIONS` / `CLICKS` / `BUDGET` / `SHARE_OF_VOICE` + goal value, pacing `EVEN` / `ASAP`, pricing `CPM` / `CPC` / `FLAT` / `NONE` + price (micros + ISO currency), value per click, strategic weight, targeting (sites, placements, countries, device classes, daypart, categories, keywords), frequency cap, `requires_personalisation` | M | M1 | ADR-15, 22, 24 |
| CAM-04 | **Creative**: belongs to an advertiser; asset type `DISPLAY` (image) / `NATIVE` (title, text, image, call to action) / `THIRD_PARTY_TAG`; `VIDEO` reserved; landing URL; category; approval status | M | M1 | ADR-12, 25 |
| CAM-05 | Creatives are assigned to flights with a rotation weight | M | M1 | — |
| CAM-06 | Upload linter (size, format, HTTPS only, no auto-redirect, allow-listed trackers) and malware scan before approval | M | M1–M2 | ADR-13 |
| CAM-07 | Preview of a creative exactly as served, incl. label | S | M1 | — |
| CAM-08 | Sponsorship booking screen (book a placement for a period at a share of voice) | C | when first sponsor | ADR-34 |

### 3.4 Decisioning (DEC)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| DEC-01 | `POST /v1/decisions` accepts several slots of one page view and returns one result per slot (contract in §5.1) | M | M2 | ADR-10 |
| DEC-02 | **Step 1 is consent**: the consent mode is determined before anything else, and it limits all later steps | M | M2 | ADR-07 |
| DEC-03 | Context (categories, keywords, language, brand-safety flags) comes from a cache; a miss never blocks, it uses site/URL rules and queues the page | M | M2 | ADR-09 |
| DEC-04 | Candidates come from an in-memory index; no database query on the request path | M | M2 | ADR-16, 21 |
| DEC-05 | Policy filters: creative approved; category blocklist for the country; minor context; political refusal; `requires_personalisation` only in `PERSONALISED`; format and size fit | M | M2 | ADR-13 |
| DEC-06 | Eligibility: flight dates, goal/budget left, pacing allows, frequency cap (per-user only with consent, otherwise per page view), max one ad per advertiser per page view | M | M3 | ADR-08, 24 |
| DEC-07 | Ranking: highest tier first; inside a tier, highest EV × strategic weight; AdSense fallback competes with its estimated EV | M | M3 | ADR-22 |
| DEC-08 | Response markup is rendered by the server with a non-removable, locale-specific label, `rel="sponsored noopener"` links and a signed click URL | M | M2 | ADR-13, 23 |
| DEC-09 | Explain mode returns every candidate with `SELECTED` or a rejection reason; only for logged-in tenant users | M | M2 | ADR-21 |
| DEC-10 | A configurable sample of decisions is logged with inputs (no personal data) and snapshot version for replay | S | M4 | ADR-21 |
| DEC-11 | Admin changes reach serving within 5 s | M | M2 | ADR-21 |
| DEC-12 | Given the same snapshot, request, counters, clock and random seed, the decision is identical | M | M2 | ADR-21 |

### 3.5 Delivery (DEL)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| DEL-01 | Plain JS tag works on any HTML page: fills `<div data-ae-slot>` elements | M | M2 | ADR-31 |
| DEL-02 | The tag stores nothing and reads no device data until the CMP signals consent; with no signal after a timeout it requests ads as "no consent" | M | M2 | ADR-07 |
| DEL-03 | Tag API: `refresh(slotIds?)`, `newPageView()`, `setConsent(...)` for single-page apps and non-TCF CMPs | M | M2 | ADR-31 |
| DEL-04 | Two delivery routes: same-origin path `/_ae/` via the site's proxy, and `ads.<domain>` CNAME with automatic TLS | M | M2 | ADR-23, 34 G6 |
| DEL-05 | Fallback chain when the engine times out (800 ms), errors or has no ad: AdSense (only if Google is consented) → static house creative from tag config → collapse slot | M | M2 | ADR-23 |
| DEL-06 | AdSense code is inserted unmodified into the slot, never inside an iframe of ours | M | M2 | ADR-23 |
| DEL-07 | Third-party tag creatives render in a sandboxed iframe | M | M2 | ADR-12 |
| DEL-08 | Below-the-fold slots fill lazily | S | M3 | ADR-23 |
| DEL-09 | Integration kit for the first live site's stack: `@adengine/react` (`<AdSlot/>`) **or** WordPress plugin (block, shortcode, settings) | M | M3 | ADR-31 |
| DEL-10 | Server-side insertion endpoint for uncached pages | C | later | ADR-23 |

### 3.6 Consent and privacy (PRV)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| PRV-01 | Consent sources per site: `TCF_PUBLISHER_PURPOSES`, `CMP_CUSTOM_VENDOR`, `TCF_VENDOR`, `NOT_APPLICABLE`; all implemented and tested with golden TC strings | M | M2 | ADR-20, 34 G5 |
| PRV-02 | Refusal policy per site: `CONTEXTUAL_ON_REFUSAL` or `NO_AD_ON_REFUSAL` | M | M2 | ADR-07 |
| PRV-03 | Country policy table: GDPR applies, refusal policy, label text, category blocks per country | M | M2 | ADR-20, 30 |
| PRV-04 | Full IP address used in memory only (geo, bot check), then dropped; only /24 (IPv4) or /48 (IPv6) stored, max 30 days | M | M2 | ADR-06 |
| PRV-05 | No identifier, hash or dedup key derived from IP or user agent | M | M2 | ADR-06 |
| PRV-06 | Raw TC strings are not stored (optional debug log ≤ 7 days, off in production); decoded flags + consent mode + source are stored | M | M2 | ADR-06 |
| PRV-07 | Per-user frequency ID only with consent: random, per site, first-party, lifetime ≤ longest cap window and ≤ 13 months | M | M3 | ADR-08 |
| PRV-08 | Retention by dropping daily partitions: raw events 90 d, truncated IP/raw UA 30 d, aggregates 10 y, audit log 6 y, ops logs 14 d. Tenants can shorten, never extend | M | M3 | ADR-06 |
| PRV-09 | Export / delete everything linked to a frequency ID | S | M5 | ADR-06 |
| PRV-10 | Report cells with fewer than 10 events are suppressed where they could single out a person | M | M3 | ADR-06 |

### 3.7 Events and counting (EVT)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| EVT-01 | An impression is counted on the server when the ad is delivered (reported as "delivered", not "viewable") | M | M3 | ADR-10 |
| EVT-02 | Clicks go through `/_ae/c/{token}`: token is HMAC-signed, expires, holds no user ID; redirect appends campaign tracking parameters | M | M3 | ADR-23 |
| EVT-03 | Events follow the ADR-14 schema + `event_name`, `ev_micros`, `ev_source`, `experiment_arm`, `consent_source`, `schema_version` | M | M3 | ADR-14, 22, 25 |
| EVT-04 | At-least-once delivery, dedup by UUIDv7 `event_id` | M | M3 | ADR-14 |
| EVT-05 | Known bots and data-centre traffic are flagged and excluded from billable counts | M | M3 | ADR-10 |
| EVT-06 | Pacing and goal counters updated within 10 s of the event | M | M3 | ADR-16 |
| EVT-07 | Conversion intake: Digistore24 IPN webhook, Amazon PartnerNet report import; aggregated per campaign and day | S | M4 | ADR-22 |

### 3.8 Reporting (REP)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| REP-01 | Dimensions: date, site, placement, advertiser, campaign, flight, creative, country, device class, consent mode, demand source. Metrics: requests, decisions, fill rate, impressions, clicks, CTR, EV, estimated revenue, conversions | M | M3 | ADR-14 |
| REP-02 | Side-by-side consented vs non-consented view (revenue per 1,000 PV) | M | M3 | ADR-22 |
| REP-03 | Advertiser report: signed, expiring read-only link + CSV export | M | M3 | ADR-15, 34 |
| REP-04 | Delivery view per flight: goal, delivered, pacing curve, forecast to end | S | M3 | ADR-24 |
| REP-05 | Lost-fill view: rejection reasons counted per placement | S | M3b | ADR-27 |
| REP-06 | Event writes go through `EventStorePort` and report reads through `ReportQueryPort`; pre-summed tables hold additive values only | M | M3 | ADR-35 |
| REP-07 | ClickHouse adapter for events and reports, runnable in the demo profile | C | M5 | ADR-35 |

### 3.9 Value model (VAL)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| VAL-01 | Manual inputs: value per click per house/affiliate flight; AdSense revenue per 1,000 impressions per site | M | M3 | ADR-32 |
| VAL-02 | Daily import of AdSense earnings per ad unit (Management API) | S | M4 | ADR-22 |
| VAL-03 | Value per click updated from conversion data | S | M4 | ADR-22 |
| VAL-04 | Learning stages switch on by data thresholds; learned values are tenant-scoped | S | M4 | ADR-32, 34 |
| VAL-05 | Holdout / experiments switch on at ≥ ~5k PV/day per site | C | M4 | ADR-32 |

### 3.10 Compliance (CPL)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| CPL-01 | Every ad, house ads included, shows the label ("Anzeige" etc.) rendered by the server | M | M2 | ADR-13 |
| CPL-02 | The advertiser's identity is reachable from the ad (info link) | M | M2 | ADR-13 |
| CPL-03 | German regulated-category blocklist; gambling blocked | M | M2 | ADR-13 |
| CPL-04 | Political ads refused by default; "suspected political" flag for review | M | M1 | ADR-13 |
| CPL-05 | Minor-context sites/sections exclude alcohol, gambling, adult, dating; no personalisation | M | M2 | ADR-13 |
| CPL-06 | Append-only audit log: who changed what, approvals, policy decisions | M | M1 | ADR-13 |

### 3.11 Administration (ADM)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| ADM-01 | Versioned REST API `/v1/...`, OpenAPI 3.1 spec, errors as RFC 9457 problem details | M | M1 | ADR-34 G4 |
| ADM-02 | React admin SPA (TypeScript, Mantine), served same-origin by the admin role: sites/placements, advertisers/campaigns/flights/creatives, reports, explain, audit log, users, settings; German and English | M | M1–M3b | ADR-29 |
| ADM-03 | The admin UI uses only the public REST API through a client generated from the OpenAPI spec | M | M1 | ADR-34 G4 |

### 3.12 Operations (OPS)

| ID | Requirement | Prio | Milestone | Source |
|---|---|---|---|---|
| OPS-01 | Infrastructure as code (OpenTofu) + Docker Compose; deploy is one command | M | M3 | ADR-26 |
| OPS-02 | Continuous WAL backup to a second provider; automated weekly restore drill with alert | M | M3 | ADR-26 |
| OPS-03 | Dashboards and alerts versioned in the repo | M | M3 | ADR-27 |
| OPS-04 | External uptime check of the decision endpoint | M | M3 | ADR-26 |
| OPS-05 | Demo profile: synthetic tenant + traffic generator, live dashboard in ≤ 5 min | M | M5 | ADR-28 |

---

## 4. Quality requirements (ISO/IEC 25010)

| ID | Category | Requirement | Target | Verified by |
|---|---|---|---|---|
| NFR-01 | Performance | Server decision latency, house/direct | p50 ≤ 3 ms, p99 ≤ 10 ms at 300 rps on 4 vCPU | Load test |
| NFR-02 | Performance | Decision core cost per call | ≤ 50 µs with 1,000 flights (to confirm) | JMH |
| NFR-03 | Performance | Admin change → serving | ≤ 5 s | Integration test |
| NFR-04 | Usability (visitor) | Page impact | Tag ≤ 5 KB gzip, 0 CLS from slots, no render-blocking request | Lighthouse CI |
| NFR-05 | Reliability | Fail-open | Fallback ≤ 800 ms after timeout; layout never breaks | Playwright, ad server stopped |
| NFR-06 | Security / privacy | Consent | No storage or device read before consent signal | Playwright |
| NFR-07 | Security / privacy | Personal data at rest | No full IP or TC string stored; raw UA only in 30-day table | Privacy guard test |
| NFR-08 | Reliability | Event integrity | Loss ≤ 0.1% vs decisions | Reconciliation job |
| NFR-09 | Functional suitability | Delivery accuracy | Daily overspend ≤ 2%; share of voice ± 2 pp | Simulator |
| NFR-10 | Functional suitability | Reconciliation | Impressions within ±10% of AdSense and site logs | Monthly report |
| NFR-11 | Reliability | Availability | 99.5%/month decision endpoint | Uptime monitor |
| NFR-12 | Reliability | Recovery | RPO ≤ 15 min, RTO ≤ 4 h | Restore drill |
| NFR-13 | Security | Admin exposure | Not reachable from the public internet (until ADR-34 package) | External port scan |
| NFR-14 | Security | Web security | OWASP ASVS level 1 on public endpoints, level 2 on admin | Checklist + ZAP baseline scan in CI |
| NFR-15 | Security | Supply chain | Permissive licences in core; SBOM per release; critical CVEs fixed ≤ 7 days | CI |
| NFR-16 | Maintainability | Architecture | Domain module has no framework dependencies; no adapter-to-adapter imports | ArchUnit |
| NFR-17 | Maintainability | Test strength | Domain: line coverage ≥ 90%, mutation score ≥ 75% | JaCoCo, PIT |
| NFR-18 | Portability | Runs anywhere | Same images on any Docker host; no provider-specific service | Demo profile on laptop |
| NFR-19 | Compatibility | Single-page apps | Route change → new page view, fresh slots | Playwright React route |
| NFR-20 | Cost | Infrastructure | ≤ €60–90/month at launch | Invoice |
| NFR-21 | Repo hygiene | No own-site data in public repo | 0 hits on denylist | CI |

---

## 5. External interfaces

### 5.1 Decision API (public)

`POST /_ae/v1/decisions` (same-origin) or `POST https://ads.<domain>/v1/decisions` (CNAME). Tenant and site come from the host.

```json
{
  "pageView": { "id": null, "url": "https://example.de/ratgeber/garten", "referrer": "https://www.google.com/" },
  "slots": [ { "slotId": "sidebar-1", "placementId": "plc_7Hk2", "sizes": [[300, 250]] } ],
  "consent": { "tcString": "CQ…", "gdprApplies": true, "customVendor": null },
  "debug": { "explain": false }
}
```

```json
{
  "pageViewId": "pv_0193b5a2…",
  "snapshotVersion": 4182,
  "consentMode": "CONTEXTUAL",
  "results": [
    { "slotId": "sidebar-1", "kind": "markup", "width": 300, "height": 250,
      "html": "<div class=\"ae-ad\"><span class=\"ae-label\">Anzeige</span><a href=\"/_ae/c/eyJ…\" rel=\"sponsored noopener\">…</a></div>" },
    { "slotId": "content-2", "kind": "fallback", "fallback": { "type": "ADSENSE", "client": "ca-pub-…", "slot": "123" } },
    { "slotId": "footer-1", "kind": "none" }
  ]
}
```

- `pageView.id` is `null` on the first call; the server issues one and the tag keeps it **in memory only** for later calls on the same page (lazy slots).
- `kind` is `markup` | `fallback` | `none`; `vast` is reserved for video (ADR-25).
- Client hints the browser sends unasked (`Sec-CH-UA-*`) and the user agent are read from headers; the tag adds nothing about the device.

### 5.2 Other interfaces

| Interface | Direction | Contract |
|---|---|---|
| Tag script `/_ae/tag.js` | Engine → browser | ≤ 5 KB, versioned, long-cache with hash |
| Tag JS API `window.adengine` | Site → tag | `refresh`, `newPageView`, `setConsent` |
| Click redirect `GET /_ae/c/{token}` | Browser → engine | 302 to landing URL; 410 if expired or invalid |
| Admin API `/v1/...` | Admin users → engine | OpenAPI 3.1, RFC 9457 errors, session + TOTP |
| Events topic `ae.events.v1` | Engine → consumers | JSON, `schema_version`, ADR-14 columns; documented with AsyncAPI |
| Conversion webhook `POST /hooks/digistore24` | Digistore24 → engine | IPN with shared secret |
| AdSense Management API | Engine → Google | OAuth; daily earnings per ad unit |
| Geo database | File | DB-IP Lite (CC BY 4.0), monthly refresh |
| Demand port (later) | Engine → Prebid Server | OpenRTB 2.6, 300 ms timeout |

---

## 6. Acceptance per milestone

| Milestone | Accepted when |
|---|---|
| M0 | CI green: build, ArchUnit, licence report, SBOM, denylist; `docker compose up` brings up Postgres, Valkey, Kafka and the app health check |
| M1 | All CAM/INV/TEN-01–05/ADM-01 requirements pass API tests; two-tenant isolation suite green; audit log records every change |
| M2 | Demo page serves house and direct ads with a label; consent branches on real TC strings from the chosen CMP; explain works; fallback and no-storage-before-consent Playwright tests green |
| M3 | Live on the first own site with counting, pacing and reports by consent mode; restore drill green; 30-day availability clock starts |
| M3b | Full admin UI (reports pivot, explain, users, settings), advertiser report link, site kit, NFR-01 load test met |
| M4 | EV ranking with imported values; simulator proves NFR-09 |
| M5 | Demo profile, README with diagrams, JMH/PIT/Lighthouse numbers published; portfolio done |
