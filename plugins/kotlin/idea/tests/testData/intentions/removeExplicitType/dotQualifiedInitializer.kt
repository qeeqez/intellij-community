// AFTER-WARNING: Parameter 'v' is never used
fun <T> String.foo(v: T): Int = 1

val s = ""

fun test(): <caret>Int = s.foo(1)