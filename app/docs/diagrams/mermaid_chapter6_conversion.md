# Mermaid Diagram: Chapter 6 Conversion Flow

```mermaid
flowchart TD
  A[Chapter 6 Markdown (docs/thesis_report/final/6.md)] --> B[LaTeX Chapter (docs/thesis_report/final/6.tex)]
  B --> C{Thesis Master Compile}
  C -->|Requires| P[Packages: textcomp or siunitx]
  B --> D[TODO: Map [n] refs to \cite{...}]
  D --> E[references.bib]
  C --> F[PDF Output]

  subgraph Constraints
    X[Exclude external/ and docs/generated_docs]
  end
```

Notes:
- Numeric references [n] temporarily preserved; migrate to \cite keys bound to references.bib.
- Ensure thesis preamble includes textcomp or siunitx for symbols (\textmu, \texttimes).
- No changes made under external/ or docs/generated_docs per guidelines.
