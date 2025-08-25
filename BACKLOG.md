

# new-bucika-gsr — Implementation Backlog (v1)

**Scope**
PC orchestrator controlling multiple Android clients. One Android is the **GSR leader** (Bluetooth to Shimmer). Default **Local** mode (store‑and‑forward); optional **Bridged** mode (live GSR to PC). Tight clock sync, robust ingest, session metadata, calibration.

**Priority legend**: P0 = Must for first working release; P1 = Nice‑to‑have for thesis completeness; P2 = Later.

**Traceability**: FR‑L0, FR1–FR11; NFR‑Perf/Time/Rel/Int/Sec/Use/Scale/Maint as per chapter.

---

## Milestones (execution order)

1. **M0 Foundations (P0)** → 2. **M1 Protocol & Time** → 3. **M2 Android Recording + GSR (Local)**
2. **M3 Session Manager + PC UI** → 5. **M4 Ingest/Offload** → 6. **M5 Reliability + Tests**
3. **M6 Security + Calibration (P1)** → 8. **M7 Observability + Docs**

---

## Epics and Work Items

### E0. Foundations & Repo Hygiene — **P0** (FR4, NFR‑Maint)

* [ ] Monorepo layout: `pc/`, `android/`, `shared-spec/`, `docs/`, `tools/`.
* [ ] Code style & hooks (ktlint/detekt; ruff/black; pre‑commit).
* [ ] CI workflows: build APK; build PC app; unit tests.
  **DoD:** Clean CI builds for PC+Android; style checks pass; version/tag recorded in app “About”.

---

### E1. Protocol & Discovery — **P0** (FR1, FR2, FR6, FR7)

* [ ] **Control channel**: JSON over WebSocket (WS); envelope `{id,type,ts,sessionId,deviceId,payload}`.
* [ ] **Messages v1**: `HELLO/REGISTER`, `PING/PONG`, `START`, `STOP`, `SYNC_MARK`, `ACK`, `ERROR`.
* [ ] **Discovery**: mDNS/UDP beacon + manual IP fallback.
* [ ] **Liveness**: 3 missed PING ⇒ offline; retries with back‑off.
  **DoD:** Two phones auto‑register; PC broadcasts `START/STOP/SYNC_MARK`; all commands ACKed; offline detection ≤10 s.

**Deps:** E0.

---

### E2. Time Synchronisation — **P0** (FR3, FR11, NFR‑Time)

* [ ] **PC SNTP‑like service** (local reference clock).
* [ ] **Android client**: offset + uncertainty; sync pre‑start and periodic during recording.
* [ ] **Local mode stamping**: leader records offset at start/stop and every 30 s.
  **DoD:** Median offset ≤5 ms; p95 ≤15 ms over 30 min; offsets written to CSV/sidecar.

**Deps:** E1.

---

### E3. Android Recording Service — **P0** (FR2, FR5, FR8)

* [ ] ForegroundService with notification; survives background/lock.
* [ ] **RGB**: CameraX 1080p\@30; constant bitrate; rotation fix.
* [ ] **Thermal** (if available): vendor wrapper; clean enable/disable.
* [ ] Local writers: MP4 segmenting (\~1 GB); crash‑safe flush.
  **DoD:** `START` → files created; `STOP` → playable MP4; no corruption; frame rate ≥29 fps avg.

**Deps:** E1, E2.

---

### E4. Android GSR Leader (Local mode) — **P0** (FR1, FR5, FR11)

* [ ] Bluetooth Classic to Shimmer; 32–128 Hz; channel config (GSR, temp).
* [ ] **CSV schema**: `t_mono_ns,t_utc_ns,offset_ms,seq,gsr_raw_uS,gsr_filt_uS,temp_C,flag_spike,flag_sat,flag_dropout`.
* [ ] Filters + QC flags (spike/saturation/dropout); deterministic simulator toggle.
  **DoD:** 30‑min run @128 Hz with <0.1% missing samples; CSV matches schema; simulator output is seed‑stable.

**Deps:** E2.

---

### E5. Session Manager (PC) — **P0** (FR4, FR6)

* [ ] Lifecycle: NEW → ARMED → RECORDING → FINALISING → DONE/FAILED.
* [ ] Session folder + `meta.json` (devices, versions, offsets, events, files manifest).
* [ ] Pre‑flight checks: disk space; device online; permissions; battery hints.
  **DoD:** Starting a session creates folder + metadata; state transitions idempotent; pre‑flight blocks unsafe start.

**Deps:** E1, E2.

---

### E6. Data Packaging & Offload — **P0** (FR10, FR5)

* [ ] Android **packager**: `manifest.json` + SHA‑256 per file; ZIP or raw + chunking.
* [ ] **Resumable upload**: `UPLOAD_BEGIN/CHUNK/END` with offsets; retry/back‑off.
* [ ] PC **ingest**: write to session, verify checksums, update `meta.json`.
  **DoD:** Interrupted upload resumes; all files present with verified hashes; manifest stored.

**Deps:** E1, E5.

---

### E7. PC Orchestrator UI — **P0** (FR6, FR2, FR7, FR10)

* [ ] Dashboard: device list, battery, state, last ping; session controls.
* [ ] Indicators: elapsed time; per‑device file sizes/bitrate; warnings/errors.
* [ ] Actions: New Session, Arm, Start, Sync Mark, Stop, Ingest progress.
  **DoD:** Operator completes end‑to‑end session without touching phones; errors clearly surfaced.

**Deps:** E5, E6.

---

### E8. Time Reconciliation (Local mode) — **P0** (FR11)

* [ ] PC reconciliation: apply offset stamps + `SYNC_MARK` to compute unified `t_ref_ns`.
* [ ] Export reconciled summary (per‑device lag/jitter; alignment QC).
  **DoD:** Post‑ingest alignment ≤ ±1 frame across videos; GSR aligned to reference within targets.

