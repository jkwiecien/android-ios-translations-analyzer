package com.schibsted.stringsmerger


object Levenshtein {

    /**
     * Odleglosc Levenshteina (odleglosc edycyjna)
     */
    fun calculate(first: String, second: String): Int {
        var i: Int
        var j: Int
        val m: Int
        val n: Int
        var cost: Int
        val d: Array<IntArray>

        m = first.length
        n = second.length

        d = Array(m + 1) { IntArray(n + 1) }

        i = 0
        while (i <= m) {
            d[i][0] = i
            i++
        }
        j = 1
        while (j <= n) {
            d[0][j] = j
            j++
        }

        i = 1
        while (i <= m) {
            j = 1
            while (j <= n) {
                if (first[i - 1] == second[j - 1])
                    cost = 0
                else
                    cost = 1

                d[i][j] = Math.min(d[i - 1][j] + 1, /* remove */
                        Math.min(d[i][j - 1] + 1, /* insert */
                                d[i - 1][j - 1] + cost))   /* change */
                j++
            }
            i++
        }

        return d[m][n]
    }

}