# Project Backlog

This file tracks pending items and TODOs identified in the thesis documentation and system development.

### Testing and Quality Assurance
- [ ] **Appium End-to-End Testing**: Implement Appium-based cross-platform tests spanning Android app and Web dashboard interactions for complete user journey validation
- [ ] **Visual Regression Testing**: Add screenshot comparison testing for UI consistency across Android fragments and PyQt5 windows using tools like percy.io or visual-testing libraries
- [ ] **Hardware-in-the-Loop Testing**: Develop automated testing infrastructure with real Shimmer sensors and thermal cameras for integration validation without manual intervention
- [ ] **Synthetic Load Testing**: Create Socket.IO load generators to simulate multiple concurrent device connections and high-frequency data streaming scenarios
- [ ] **Thermal Camera Integration Testing**: Automated testing pipeline for TOPDON thermal camera SDK integration when hardware becomes available in test environment
- [ ] **Performance Regression Testing**: Establish baseline performance metrics and automated detection of performance degradation in CI pipeline
- [ ] **Accessibility Compliance Testing**: Expand beyond basic AccessibilityChecks to include full WCAG 2.1 compliance testing for Web UI using tools like axe-core
- [ ] **Security Penetration Testing**: Automated security scanning for Web APIs, input validation, and potential vulnerability detection in CI/CD pipeline
- [ ] **Cross-Browser Compatibility Testing**: Playwright-based testing across Chrome, Firefox, Safari, and Edge for Web dashboard compatibility
- [ ] **Device Compatibility Matrix Testing**: Systematic testing across different Android versions (8.0-14) and device manufacturers beyond Samsung

## Documentation TODOs

### High Priority
- [ ] **Maintenance Documentation** (Appendices): Provide detailed maintenance schedule document covering daily checks, weekly maintenance, monthly calibration, and annual system updates
- [ ] **Figure Implementation - Session Data**: Complete figures A2, A3, A4, A5, A10, A11 requiring implementation with collected session data
- [ ] **Figure Implementation - Sensor Data**: Complete figure A9 requiring implementation with sensor characterisation data  
- [ ] **Figure Implementation - Usage Data**: Complete figure A11 requiring implementation with usage analysis data

- [ ] **Chapter 4 Citation Mapping**: Replace bracketed numeric references [13], [14], [16], etc. with proper \cite{...} keys in `docs/thesis_report/final/latex/references.bib`
- [ ] **Centralise Bibliography**: Migrate `docs/thesis_report/final/latex/references.tex` to BibTeX using root-level `references.bib`; update `docs/thesis_report/final/latex/main.tex` to use `\bibliography{references}` with natbib; map all chapter citations to \cite{...}; remove manual thebibliography after verification
- [ ] **Chapter 4 Figures Verification**: Provide assets for `fig_4_04_desktop_gui_layout.png`, `fig_4_05_protocol_sequence.png`, `fig_4_06_data_processing_pipeline.png` or update figure wrappers accordingly
- [ ] **Chapter 4 LaTeX Integration**: Integrate `docs/thesis_report/final/4.tex` into LaTeX build (e.g., `docs/thesis_report/final/latex/main.tex`), reconcile prior entry pointing to `docs/thesis_report/final/latex/chapter4.tex`, and standardize chapter file locations
- [ ] **LaTeX Preamble Packages**: Ensure main preamble includes `graphicx`, `textcomp`, `amsmath`, and `hyperref` required by Chapter 4; validate symbol rendering (\texttimes, \mu) and figure compilation
- [ ] **Figure 4.6 Duplication Validation**: Confirm whether Figure 4.6 should appear twice; if not, remove duplicate include and update references accordingly
- [ ] **Chapter 2 Citation Mapping**: Replace bracketed numeric references [4], [5], [7], [21] in `docs/thesis_report/final/latex/chapter2.tex` with proper \cite{...} keys in `references.bib`; add missing entries for Chen2019, Patel2024, Zhang2021, RFC793, pandas, h5py
- [ ] **Chapter 2 LaTeX Integration**: Include `docs/thesis_report/final/2.tex` in the thesis master once available; reconcile duplication with `docs/thesis_report/final/latex/chapter2.tex`; verify compilation
- [ ] **Chapter 2 Location Normalisation**: Standardize chapter file locations (prefer `docs/thesis_report/final/*.tex`); remove duplicate `final/latex/chapter2.tex` after integration and update includes accordingly

- [ ] **Chapter 5 Citation Mapping**: Audit Chapter 5 for bracketed numeric references and replace with proper \cite{...} keys in `docs/thesis_report/final/latex/references.bib`
- [ ] **Chapter 5 Figure Integration Verification**: Confirm TikZ/pgfplots figures compile and render correctly (sync accuracy, scalability, extended operation, correlation)
- [ ] **Chapter 5 Content Parity Review**: Ensure `latex/chapter5.tex` fully reflects `final/5.md`; integrate any missing sections or details
- [ ] **Chapter 3 Citation Mapping**: Replace bracketed numeric references [1], [11], [12], [21] in `docs/thesis_report/final/3.tex` with proper \cite{...} keys in `references.bib` or standardize on centralised references.md
- [ ] **Samsung Device Validation**: Build and run Android app on a Samsung device after documentation changes impacting Android features; record outcomes in test reports
- [ ] **Chapter 6 Citation Mapping**: Replace bracketed numeric references [21], [22], [17], [8], [19] in `docs/thesis_report/final/6.tex` with proper \cite{...} keys from `references.bib`
- [ ] **Chapter 6 LaTeX Preamble Packages**: Ensure thesis master preamble includes `textcomp` or `siunitx` for symbol commands (\textmu, \texttimes) used in Chapter 6
- [ ] **Chapter 6 Build Verification**: Include `docs/thesis_report/final/6.tex` in the thesis master file and compile; resolve any warnings from symbols or references
- [ ] **Chapter 6 Specific Improvements Operationalization**: Track execution of concrete improvements listed in Chapter 6
  - [ ] UI Threading and Responsiveness: Refactor DeviceManager.scan_network() to async QNetworkAccessManager; add pyqtSignal updates; set 5 s timeouts; replace blocking sockets in SessionManager.py
  - [ ] mDNS-Based Discovery: Integrate python-zeroconf on desktop and Android NsdManager; register `_bucika._tcp.local`
  - [ ] Shimmer SDK Replacement: Implement RFCOMM protocol with heartbeat; reverse-engineer packet format; remove fragile SDK dependency
  - [ ] Hardware GSR Sync Trigger: Arduino Nano TTL pulse generator; target <200 µs sync accuracy; parts list and PCB
  - [ ] Contactless GSR Algorithm: Thermal features from palmar regions; ridge regression; dataset 20x10min; target R^2 > 0.6
