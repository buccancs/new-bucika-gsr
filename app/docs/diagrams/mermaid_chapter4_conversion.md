# Chapter 4 Conversion Overview

```mermaid
flowchart TD
    A[docs/thesis_report/final/4.md Markdown] -->|Convert| B[docs/thesis_report/final/4.tex]
    B --> C{main.tex includes 4.tex}
    C --> D[PDF build]
    subgraph Figures
        F1[figures/chapter4_system_architecture.tex TeX wrapper]
        F2((fig_4_03_shimmer_gsr_integration.png))
        F3((fig_4_04_desktop_gui_layout.png))
        F4((fig_4_05_protocol_sequence.png))
        F5((fig_4_06_data_processing_pipeline.png))
    end
    B --> F1
    B --> F2
    B --> F3
    B --> F4
    B --> F5
    classDef missing fill:#fff0f0,stroke:#cc0000,stroke-width:1px;
    class F2,F3,F4,F5 missing
```

Notes:
- Image includes use conditional placeholders to avoid build breaks if assets are missing.
- TODO: Map numeric citations [13], [14], [16], etc. to proper \cite{...} keys in references.bib.
