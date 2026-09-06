#!/usr/bin/env bash
#
# build-pdf.sh
#
# Genera docs/build/manuale-tecnico.pdf e docs/build/manuale-utente.pdf a partire
# da docs/manuale-tecnico.md e docs/manuale-utente.md.
#
# Passi:
#   1. Pre-renderizza ogni blocco ```mermaid``` in un'immagine PNG con mermaid-cli
#      (mmdc), perché xelatex non sa interpretare Mermaid direttamente.
#   2. Copia le immagini referenziate (![](images/nome.png)) da docs/images/ nella
#      cartella di build; per ogni screenshot ancora mancante genera un placeholder
#      vuoto, cosi' la build non si interrompe, e stampa l'elenco a fine esecuzione.
#   3. Lancia Pandoc con il template Eisvogel e il motore xelatex per produrre i due PDF.
#
# Prerequisiti:
#   - pandoc            https://pandoc.org/installing.html
#   - una distribuzione TeX con xelatex (TeX Live / MacTeX / MiKTeX)
#   - il template Eisvogel installato nella cartella dei template di Pandoc
#     (https://github.com/Wandmalfarbe/pandoc-latex-template), tipicamente:
#       mkdir -p "$(pandoc --version | sed -n 's/^User data directory: //p')/templates"
#       # scarica l'ultima release di eisvogel.latex dal repository e copialo li'
#   - Node.js (per "npx @mermaid-js/mermaid-cli" via npx, nessuna installazione globale richiesta)
#   - python3 (usato solo per il pre-processing dei file Markdown, nessuna dipendenza esterna)
#
# Uso:
#   ./docs/build-pdf.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
IMAGES_SRC_DIR="$SCRIPT_DIR/images"

FILES=(manuale-tecnico manuale-utente)

# 1x1 PNG trasparente, usato come segnaposto per gli screenshot non ancora disponibili.
PLACEHOLDER_PNG_B64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="

log()  { printf '\033[1;34m[build-pdf]\033[0m %s\n' "$1"; }
warn() { printf '\033[1;33m[build-pdf][warn]\033[0m %s\n' "$1"; }
die()  { printf '\033[1;31m[build-pdf][error]\033[0m %s\n' "$1" >&2; exit 1; }

check_dependencies() {
  command -v pandoc  >/dev/null 2>&1 || die "pandoc non trovato. Installa pandoc: https://pandoc.org/installing.html"
  command -v xelatex >/dev/null 2>&1 || die "xelatex non trovato. Installa una distribuzione TeX Live/MacTeX/MiKTeX con supporto xelatex."
  command -v npx     >/dev/null 2>&1 || die "npx (Node.js) non trovato. Installa Node.js per usare @mermaid-js/mermaid-cli."
  command -v python3 >/dev/null 2>&1 || die "python3 non trovato."

  local pandoc_data_dir
  pandoc_data_dir="$(pandoc --version | sed -n 's/^User data directory: //p')"
  [ -n "$pandoc_data_dir" ] || pandoc_data_dir="$HOME/.local/share/pandoc"

  if [ ! -f "$pandoc_data_dir/templates/eisvogel.latex" ]; then
    die "Template Eisvogel non trovato in $pandoc_data_dir/templates/eisvogel.latex
Installalo con:
  mkdir -p \"$pandoc_data_dir/templates\"
  # scarica l'ultima release di eisvogel.latex da
  # https://github.com/Wandmalfarbe/pandoc-latex-template
  # e copialo in \"$pandoc_data_dir/templates/eisvogel.latex\"
Poi rilancia questo script."
  fi
}

render_mermaid_diagrams() {
  local name="$1"
  local src="$SCRIPT_DIR/$name.md"
  local out="$BUILD_DIR/$name.rendered.md"

  log "Rendering diagrammi Mermaid per $name.md..."
  python3 - "$src" "$out" "$BUILD_DIR/diagrams" "$name" <<'PYEOF'
import os
import re
import subprocess
import sys

src, out, diagrams_dir, name = sys.argv[1:5]
os.makedirs(diagrams_dir, exist_ok=True)
os.makedirs(os.path.dirname(out), exist_ok=True)

text = open(src, encoding="utf-8").read()
pattern = re.compile(r"```mermaid\n(.*?)\n```", re.DOTALL)
counter = {"n": 0}

def render(match):
    counter["n"] += 1
    idx = counter["n"]
    mmd_path = os.path.join(diagrams_dir, f"{name}-{idx}.mmd")
    png_path = os.path.join(diagrams_dir, f"{name}-{idx}.png")

    with open(mmd_path, "w", encoding="utf-8") as f:
        f.write(match.group(1))

    print(f"  -> diagramma {idx}: {os.path.relpath(png_path)}")
    subprocess.run(
        [
            "npx", "-y", "@mermaid-js/mermaid-cli",
            "-i", mmd_path,
            "-o", png_path,
            "-b", "white",
            "-s", "2",
        ],
        check=True,
    )

    rel = os.path.relpath(png_path, os.path.dirname(out))
    return f"![]({rel})"

rendered = pattern.sub(render, text)
with open(out, "w", encoding="utf-8") as f:
    f.write(rendered)

print(f"  {counter['n']} diagrammi renderizzati.")
PYEOF
}

stage_images() {
  local name="$1"
  local rendered="$BUILD_DIR/$name.rendered.md"

  log "Verifico gli screenshot referenziati in $name.md..."
  python3 - "$rendered" "$IMAGES_SRC_DIR" "$BUILD_DIR/images" "$PLACEHOLDER_PNG_B64" <<'PYEOF'
import base64
import os
import re
import shutil
import sys

rendered, images_src, images_out, placeholder_b64 = sys.argv[1:5]
os.makedirs(images_out, exist_ok=True)

text = open(rendered, encoding="utf-8").read()
placeholder_bytes = base64.b64decode(placeholder_b64)

missing = []
for m in re.finditer(r"!\[[^\]]*\]\(images/([^)]+)\)", text):
    fname = m.group(1)
    dest = os.path.join(images_out, fname)
    if os.path.exists(dest):
        continue
    src_file = os.path.join(images_src, fname)
    if os.path.exists(src_file):
        shutil.copyfile(src_file, dest)
    else:
        with open(dest, "wb") as f:
            f.write(placeholder_bytes)
        missing.append(fname)

if missing:
    print("  Screenshot mancanti (sostituiti con un placeholder vuoto):")
    for fname in missing:
        print(f"    - images/{fname}")
else:
    print("  Tutti gli screenshot referenziati sono presenti.")
PYEOF
}

build_pdf() {
  local name="$1"
  local rendered="$BUILD_DIR/$name.rendered.md"
  local output="$BUILD_DIR/$name.pdf"

  log "Genero $output con Pandoc + Eisvogel (xelatex)..."
  pandoc "$rendered" \
    --resource-path="$BUILD_DIR:$SCRIPT_DIR" \
    --from markdown+yaml_metadata_block \
    --template eisvogel \
    --pdf-engine=xelatex \
    --number-sections \
    --toc \
    --listings \
    -o "$output"
  log "Creato: $output"
}

main() {
  check_dependencies
  mkdir -p "$BUILD_DIR/diagrams" "$BUILD_DIR/images"

  for name in "${FILES[@]}"; do
    [ -f "$SCRIPT_DIR/$name.md" ] || die "$SCRIPT_DIR/$name.md non trovato."
    render_mermaid_diagrams "$name"
    stage_images "$name"
    build_pdf "$name"
  done

  log "Fatto. PDF generati in: $BUILD_DIR"
}

main "$@"