- [ ] **Mermaid Diagram Policy**: No architectural changes were introduced by Chapter 4 conversion; document rationale for not adding new Mermaid diagrams

### Medium Priority
- [ ] **Cross-reference Validation**: Validate and fix internal links pointing to `docs/thesis_report/Chapter_7_Appendices.md` to ensure they point to correct paths
- [ ] **ADR References**: Add cross-references to Architecture Decision Records (ADR-001, ADR-002, ADR-003) in Chapter 4 design decision sections
- [x] **Risk Management Section**: Added comprehensive risk management section to Chapter 3 (3.7) covering technical, operational, and project management risks (2025-08-11)
- [ ] **MD→LaTeX Automation**: Set up Pandoc-based conversion pipeline and CI job to keep `.tex` files in sync with `.md`; add parity tests to verify structure and content alignment
- [x] **LaTeX Integration**: Created minimal master at `docs/thesis_report/final/main.tex` including `1.tex` and `3.tex` (2025-08-11); TODOs remain for full compile and docs
- [ ] **LaTeX Build Pipeline**: Add BibTeX integration, preamble package review, Mermaid render pipeline, and CI LaTeX compile; update README with build instructions

### Low Priority
- [ ] **Extended Performance Testing**: Implement peak load scenario tests (>8 devices, additional video streams)
- [ ] **Advanced Synchronisation Metrics**: Develop additional synchronisation quality metrics and visualisations

## Code/System TODOs

### Future Enhancements
- [ ] **Cloud Integration**: Investigate cloud storage and remote monitoring capabilities
- [ ] **Mobile Controller**: Explore Android device as session coordinator option
- [ ] **Edge Computing**: Research single-board computer alternatives to replace PC controller
- [ ] **Production Deployment**: Address technical debt, improve test coverage, and containerisation for easier setup

## Notes
- Items marked as TODO in thesis documentation should be addressed before final submission
- Session data collection needed for completing several appendix figures
- Consider ethics approval requirements for any future human participant studies


### Chapter 5 LaTeX Integration TODOs
- [x] Restore `docs/thesis_report/final/latex/main.tex` and ensure preamble includes `\usepackage{amssymb}` (for \checkmark) and `\usepackage{hyperref}`. — Completed 2025-08-11
- [x] Integrate `docs/thesis_report/final/latex/chapter5.tex` into the main build via `\input{chapter5}`. — Completed 2025-08-11
- [ ] Verify that \checkmark renders correctly; if not, switch to a text marker or include `pifont` with `\ding{51}`).


### Appendices Conversion Follow-ups (Added 2025-08-11)
- [x] Convert Appendix C (Supporting Documentation) from placeholder to full LaTeX with proper sections, lists, and citations. — Completed 2025-08-11
- [ ] Convert Appendix F (Code Listings) using listings or minted; define language styles; replace verbatim placeholders with \begin{lstlisting} or minted blocks.
- [x] Convert Appendix Z (Consolidated Figures) to LaTeX with figure environments and cross-reference tables; render Mermaid to static images for print. — Initial conversion completed with verbatim (2025-08-11); TODO in later pass: render Mermaid to static images and convert tables to LaTeX tabular.
- [ ] Replace verbatim-wrapped content in A, B, D, E, G, H, I with structured LaTeX (itemize/enumerate/tabular) and map bracket citations to \cite{...} using references.bib.
- [ ] Add \usepackage{listings} (or minted) and styles to main preamble when moving away from verbatim.
- [ ] Compile LaTeX build and resolve any warnings related to verbatim and UTF-8 symbols.
- [ ] Samsung device validation note: documentation changes do not affect runtime; schedule Android build and run on Samsung device to satisfy policy; record results in test reports.
- [ ] Add lightweight tests to verify presence of all appendix files and \input entries in main.tex.


### Mermaid Integration Follow-ups (Added 2025-08-11)
- [ ] Replace remaining Mermaid code blocks embedded in verbatim across appendices (A, B, D, E, G, H) with live LaTeX `\begin{mermaid}` environments where appropriate; keep PNG fallbacks for print where needed.
- [ ] Set up CI LaTeX build job with `-shell-escape` enabled and Node.js + `@mermaid-js/mermaid-cli` installed; cache rendered assets to reduce build time.
- [ ] Document fallback strategy if Mermaid package unavailable on target toolchain (export static images and disable live rendering).
- [ ] Validate that all Mermaid diagrams compile without syntax warnings (update docs/diagrams/*.mmd as needed).
