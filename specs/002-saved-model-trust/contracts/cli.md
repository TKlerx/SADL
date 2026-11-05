# Saved model CLI contract

The [migrated CLI](../../train-to-test-cli/spec.md) remains the baseline for successful use. `test -model <path>` accepts a supported artifact within the [content policy and limits](../data-model.md). Operator knowledge of provenance does not bypass checks.

On success, the existing evaluation result and exit behavior apply. The existing fresh and frozen RTI fixture must retain TP 1, FP 0, TN 1, and FN 0 without changes to its inputs or reference bytes.

For missing, unreadable, malformed, unsupported, or over-limit models, fail before evaluation. Return a nonzero exit and a concise model-load diagnostic identifying the failure category, without printing model contents. Create no evaluation output and leave any pre-existing result untouched. Apply the same fail-closed behavior if the runtime cannot install the filter or the worker fails during loading.
