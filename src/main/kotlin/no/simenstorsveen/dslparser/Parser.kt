package no.simenstorsveen.dslparser

abstract class Parser<I, R> {

    abstract fun parse(input: Input<I>): Result<I, R>

    fun parseAll(input: Input<I>): Result<I, R> {
        val result = parse(input)
        val next = result.nextInput
        if (result.isSuccess() && !next.isEmpty()) return Failure("Unparsed input remains", next)
        return result
    }

    fun <T> map(f: (value: R) -> T): Parser<I, T> =
        MapParser(p = this, f)

    fun <T> tryMap(f: (value: R, nextInput: Input<I>) -> Result<I, T>): Parser<I, T> =
        TryMapParser(p = this, f)

    fun recover(f: (errorMessage: String) -> R): Parser<I, R> =
        RecoverParser(p = this, f)

    fun tryRecover(f: (errorMessage: String, nextInput: Input<I>) -> Result<I, R>): Parser<I, R> =
        TryRecoverParser(p = this, f)

    fun <T> then(q: Parser<I, T>): Parser<I, Pair<R, T>> =
        ThenParser(p = this, q)

    fun <T> thenSkip(q: Parser<I, T>): Parser<I, R> =
        this.then(q).map { it.first }

    fun <T> skipThen(q: Parser<I, T>): Parser<I, T> =
        this.then(q).map { it.second }

    fun or(q: Parser<I, R>): Parser<I, R> =
        OrParser(p = this, q)

    fun <T> orEither(q: Parser<I, T>): Parser<I, Either<R, T>> =
        OrEitherParser(p = this, q)

    fun opt(): Parser<I, Maybe<R>> =
        opt(p = this)

    fun seq(): Parser<I, List<R>> =
        seq(p = this)
}

fun <I, R> seq(p: Parser<I, R>): Parser<I, List<R>> = SeqParser(p)
fun <I, R> opt(p: Parser<I, R>): Parser<I, Maybe<R>> = p.map<Maybe<R>> { Something(it) }.recover { Nothing() }
fun <I, R> ref(f: () -> Parser<I, R>): Parser<I, R> = RefParser(f)
fun <I, R> succeed(value: R): Parser<I, R> = SucceedParser(value)
fun <I, R> fail(errorMessage: String): Parser<I, R> = FailParser(errorMessage)

abstract class TerminalParser<I, R> : Parser<I, R>()

abstract class DecoratorParser<I, R1, R>(protected val p: Parser<I, R1>) : Parser<I, R>()

abstract class CombinatorParser<I, R1, R2, R>(
    p: Parser<I, R1>, protected val q: Parser<I, R2>
) : DecoratorParser<I, R1, R>(p)

class MapParser<I, R1, R>(p: Parser<I, R1>, private val f: (value: R1) -> R) : DecoratorParser<I, R1, R>(p) {

    override fun parse(input: Input<I>): Result<I, R> = when (val pResult = p.parse(input)) {
        is Success -> Success(f(pResult.value), pResult.nextInput)
        is Failure -> Failure(pResult.errorMessage, pResult.nextInput)
    }
}

class TryMapParser<I, R1, R>(
    p: Parser<I, R1>, private val f: (value: R1, nextInput: Input<I>) -> Result<I, R>
) : DecoratorParser<I, R1, R>(p) {

    override fun parse(input: Input<I>): Result<I, R> = when (val pResult = p.parse(input)) {
        is Success -> f(pResult.value, pResult.nextInput)
        is Failure -> Failure(pResult.errorMessage, pResult.nextInput)
    }
}

class RecoverParser<I, R>(p: Parser<I, R>, private val f: (errorMessage: String) -> R) : DecoratorParser<I, R, R>(p) {

    override fun parse(input: Input<I>): Result<I, R> = when (val pResult = p.parse(input)) {
        is Success -> pResult
        is Failure -> Success(f(pResult.errorMessage), pResult.nextInput)
    }
}

class TryRecoverParser<I, R>(
    p: Parser<I, R>, private val f: (errorMessage: String, nextInput: Input<I>) -> Result<I, R>
) : DecoratorParser<I, R, R>(p) {

    override fun parse(input: Input<I>): Result<I, R> = when (val pResult = p.parse(input)) {
        is Success -> pResult
        is Failure -> f(pResult.errorMessage, pResult.nextInput)
    }
}

class ThenParser<I, R1, R2>(p: Parser<I, R1>, q: Parser<I, R2>) : CombinatorParser<I, R1, R2, Pair<R1, R2>>(p, q) {

    override fun parse(input: Input<I>): Result<I, Pair<R1, R2>> = when (val pResult = p.parse(input)) {
        is Success -> when (val qResult = q.parse(pResult.nextInput)) {
            is Success -> Success(pResult.value to qResult.value, qResult.nextInput)
            is Failure -> Failure(qResult.errorMessage, qResult.nextInput)
        }
        is Failure -> Failure(pResult.errorMessage, pResult.nextInput)
    }
}

class OrParser<I, R>(p: Parser<I, R>, q: Parser<I, R>) : CombinatorParser<I, R, R, R>(p, q) {

    override fun parse(input: Input<I>): Result<I, R> = when (val pResult = p.parse(input)) {
        is Success -> pResult
        is Failure -> q.parse(pResult.nextInput)
    }
}

class OrEitherParser<I, R1, R2>(
    p: Parser<I, R1>, q: Parser<I, R2>
) : CombinatorParser<I, R1, R2, Either<R1, R2>>(p, q) {

    override fun parse(input: Input<I>): Result<I, Either<R1, R2>> = when (val pResult = p.parse(input)) {
        is Success -> Success(Left(pResult.value), pResult.nextInput)
        is Failure -> when (val qResult = q.parse(pResult.nextInput)) {
            is Success -> Success(Right(qResult.value), qResult.nextInput)
            is Failure -> Failure(qResult.errorMessage, qResult.nextInput)
        }
    }
}

class SeqParser<I, R>(p: Parser<I, R>) : DecoratorParser<I, R, List<R>>(p) {

    override fun parse(input: Input<I>): Result<I, List<R>> {
        val rs = mutableListOf<R>()
        var nextInput = input
        var r = p.parse(nextInput)
        while (r is Success) {
            rs.add(r.value)
            nextInput = r.nextInput
            r = p.parse(nextInput)
        }
        return Success(rs, nextInput)
    }
}

class SucceedParser<I, R>(private val value: R) : TerminalParser<I, R>() {

    override fun parse(input: Input<I>): Result<I, R> = Success(value, input)
}

class FailParser<I, R>(private val errorMessage: String) : TerminalParser<I, R>() {

    override fun parse(input: Input<I>): Result<I, R> = Failure(errorMessage, input)
}

class RefParser<I, R>(f: () -> Parser<I, R>) : Parser<I, R>() {

    private val pRef: Parser<I, R> by lazy(f)

    override fun parse(input: Input<I>): Result<I, R> = pRef.parse(input)
}
