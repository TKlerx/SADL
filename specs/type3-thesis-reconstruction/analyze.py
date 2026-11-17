from bisect import bisect_left, bisect_right
from collections import Counter, defaultdict
from pathlib import Path
from statistics import mean
import csv
import re

seeds = (7222525536004714236, 37, 20261117)
modes = {
    'PDRTA': 'tk-pdrta-normal/tpta-noPrep',
    'PDTTA': 'tk-pdtta-det-normal/tpta-prep',
}
event = re.compile(r'\(([^,]+),(-?\d+)\)')
alias = re.compile(r'^([abcd])\.\d+$')

def auc(normals, anomalies):
    normals = sorted(normals)
    n = len(normals)
    return sum(n - (bisect_left(normals, a) + bisect_right(normals, a)) / 2
               for a in anomalies) / (n * len(anomalies))

def distance(normal, abnormal):
    return sum(abs(normal[p] / normal.total() - abnormal[p] / abnormal.total())
               for p in normal.keys() | abnormal.keys()) / 2

def frequencies(rows, collapse):
    counts = (Counter(), Counter())
    for row in rows:
        text, label = row.rsplit(':', 1)
        symbols = (symbol for symbol, _ in event.findall(text))
        path = tuple(alias.sub(r'\1', symbol) if collapse else symbol for symbol in symbols)
        assert path
        counts[int(label)][path] += 1
    return counts

with Path('path-frequencies.tsv').open('w') as table, Path('per-file.tsv').open('w') as file_table:
    writer = csv.writer(table, delimiter='\t')
    writer.writerow(['seed', 'mode', 'file', 'arm', 'path', 'normal', 'anomaly'])
    files_writer = csv.writer(file_table, delimiter='\t')
    files_writer.writerow(['seed', 'mode', 'file', 'arm', 'normal_count', 'anomaly_count', 'TV', 'AUC'])
    for seed in seeds:
        scores = defaultdict(lambda: ([], []))
        pairs = defaultdict(list)
        for line in (Path(str(seed)) / 'scores.log').read_text().splitlines():
            if line.startswith('SCORE\t'):
                _, name, collapse, anomaly, value = line.split('\t')
                scores[Path(name), collapse == 'true'][anomaly == 'true'].append(float(value))
            elif line.startswith('PAIR\t'):
                _, name, collapse, value = line.split('\t')
                pairs[Path(name), collapse == 'true'].append(float(value))
        data = {}
        totals = {}
        print(f'### Seed {seed}')
        for mode, path in modes.items():
            root = Path('output2/smac-data') / path / 'type3/test'
            files = sorted((Path(str(seed)) / root).glob('*.txt'))
            assert len(files) == 10
            for collapse in (False, True):
                arm = 'base' if collapse else 'exported'
                pooled = (Counter(), Counter())
                data[mode, arm] = []
                matched_count = 0
                for file in files:
                    relative = file.relative_to(str(seed))
                    sequence = int(file.name.split('-')[-1].split('_')[0])
                    train, test = file.read_text().split('?' * 25)
                    rows = [row for row in test.splitlines() if row]
                    assert len(train.splitlines()) == 700 and len(rows) == 300
                    normal, abnormal = frequencies(rows, collapse)
                    normal_scores, anomaly_scores = scores[relative, collapse]
                    assert len(normal_scores) == normal.total() and len(anomaly_scores) == abnormal.total()
                    assert pairs[relative, collapse] == normal_scores
                    matched_count += len(normal_scores)
                    score = auc(normal_scores, anomaly_scores)
                    tv = distance(normal, abnormal)
                    data[mode, arm].append((sequence, tv, score))
                    pooled[0].update(normal)
                    pooled[1].update(abnormal)
                    files_writer.writerow([seed, mode, sequence, arm, normal.total(), abnormal.total(), tv, score])
                    files_writer.writerow([seed, mode, sequence, arm + '-matched', normal.total(), normal.total(), 0, 0.5])
                    for p in sorted(normal.keys() | abnormal.keys()):
                        writer.writerow([seed, mode, sequence, arm, ' '.join(p), normal[p], abnormal[p]])
                    for p in sorted(normal):
                        writer.writerow([seed, mode, sequence, arm + '-matched', ' '.join(p), normal[p], normal[p]])
                totals[mode, arm] = pooled
                top = sorted(pooled[0].keys() | pooled[1].keys(), key=lambda p: abs(pooled[0][p]/pooled[0].total()-pooled[1][p]/pooled[1].total()), reverse=True)[:3]
                print(f'{mode} {arm} n {pooled[0].total()}/{pooled[1].total()} pairs {matched_count} pooled TV {distance(*pooled):.3f} mean AUC {mean(v[2] for v in data[mode,arm]):.3f}')
                print('top:', '; '.join(f'{" ".join(p)} {pooled[0][p]}/{pooled[1][p]}' for p in top))
        assert data['PDRTA','base'] == data['PDRTA','exported']
        print('| File | PDRTA TV | PDRTA AUC | PDTTA exported TV | PDTTA exported AUC | PDTTA base TV | PDTTA base AUC |')
        print('| ---: | ---: | ---: | ---: | ---: | ---: | ---: |')
        for rows in zip(data['PDRTA','base'], data['PDTTA','exported'], data['PDTTA','base'], strict=True):
            assert len({row[0] for row in rows}) == 1
            print(f'| {rows[0][0]} | ' + ' | '.join(f'{x:.3f}' for row in rows for x in row[1:]) + ' |')
