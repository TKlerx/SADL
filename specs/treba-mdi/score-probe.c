#include <math.h>
#include <stdio.h>
#include <string.h>
#include "treba.h"

double dffa_mdi_score2(struct dffa *, int, int, int);
int dffa_mdi_compatible(struct dffa *, int, int, double, struct observations *, int);

int main(int argc, char **argv) {
    if (argc != 2 || (strcmp(argv[1], "cutoff") && strcmp(argv[1], "blue-only"))) {
        return 2;
    }
    const int blue_only = !strcmp(argv[1], "blue-only");
    int targets[] = {2, 3, 0, 0};
    int arcs[] = {blue_only ? 0 : 2, 3, 0, 0};
    int stops[] = {2, 1, 2, 3};
    int totals[] = {arcs[0] + stops[0], 4, 2, 3};
    struct dffa states = {4, 1, targets, arcs, stops, totals};
    const int observations = totals[0] + totals[1];
    const double score = dffa_mdi_score2(&states, 0, 1, observations);
    const double below = nextafter(score, -INFINITY);
    const double above = nextafter(score, INFINITY);
    printf("%s\t%.17g\t%d\t%d\t%d\n", argv[1], score,
        dffa_mdi_compatible(&states, 0, 1, below, NULL, observations),
        dffa_mdi_compatible(&states, 0, 1, score, NULL, observations),
        dffa_mdi_compatible(&states, 0, 1, above, NULL, observations));
}
