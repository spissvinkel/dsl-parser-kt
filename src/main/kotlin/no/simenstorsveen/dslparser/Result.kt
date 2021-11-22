package no.simenstorsveen.dslparser

sealed class Result<I, R>(val nextInput: Input<I>) {

    abstract fun isSuccess(): Boolean

    fun isFailure(): Boolean = !isSuccess()
}

class Success<I, R>(val value: R, nextInput: Input<I>) : Result<I, R>(nextInput) {

    override fun isSuccess(): Boolean = true
    override fun toString(): String = "Success($value)"
}

class Failure<I, R>(val errorMessage: String, nextInput: Input<I>) : Result<I, R>(nextInput) {

    override fun isSuccess(): Boolean = false
    override fun toString(): String = "Failure(${fullErrorMessage()})"

    fun fullErrorMessage(): String = nextInput.fullErrorMessage(errorMessage)
}