**Deps:** E2, E6.

---

### E9. Reliability & Recovery — **P1** (FR8, NFR‑Rel)

* [ ] Auto‑reconnect (WS + Bluetooth); exponential back‑off.
* [ ] **State replay**: on rejoin, device requests missed commands; resumes if session active.
* [ ] Crash‑safe writers; partial file salvage.
  **DoD:** Forced Wi‑Fi drop on one phone does not stop others; rejoin resumes; no corrupted files after forced kill.

**Deps:** E3–E6.

---

### E10. Bridged Mode (optional live GSR) — **P1** (FR‑L0, FR5, NFR‑Perf)

* [ ] Leader → PC live `GSR_SAMPLE` frames (batched); ring buffer; back‑pressure.
* [ ] UI live counter/preview (no strict requirement in Local mode).
  **DoD:** p95 Android→PC latency ≤150 ms; zero loss in 30‑min soak; toggle between Local/Bridged recorded in metadata.

**Deps:** E4, E1.

---

### E11. Calibration Utility (RGB↔Thermal) — **P1** (FR9)

* [ ] PC‑driven capture of N paired frames; OpenCV solve; reprojection error.
* [ ] Persist per‑device params `calibration/<deviceId>.json`; link in metadata.
  **DoD:** Error reported; params saved; re‑capture flow on high error.

**Deps:** E3, E7.

---

### E12. Security & Enrolment — **P1** (NFR‑Sec)

* [ ] TLS for WS; pinned certs or local CA.
* [ ] Device enrolment (QR/token); allow‑list on PC.
* [ ] Security lints (no PII; safe defaults).
  **DoD:** Only enrolled devices connect; traffic encrypted; warnings on insecure configs.

**Deps:** E1.

---

### E13. Simulation & Test Fixtures — **P1** (NFR‑Maint/Use)

* [ ] GSR simulator (seed, artefact injection).
* [ ] Synthetic camera source (colour bars/timecode).
* [ ] Headless Android client stub for CI protocol tests.
  **DoD:** CI can run end‑to‑end without hardware; reproducible outputs for fixed seed.

**Deps:** E1–E4.

---

### E14. Data Schema, QC & Lints — **P1** (NFR‑Int)

* [ ] JSON Schema for `meta.json`, `manifest.json`.
* [ ] Data dictionary in `docs/`; schema validator tool.
  **DoD:** Schemas validate in CI; sample sessions pass lints; QC flags present.

**Deps:** E4–E6.

---

### E15. Observability — **P2** (NFR‑Maint/Rel)

* [ ] Structured logs (JSON) with session/device IDs; rotation.
* [ ] Metrics: offset, jitter, dropped frames/samples, upload throughput.
* [ ] One‑click debug bundle (logs + meta).
  **DoD:** Operators can export a bundle; metrics visible for a session.

**Deps:** E1–E7.

---

### E16. Documentation & Ops — **P0** (NFR‑Use/Maint)

* [ ] Operator guide (PC + Android quick‑start).
* [ ] Developer guide (architecture, build/run, adding sensors).
* [ ] Acceptance procedures mapped to FR/NFR; reproducibility note (commit hashes).
  **DoD:** New user records a 2‑device session in <15 min using docs.

**Deps:** Rolling.

---

## Acceptance / Soak Tests (map to NFRs)

* **T1 Time accuracy (P0)**: 30‑min run; median offset ≤5 ms, p95 ≤15 ms; `SYNC_MARK` alignment ±1 frame. *(NFR‑Time)*
* **T2 Performance (P0)**: 3 phones (RGB) + GSR leader; 1080p\@30 + 128 Hz; no loss; UI responsive. *(NFR‑Perf)*
* **T3 Reliability (P1)**: Induced Wi‑Fi drop (5 min) on leader; continuous local CSV; reconciliation OK post‑ingest. *(NFR‑Rel)*
* **T4 Offload (P0)**: Interrupted upload resumes; checksums verified; manifest complete. *(FR10, NFR‑Int)*
* **T5 Security (P1)**: Unenrolled device rejected; TLS enforced; warning on downgrade. *(NFR‑Sec)*

---

## Traceability (epic → requirements)

* **E1/E7** → FR2, FR6, FR7
* **E2/E8** → FR3, FR11, NFR‑Time
* **E3/E4** → FR1, FR5, FR8
* **E5/E6** → FR4, FR10
* **E11** → FR9
* **E12** → NFR‑Sec
* **E13/E14/E15/E16** → NFR‑Maint/Int/Use/Rel

---

## Non‑Goals (explicit out of scope for this phase)

* Multi‑host GSR, cloud backends, on‑device ML inference, iOS client.

---

## Issue Labels (suggested)

`priority:P0|P1|P2`, `component:pc|android|proto|time|ui|ingest|security|calibration|sim`, `type:feature|bug|techdebt|docs`, `good-first-issue`.

---

## ADRs to write

1. **Transport**: WS+JSON vs gRPC (chosen: WS+JSON for simplicity).
2. **Time sync**: SNTP‑like service vs third‑party library (chosen: lightweight service).
3. **Storage**: MP4 segmenting strategy; CSV vs Parquet for signals (chosen: CSV initially).
4. **Security**: Local CA + pinning; enrolment flow.

---

### Definition of Done (project‑wide)

* Requirements mapped (ID in PR).
* Unit/integration tests added or updated.
* CI green; schemas validated; docs amended.
* No TODOs in production code; logs free of secrets.

---

**Next step**: create GitHub issues from E0–E8 (P0 scope), link to FR/NFR IDs, and start with M0→M1→M2.
