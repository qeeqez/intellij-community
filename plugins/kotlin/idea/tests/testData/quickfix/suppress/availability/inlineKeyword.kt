// "Suppress 'NOTHING_TO_INLINE' for fun run12" "true"

inline<caret> fun <T> run12(noinline f: () -> T): T = f()
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.inspections.suppress.KotlinSuppressIntentionAction
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.inspections.suppress.KotlinSuppressIntentionAction