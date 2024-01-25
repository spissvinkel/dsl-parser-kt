@file:Suppress("unused")

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

    override fun parse(input: Input<String>): ParseResult<String, String> {
        if (input.length() >= n && input.remaining(n) == literal) return ParseSuccess(literal, input.forward(n))
        return ParseFailure(errorMessage = "Expected string literal \"$literal\"", input)
    }
}

class ReParser(regex: String) : TerminalParser<String, String>() {

    private val re = Regex(if (regex.startsWith(prefix = "^")) regex else "^$regex")

    override fun parse(input: Input<String>): ParseResult<String, String> = re.find(input.remaining())
        ?.let { ParseSuccess(it.value, input.forward(it.value.length)) }
        ?: ParseFailure(errorMessage = "No match for regex \"${re.pattern}\"", input)
}

typealias StringParser<T> = Parser<String, T>

fun lit(literal: String): StringParser<String> = LitParser(literal)
fun re(regex: String): StringParser<String> = ReParser(regex)


fun <R> String.map(f: (value: String) -> R): StringParser<R> =
    MapParser(lit(literal = this), f)

fun <R> String.tryMap(f: (value: String, next: Input<String>) -> ParseResult<String, R>): StringParser<R> =
    TryMapParser(lit(literal = this), f)

fun String.recover(f: (errorMessage: String) -> String): StringParser<String> =
    RecoverParser(lit(literal = this), f)

fun String.tryRecover(f: (errMsg: String, next: Input<String>) -> ParseResult<String, String>): StringParser<String> =
    TryRecoverParser(lit(literal = this), f)

fun <R> String.then(q: StringParser<R>): StringParser<Pair<String, R>> =
    ThenParser(lit(literal = this), q)

fun <R> StringParser<R>.then(q: String): StringParser<Pair<R, String>> =
    ThenParser(p = this, lit(literal = q))

fun String.then(q: String): StringParser<Pair<String, String>> =
    ThenParser(lit(literal = this), lit(literal = q))

fun <R> String.thenSkip(q: StringParser<R>): StringParser<String> =
    this.then(q).map { it.first }

fun <R> StringParser<R>.thenSkip(q: String): StringParser<R> =
    this.then(q).map { it.first }

fun String.thenSkip(q: String): StringParser<String> =
    this.then(q).map { it.first }

fun <R> String.skipThen(q: StringParser<R>): StringParser<R> =
    this.then(q).map { it.second }

fun <R> StringParser<R>.skipThen(q: String): StringParser<String> =
    this.then(q).map { it.second }

fun String.skipThen(q: String): StringParser<String> =
    this.then(q).map { it.second }

fun String.or(q: StringParser<String>): StringParser<String> =
    OrParser(lit(literal = this), q)

fun String.or(q: String): StringParser<String> =
    OrParser(lit(literal = this), lit(literal = q))

fun <R> String.orEither(q: StringParser<R>): StringParser<Either<String, R>> =
    OrEitherParser(lit(literal = this), q)

fun <R> StringParser<R>.orEither(q: String): StringParser<Either<R, String>> =
    OrEitherParser(p = this, lit(literal = q))

fun String.orEither(q: String): StringParser<Either<String, String>> =
    OrEitherParser(lit(literal = this), lit(literal = q))

fun String.opt(): StringParser<Maybe<String>> =
    opt(lit(literal = this))

fun String.seq(): StringParser<List<String>> =
    seq(lit(literal = this))


val digitStr: StringParser<String> =
    re(regex = "[0-9]").or(fail(errorMessage = "Digit expected"))

val digit: StringParser<Int> = digitStr.map { it.toInt() }

val integerStr: StringParser<String> =
    re(regex = "[+-]?(?:0|[1-9]\\d*)").or(fail(errorMessage = "Integer expected"))

val integer: StringParser<Int> = integerStr.map { it.toInt() }

val floatStr: StringParser<String> =
    re(regex = "[+-]?(?:0|[1-9]\\d*)(?:\\.\\d+)?").or(fail(errorMessage = "Number expected"))

val float: StringParser<Float> = floatStr.map { it.toFloat() }


fun <R> parseAll(s: String, p: StringParser<R>): ParseResult<String, R> = p.parseAll(StringInput(StringSource(s)))
