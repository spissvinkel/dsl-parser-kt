package no.simenstorsveen.dslparser

sealed class Maybe<T> {

    abstract fun isSomething(): Boolean
    abstract fun orDefault(defVal: T): T
    abstract fun <U> map(f: (t: T) -> U): Maybe<U>
    abstract fun <U> flatMap(f: (t: T) -> Maybe<U>): Maybe<U>
    abstract fun filter(p: (t: T) -> Boolean): Maybe<T>

    fun isNothing(): Boolean = !isSomething()
}

class Something<T>(val value: T) : Maybe<T>() {

    override fun isSomething(): Boolean = true
    override fun orDefault(defVal: T): T = value
    override fun <U> map(f: (t: T) -> U): Maybe<U> = Something(f(value))
    override fun <U> flatMap(f: (t: T) -> Maybe<U>): Maybe<U> = f(value)
    override fun filter(p: (t: T) -> Boolean): Maybe<T> = if (p(value)) this else Nothing()
    override fun toString(): String = "Something($value)"
}

class Nothing<T> : Maybe<T>() {

    override fun isSomething(): Boolean = false
    override fun orDefault(defVal: T) = defVal
    override fun <U> map(f: (t: T) -> U): Maybe<U> = Nothing()
    override fun <U> flatMap(f: (t: T) -> Maybe<U>): Maybe<U> = Nothing()
    override fun filter(p: (t: T) -> Boolean): Maybe<T> = Nothing()
    override fun toString(): String = "Nothing"
}
