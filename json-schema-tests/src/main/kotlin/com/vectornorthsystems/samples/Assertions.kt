package com.vectornorthsystems.samples

/**
 * Throws if left side argument doesn't contain the right side.
 */
infix fun String.shouldContain(string: String) {
    if (string !in this) {
        throw AssertionError("$string is not in $this")
    }
}

/**
 * Throws if caller is not null.
 */
fun <T> T?.shouldBeNull() {
    if (this != null) {
        throw AssertionError("$this is not null")
    }
}

/**
 * Throws if left side argument is not equal to the right side.
 */
infix fun <T> T.shouldBe(string: T) {
    if (this != string) {
        throw AssertionError("$this is not equal to $string")
    }
}
