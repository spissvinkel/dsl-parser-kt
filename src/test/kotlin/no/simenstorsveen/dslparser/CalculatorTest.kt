package no.simenstorsveen.dslparser

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

typealias Op = (n: Int) -> Int

class CalculatorTest {

    companion object {

        /*
         * Calculator grammar:
         *
         *     expr ::= term { addTerm | subTerm }
         *     term ::= factor { mulFactor | divFactor }
         *     factor ::= integer | "(" expr ")"
         *     addTerm ::= "+" term
         *     subTerm ::= "-" term
         *     mulFactor ::= "*" factor
         *     divFactor ::= "/" factor
         */

        private val add: (arg: Int) -> Op = { arg -> { acc -> acc + arg } }
        private val sub: (arg: Int) -> Op = { arg -> { acc -> acc - arg } }
        private val mul: (arg: Int) -> Op = { arg -> { acc -> acc * arg } }
        private val div: (arg: Int) -> Op = { arg -> { acc -> acc / arg } }

        private val evaluate: (acc: Int, op: Op) -> Int = { acc, op -> op(acc) }

        private val calculate: (initAndOps: Pair<Int, List<Op>>) -> Int = { (init, ops) -> ops.fold(init, evaluate) }

        // Create references to break cycles and refer to parsers not yet initialized
        private val expr: Parser<String, Int> = ref { term.then(seq(addTerm.or(subTerm))).map(calculate) }
        private val term: Parser<String, Int> = ref { factor.then(seq(mulFactor.or(divFactor))).map(calculate) }

        private val factor: Parser<String, Int> = integer.or("(".skipThen(expr).thenSkip(")"))

        private val addTerm: Parser<String, Op> = "+".skipThen(term).map(add)
        private val subTerm: Parser<String, Op> = "-".skipThen(term).map(sub)
        private val mulFactor: Parser<String, Op> = "*".skipThen(factor).map(mul)
        private val divFactor: Parser<String, Op> = "/".skipThen(factor).map(div)

        fun parse(s: String): ParseResult<String, Int> = parseAll(s, expr)
    }

    @Test
    fun `calculator expressions are parsed correctly as successes`(): Unit = assertSoftly {
        val ss = listOf(
            "2*3" to 6,
            "2*3+7" to 13,
            "2*3-7" to -1,
            "2*3-7/2" to 3,
            "(23)+7" to 30,
            "(2*3)+7" to 13,
            "2*(3+7)" to 20,
            "2*(3+7)/3" to 6,
            "2+3-7" to -2,
            "+2" to 2,
            "-2" to -2,
            "2*-3" to -6,
            "2+-3" to -1,
            "2--3" to 5,
            "2++3" to 5,
            "2-3*7" to -19,
        )
        ss.forEach { (s, v) ->
            val r = parse(s)
            r.shouldBeInstanceOf<ParseSuccess<String, Int>>()
            r.value shouldBe v
        }
    }

    @Test
    fun `calculator expressions are parsed correctly as failures`(): Unit = assertSoftly {
        val ss = listOf(
            "2*(3+7))",
            "2*((3+7)+3",
            "(2*(3+7)",
            "2+3-",
            "x",
            "2+",
            "",
        )
        ss.forEach { s ->
            val r = parse(s)
            r.shouldBeInstanceOf<ParseFailure<String, Int>>()
        }
    }
}
