#include <stdio.h>
#include "treba.h"

int dffa_alergia_test(struct dffa *, int, int, double);
int dffa_merge_compatible(struct dffa *, int, int,
    double, int (*)(struct dffa *, int, int, double), int);

/* Two disjoint chains over one symbol. Each state has 100 observations.
 * The left chain stops at depth two and the right chain stops at depth three. */
int main(int argc, char **argv) {
    (void)argc;
    (void)argv;
    int transitions[] = {1, 2, 0, 4, 5, 6, 0};
    int continuing[] = {100, 100, 0, 100, 100, 100, 0};
    int stopping[] = {0, 0, 100, 0, 0, 0, 100};
    int totals[] = {100, 100, 100, 100, 100, 100, 100};
    struct dffa tree = {7, 1, transitions, continuing, stopping, totals};
    const double alpha = 0.05;

    puts("left,right,local,recursive_off,recursive_on");
    for (int depth = 0; depth < 3; ++depth) {
        int left = depth;
        int right = depth + 3;
        printf("%d,%d,%d,%d,%d\n", left, right,
            dffa_alergia_test(&tree, left, right, alpha),
            dffa_merge_compatible(&tree, left, right, alpha, dffa_alergia_test, 0),
            dffa_merge_compatible(&tree, left, right, alpha, dffa_alergia_test, 1));
    }
}
