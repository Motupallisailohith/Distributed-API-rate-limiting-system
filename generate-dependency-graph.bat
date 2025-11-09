@echo off
REM Script to generate dependency graph for the codebase (Windows)

echo ============================================
echo Dependency Graph Generator
echo ============================================
echo.

REM Compile the project
echo Step 1: Compiling project...
call mvn compile -q
if errorlevel 1 (
    echo Error: Compilation failed!
    exit /b 1
)

REM Run the dependency graph generator
echo Step 2: Generating dependency graph...
java -cp target\classes com.Motupallisailohith.ratelimit.tools.DependencyGraphGenerator . dependency-graph.dot
if errorlevel 1 (
    echo Error: Graph generation failed!
    exit /b 1
)

echo.
echo ✅ Dependency graph generated successfully!
echo.
echo Output file: dependency-graph.dot
echo.
echo ============================================
echo How to visualize the graph:
echo ============================================
echo.
echo Option 1: Online Viewer (easiest)
echo   → Copy the content of dependency-graph.dot
echo   → Visit: https://dreampuf.github.io/GraphvizOnline/
echo   → Paste the content and view the graph
echo.
echo Option 2: Command Line (requires graphviz)
echo   Install graphviz:
echo     - Windows: choco install graphviz
echo     - Or download from: https://graphviz.org/download/
echo.
echo   Then generate images:
echo     PNG: dot -Tpng dependency-graph.dot -o dependency-graph.png
echo     SVG: dot -Tsvg dependency-graph.dot -o dependency-graph.svg
echo     PDF: dot -Tpdf dependency-graph.dot -o dependency-graph.pdf
echo.

REM Try to generate PNG and SVG if graphviz is installed
where dot >nul 2>nul
if %errorlevel% equ 0 (
    echo Step 3: Graphviz detected! Generating PNG and SVG...
    dot -Tpng dependency-graph.dot -o dependency-graph.png
    dot -Tsvg dependency-graph.dot -o dependency-graph.svg
    echo ✅ Generated dependency-graph.png and dependency-graph.svg
    echo.
) else (
    echo ℹ️  Graphviz not found. Install it to auto-generate images.
    echo.
)

echo ============================================
echo Graph Information:
echo ============================================
echo.
echo The dependency graph shows:
echo   • Classes as blue boxes
echo   • Interfaces as green diamonds
echo   • Enums as orange ellipses
echo   • Dependencies as arrows
echo   • Packages are color-coded and grouped
echo.
echo Package colors:
echo   • main (green) - Main entry point
echo   • bucket (blue) - Rate limiting algorithms
echo   • protocol (orange) - UDP protocol
echo   • reliability (purple) - Reliability layer
echo   • security (red) - JWT security
echo   • server (yellow) - HTTP/UDP servers
echo   • tools (teal) - Development tools
echo.
