"""Compare the model and trace files written by SharedTrebaReferenceCapture."""

import ast
import math
import sys
from pathlib import Path


def read_model(path):
    stops, arcs = {}, {}
    for line in path.read_text().splitlines()[1:]:
        kind, source, *fields = line.split("\t")
        if kind == "stop":
            stops[source] = float(fields[0])
        else:
            symbol, target, probability = fields
            arcs.setdefault(source, {})[symbol] = (target, float(probability))
    return stops, arcs


def same_probability(left, right):
    return math.isclose(left, right, rel_tol=0, abs_tol=1e-12)


def compare_models(native, java):
    left_stops, left_arcs = native
    right_stops, right_arcs = java
    mapped = {"0": "0"}
    used = {"0"}
    pending = ["0"]
    for left in pending:
        right = mapped[left]
        if not same_probability(left_stops[left], right_stops[right]):
            return False
        outgoing = left_arcs.get(left, {})
        other = right_arcs.get(right, {})
        if outgoing.keys() != other.keys():
            return False
        for symbol, (target, probability) in outgoing.items():
            peer, value = other[symbol]
            if not same_probability(probability, value):
                return False
            if target in mapped:
                if mapped[target] != peer:
                    return False
            elif peer in used:
                return False
            else:
                mapped[target] = peer
                used.add(peer)
                pending.append(target)
    return len(mapped) == len(left_stops) == len(right_stops)


def compare_traces(native, java):
    acceptance = event_scores = time_scores = 0
    for left, right in zip(native, java, strict=True):
        index, length, accepted, events, times = left.split("\t")
        other_index, other_length, other_accepted, other_events, other_times = right.split("\t")
        assert (index, length) == (other_index, other_length)
        acceptance += accepted != other_accepted
        event_scores += different_vector(events, other_events)
        time_scores += different_vector(times, other_times)
    return acceptance, event_scores, time_scores


def different_vector(left, right):
    a, b = ast.literal_eval(left), ast.literal_eval(right)
    return len(a) != len(b) or any(not same_probability(x, y) for x, y in zip(a, b))


for directory in map(Path, sys.argv[1:]):
    for mode in ("mdi",):
        def rows(suffix):
            return (directory / f"{mode}-{suffix}.tsv").read_text().splitlines()[1:]

        models = []
        for fixture in ("small", "model"):
            native, java = (read_model(directory / f"{mode}-{fixture}-{engine}.tsv")
                            for engine in ("native", "java"))
            models.append((len(native[0]), len(java[0]), compare_models(native, java)))
        print(directory.name, mode, models, compare_traces(rows("native"), rows("java")))
