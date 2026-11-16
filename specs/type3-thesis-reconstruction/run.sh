#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Usage: bash $0 OUTPUT_DIRECTORY" >&2
    exit 2
fi

script_dir=$(cd -- "$(dirname -- "$0")" && pwd)
repo=$(git -C "$script_dir" rev-parse --show-toplevel)
baseline=8ea0694a0c2e9be772ca31f7a00675ce3f7a2371
historical=6f9fe2c4840c3e968c016f75d17e08337a96e868

mkdir -- "$1"
cd -- "$1"
output=$PWD
mkdir sadl classes
git -C "$repo" archive "$baseline" | tar -x -C sadl
core="$output/sadl/PDTTA-core"
mvn -B -f "$output/sadl/pom.xml" -pl PDTTA-core -DskipTests \
    compile dependency:build-classpath -Dmdep.outputFile="$output/project-classpath.txt" >build.log 2>&1

git -C "$repo" show "$historical:PDTTA-core/src/sadl/run/datagenerators/Temp.java" >Temp.java
python3 - <<'PY'
from pathlib import Path

driver = Path("Temp.java")
text = driver.read_text()
type_filter = "if (type == AnomalyInsertionType.NONE) {"
random_section = "// randomly"
assert text.count(type_filter) == 1 and text.count(random_section) == 1
text = text.replace(type_filter, "if (type != AnomalyInsertionType.TYPE_THREE) {")
text = text.replace(random_section, "if (type == AnomalyInsertionType.TYPE_THREE) { continue; }\n// randomly")
driver.write_text(text)
PY

classpath="$output/classes:$core/target/classes:$(cat project-classpath.txt)"
javac -cp "$classpath" -d classes Temp.java \
    "$script_dir/ReconstructTypeThree.java" "$script_dir/ScoreTypeThree.java"

for seed in 7222525536004714236 37 20261117; do
    mkdir "$seed"
    (
        cd "$seed"
        ln -s "$core/resources" resources
        java -cp "$classpath" ReconstructTypeThree "$seed" >generation.log 2>&1
        java -cp "$classpath" ScoreTypeThree \
            output2/smac-data/tk-pdrta-normal/tpta-noPrep/type3/test/*.txt \
            output2/smac-data/tk-pdtta-det-normal/tpta-prep/type3/test/*.txt >scores.log 2>&1
    )
done

python3 "$script_dir/analyze.py" >results.md
printf 'Results written to %s\n' "$output"
