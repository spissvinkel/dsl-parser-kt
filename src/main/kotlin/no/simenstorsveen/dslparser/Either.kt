@file:Suppress("unused")

package no.simenstorsveen.dslparser

sealed class Either<L, R> {

    abstract fun isLeft(): Boolean

    fun isRight(): Boolean = !isLeft()
}

class Left<L, R>(val value: L): Either<L, R>() {

    override fun isLeft(): Boolean = true
    override fun toString(): String = "Left($value)"
}

class Right<L, R>(val value: R): Either<L, R>() {

    override fun isLeft(): Boolean = false
    override fun toString(): String = "Right($value)"
}

fun <T> valueOf(e: Either<T, T>): T = when (e) {
    is Left -> e.value
    is Right -> e.value
}
