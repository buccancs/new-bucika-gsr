# LaTeX Thesis — Bibliography (BibTeX) Workflow

This thesis now uses a centralised BibTeX database for references instead of a manual `thebibliography` list.

Key files:

- main.tex — includes chapters and the global bibliography section
- references.bib — BibTeX database with keys ref1–ref24
- 2.tex (example chapter) — uses `\cite{...}` / `\citet{...}` instead of hard-coded [n]

Build sequence (BibTeX):

1) pdflatex main
2) bibtex main
3) pdflatex main
4) pdflatex main

Notes:

- Citations are numeric with square brackets via natbib: `\usepackage[numbers]{natbib}` and `\setcitestyle{square}`.
- The bibliography heading is renamed to "References" in main.tex using `\renewcommand{\bibname}{References}` (report
  class).
- If Mermaid diagrams are used, ensure a LaTeX package or external toolchain is available (e.g., mermaid-cli) and
  compile with appropriate flags (e.g., `-shell-escape`).

Mermaid: Bibliography flow

```mermaid
flowchart LR
    A[Chapter .tex with citation keys] --> B[natbib]
    B --> C[LaTeX pass 1 (main.aux)]
C --> D[BibTeX reads references.bib]
D --> E[main.bbl generated]
E --> F[LaTeX pass 2]
F --> G[LaTeX pass 3]
G --> H[PDF with numbered citations and References]
```

Conventions for adding references:

- Add entries to references.bib using the existing keys scheme (ref1, ref2, ...). Prefer @article, @book, @online,
  @techreport as appropriate.
- Include DOI where available; include url and urldate for online resources.
- TODO placeholders are allowed for missing fields; please fill them when sources are available.

Outstanding items (to be addressed):

- Convert remaining chapters/appendices to \cite commands where bracketed numbers remain.
- Enrich references.bib with missing pagination/issue metadata.
- Validate LaTeX build on your environment (TeX Live/MiKTeX) and resolve optional Mermaid package warning if needed.
