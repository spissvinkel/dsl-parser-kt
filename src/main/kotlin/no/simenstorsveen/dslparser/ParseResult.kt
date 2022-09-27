package no.simenstorsveen.dslparser

sealed class ParseResult<I, R>(val nextInput: Input<I>) {

    abstract fun isSuccess(): Boolean

    fun isFailure(): Boolean = !isSuccess()
}

class ParseSuccess<I, R>(val value: R, nextInput: Input<I>) : ParseResult<I, R>(nextInput) {

    override fun isSuccess(): Boolean = true
    override fun toString(): String = "ParseSuccess($value)"
}

class ParseFailure<I, R>(val errorMessage: String, nextInput: Input<I>) : ParseResult<I, R>(nextInput) {

    override fun isSuccess(): Boolean = false
    override fun toString(): String = "ParseFailure(${fullErrorMessage()})"

    @Suppress("MemberVisibilityCanBePrivate")
    fun fullErrorMessage(): String = nextInput.fullErrorMessage(errorMessage)
}
