#!/usr/bin/env bash
# Monta o site estatico do Marellar (GitHub Pages) em client-godot/export/web:
#   1. compila o motor para JavaScript (marellar-web, TeaVM);
#   2. exporta o cliente Godot para Web (sem threads: roda sem cabecalhos COOP/COEP);
#   3. copia o motor, o script de inicio e os dados de data/ ao lado do index.html.
# Uso: GODOT=/caminho/do/godot web/montar_site.sh
set -euo pipefail
cd "$(dirname "$0")/.."
GODOT="${GODOT:-godot}"
SAIDA=client-godot/export/web

./mvnw -B -q -pl marellar-web -am package -DskipTests -Dteavm.minifying=true
rm -rf "$SAIDA" && mkdir -p "$SAIDA/data"
"$GODOT" --headless --path client-godot --import
"$GODOT" --headless --path client-godot --export-release "Web" export/web/index.html
cp marellar-web/target/js/marellar-motor.js web/marellar-inicio.js "$SAIDA/"
cp data/*.json "$SAIDA/data/"
touch "$SAIDA/.nojekyll"
echo "Site pronto em $SAIDA"
ls -la "$SAIDA"
