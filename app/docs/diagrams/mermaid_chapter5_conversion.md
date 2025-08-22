# Chapter 5 Conversion Overview

```mermaid
flowchart TD
    A[docs/thesis_report/final/5.md (Markdown)] -->|Normalise/Convert| B[docs/thesis_report/final/latex/chapter5.tex]
    B --> C{main.tex includes \input{chapter5}}
    C --> D[PDF build]
    subgraph Figures
        F1[figures/chapter5_sync_accuracy.tex (TikZ/pgfplots)]
        F2[figures/chapter5_scalability.tex (TikZ/pgfplots)]
        F3[figures/chapter5_extended_operation.tex (TikZ/pgfplots)]
        F4[figures/chapter5_correlation.tex (TikZ/pgfplots)]
    end
    B --> F1
    B --> F2
    B --> F3
    B --> F4
    subgraph Preamble Deps
        P1((tikz))
        P2((pgfplots))
    end
    P1 --> C
    P2 --> C

    classDef warn fill:#fff7e6,stroke:#cc8400,stroke-width:1px;
    class P1,P2 warn
```

Notes:
- Chapter 5 figures are implemented in TikZ/pgfplots and require \usepackage{tikz} and \usepackage{pgfplots} in the main LaTeX preamble.
- TODO: Review parity with 5.md and map any bracketed references to proper \cite{...} keys.
