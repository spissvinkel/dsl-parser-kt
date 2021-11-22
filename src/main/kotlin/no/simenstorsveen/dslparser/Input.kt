package no.simenstorsveen.dslparser

abstract class Source<I>(val value: I)

abstract class Input<I>(val source: Source<I>, protected val index: Int = 0) {

    abstract fun forward(length: Int): Input<I>
    abstract fun remaining(): String
    abstract fun remaining(length: Int): String
    abstract fun length(): Int

    fun fullErrorMessage(message: String): String = "$message at $index"
    fun isEmpty(): Boolean = length() <= 0
}
