# Order and Payment v2/v3 Implementation Plan

Goal: Publish two traceable revisions of both services, with method-body changes in v2 and Java method-signature changes in v3, then deliver four service TARs to Nexus.

Architecture: Retain HTTP routes and Boolean payment responses. Change service implementations and their in-module callers, keeping mapper SQL contracts intact. Build the full Maven reactor for each revision and archive the executable JARs before changing to the next revision.

Tech stack: Java 17, Maven, JUnit 4.13.2, Nexus Maven hosted component upload.

Spec: User request in this chat, 2026-11-02.

## Constraints
- Work in the requested clean checkout at C:/m4c/mall4cloud.
- Create two commits on main and push to the existing origin without force.
- Each commit changes both order and payment services.
- v2 keeps Java service signatures; v3 changes those signatures and all production callers.
- Preserve existing HTTP contracts, database schema and all existing Nexus assets.
- Publish group Mall4Cloud, artifacts mall4cloud-order and mall4cloud-payment, versions v2 and v3.
- Each TAR contains only service/mall4cloud/target/service.jar and its directories.
- Do not download artifacts for verification; compare local hashes to Nexus asset metadata and inspect Browse indexes.

## Design
- v2 order: normalize positive, distinct order IDs and short-circuit empty queries before the SQL IN clause.
- v2 payment: short-circuit empty IDs/invalid users; trim surrounding whitespace; interpret a positive SQL COUNT as paid; remove an unused status query from the controller.
- v3 order: replace getSpuNameListByOrderIds(long[]) with getSpuNameListByOrderIds(List<Long>), support null elements, and migrate the controller.
- v3 payment: replace Integer isPay(String, Long) with boolean isPay(Long, String), preserve v2 behavior and migrate the controller.
- Test only database boundaries with lightweight mapper doubles; instantiate real service/controller implementations.

## Tasks
- [x] Add meaningful v2 regression tests and observe both modules failing for the intended reasons.
- [x] Implement v2 method bodies, run the complete reactor clean verify, review the diff and commit/push.
- [x] Archive v2 executable JARs with commit/hash records and start new-path Nexus uploads.
- [x] Add failing v3 collection/signature behavior tests, implement/migrate interfaces and callers.
- [x] Run the complete reactor clean verify, review the diff and commit/push v3.
- [ ] Archive/upload v3 JARs and verify all four Nexus sizes, SHA-256 values, generated POMs and Browse directories.
- [ ] Deliver commit links, exact method differences, test/build results and per-version download URLs.

## Review focus
- Null/empty/all-invalid order IDs must not produce an empty SQL IN clause.
- Duplicate order IDs must preserve first-seen query order without repeated IDs.
- SQL COUNT greater than one must still produce a paid response.
- Missing payment rows, blank order IDs or invalid users must produce an unpaid response without an unnecessary query.
- v3 must migrate every Java caller while preserving HTTP endpoint paths and response types.
- v2 JARs must be archived before the v3 build overwrites target outputs.
