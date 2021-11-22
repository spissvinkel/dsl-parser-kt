package no.simenstorsveen.dslparser

class StringSource(s: String) : Source<String>(s)

class StringInput(source: Source<String>, index: Int = 0) : Input<String>(source, index) {

    override fun forward(length: Int): StringInput = StringInput(source, index = index + length)
    override fun remaining(): String = source.value.substring(index)
    override fun remaining(length: Int): String = source.value.substring(index, index + length)
    override fun length(): Int = source.value.length - index
}

class LitParser(private val literal: String) : TerminalParser<String, String>() {

    private val n = literal.length

    override fun parse(input: Input<String>): Result<String, String> {
        if (input.length() >= n && input.remaining(n) == literal) return Success(literal, input.forward(n))
        return Failure(errorMessage = "Expected string literal \"$literal\"", input)
    }
}

class ReParser(private val regex: String) : TerminalParser<String, String>() {

    private val re = Regex(if (regex.startsWith(prefix = "^")) regex else "^$regex")

    override fun parse(input: Input<String>): Result<String, String> = re.find(input.remaining())
        ?.let { Success(it.value, input.forward(it.value.length)) }
        ?: Failure(errorMessage = "No match for regex \"${re.pattern}\"", input)
}

fun lit(literal: String): Parser<String, String> = LitParser(literal)
fun re(regex: String): Parser<String, String> = ReParser(regex)


fun <R> String.map(f: (value: String) -> R): Parser<String, R> =
    MapParser(lit(literal = this), f)

fun <R> String.tryMap(f: (value: String, next: Input<String>) -> Result<String, R>): Parser<String, R> =
    TryMapParser(lit(literal = this), f)

fun String.recover(f: (errorMessage: String) -> String): Parser<String, String> =
    RecoverParser(lit(literal = this), f)

fun String.tryRecover(f: (errMsg: String, next: Input<String>) -> Result<String, String>): Parser<String, String> =
    TryRecoverParser(lit(literal = this), f)

fun <R> String.then(q: Parser<String, R>): Parser<String, Pair<String, R>> =
    ThenParser(lit(literal = this), q)

fun <R> Parser<String, R>.then(q: String): Parser<String, Pair<R, String>> =
    ThenParser(p = this, lit(literal = q))

fun String.then(q: String): Parser<String, Pair<String, String>> =
    ThenParser(lit(literal = this), lit(literal = q))

fun <R> String.thenSkip(q: Parser<String, R>): Parser<String, String> =
    this.then(q).map { it.first }

fun <R> Parser<String, R>.thenSkip(q: String): Parser<String, R> =
    this.then(q).map { it.first }

fun String.thenSkip(q: String): Parser<String, String> =
    this.then(q).map { it.first }

fun <R> String.skipThen(q: Parser<String, R>): Parser<String, R> =
    this.then(q).map { it.second }

fun <R> Parser<String, R>.skipThen(q: String): Parser<String, String> =
    this.then(q).map { it.second }

fun String.skipThen(q: String): Parser<String, String> =
    this.then(q).map { it.second }

fun String.or(q: Parser<String, String>): Parser<String, String> =
    OrParser(lit(literal = this), q)

fun String.or(q: String): Parser<String, String> =
    OrParser(lit(literal = this), lit(literal = q))

fun <R> String.orEither(q: Parser<String, R>): Parser<String, Either<String, R>> =
    OrEitherParser(lit(literal = this), q)

fun <R> Parser<String, R>.orEither(q: String): Parser<String, Either<R, String>> =
    OrEitherParser(p = this, lit(literal = q))

fun String.orEither(q: String): Parser<String, Either<String, String>> =
    OrEitherParser(lit(literal = this), lit(literal = q))

fun String.opt(): Parser<String, Maybe<String>> =
    opt(lit(literal = this))

fun String.seq(): Parser<String, List<String>> =
    seq(lit(literal = this))


val digitStr: Parser<String, String> =
    re(regex = "[0-9]").or(fail(errorMessage = "Digit expected"))

val digit: Parser<String, Int> = digitStr.map { it.toInt() }

val integerStr: Parser<String, String> =
    re(regex = "[+-]?(?:0|[1-9]\\d*)").or(fail(errorMessage = "Integer expected"))

val integer: Parser<String, Int> = integerStr.map { it.toInt() }

val floatStr: Parser<String, String> =
    re(regex = "[+-]?(?:0|[1-9]\\d*)(?:\\.\\d+)?").or(fail(errorMessage = "Number expected"));

val float: Parser<String, Float> = floatStr.map { it.toFloat() }


fun <R> parseAll(s: String, p: Parser<String, R>): Result<String, R> = p.parseAll(StringInput(StringSource(s)))
