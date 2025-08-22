# Thesis Report Final Documentation

This directory contains the final thesis chapters and appendices in both Markdown and LaTeX formats.

## Contents

### Main Chapters
- `1.md` / `latex/1.tex` - Introduction
- `2.md` / `latex/2.tex` - Background and Research Context
- `3.md` / `latex/3.tex` - Requirements Analysis
- `4.md` / `latex/4.tex` - System Design and Architecture
- `5.md` / `latex/5.tex` - Evaluation and Testing
- `6.md` / `latex/6.tex` - Conclusion and Future Work

### Appendices
- `appendix_a_system_manual.md` / `latex/appendix_A.tex` - System Manual
- `appendix_b_user_manual.md` / `latex/appendix_B.tex` - User Manual
- `appendix_c_supporting_documentation.md` / `latex/appendix_C.tex` - Supporting Documentation
- `appendix_d_test_reports.md` / `latex/appendix_D.tex` - Test Reports
- `appendix_e_evaluation_data.md` / `latex/appendix_E.tex` - Evaluation Data
- `appendix_f_code_listings.md` / `latex/appendix_F.tex` - Code Listings
- `appendix_g_diagnostic_figures.md` / `latex/appendix_G.tex` - Diagnostic Figures
- `appendix_h_reference_index.md` / `latex/appendix_H.tex` - Reference Index
- `appendix_i_figures_and_diagrams.md` / `latex/appendix_I.tex` - Figures and Diagrams
- `appendix_z_consolidated_figures.md` / `latex/appendix_Z.tex` - Consolidated Figures

## Content Comparison Tool

This directory includes a comprehensive content comparison tool to ensure synchronisation between Markdown and LaTeX versions:

### Quick Usage
```bash
# Compare all files and generate HTML report
./quick_compare.sh

# Compare specific files
./quick_compare.sh "chapter_1"
./quick_compare.sh "appendix_a"

# Full comparison with details
python compare_md_tex.py --verbose --html-report
```

### Tool Features
- **Content Normalization**: Handles format differences between Markdown and LaTeX
- **Similarity Analysis**: Provides percentage similarity scores
- **HTML Reports**: Interactive visual reports for easy review
- **Critical Issue Detection**: Highlights files requiring immediate attention

### Documentation
- `README_COMPARISON.md` - Comprehensive tool documentation
- `compare_md_tex.py` - Main comparison script
- `quick_compare.sh` - Convenient wrapper script

## Build Process

### LaTeX Compilation
```bash
cd latex/
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
```

See `latex/README.md` for detailed LaTeX build instructions.

## Current Status

Based on the latest comparison run, there are content differences across all file pairs that need attention. See the generated `comparison_report.html` for detailed analysis.

### Critical Files
- **Chapter 2**: Significant content differences (27% similarity)
- **Appendix F**: LaTeX version is a placeholder (0.2% similarity)

### Well-Synchronised Files
- **Appendix A**: System Manual (98.9% similarity)
- **Appendix H**: Reference Index (98.0% similarity)

## Contributing

When updating content:
1. Make changes in the appropriate Markdown file
2. Run the comparison tool to check synchronisation
3. Update the corresponding LaTeX file if needed
4. Verify with another comparison run

## References

- [UCL Thesis Guidelines](https://www.ucl.ac.uk/students/academic-support/)
- [LaTeX Documentation](https://www.latex-project.org/help/documentation/)
- [Markdown Specification](https://spec.commonmark.org/)