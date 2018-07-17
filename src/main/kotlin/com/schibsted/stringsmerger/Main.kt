package com.schibsted.stringsmerger

import com.opencsv.CSVWriter
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

fun main(args: Array<String>) {
    val androidStringsPool = mutableMapOf<String, String>()
    val androidFile = File(args[0])

    println("Parsing android file: ${androidFile.path} ...")

    androidFile.forEachLine {
        if (it.isNotBlank()) {
            "<string.+?name=\"(.+?)\".*?>(.+?)</.+?>".toRegex().findAll(it).forEach {
                val key = it.groupValues[1]
                val value = it.groupValues[2]
                androidStringsPool[key] = value
            }
        }
    }
    val androidStrings = androidStringsPool.toMap()
    println("Android strings: ${androidStrings.size}")


    val iosStringsPool = mutableMapOf<String, String>()
    val iosFile = File(args[1])

    println("Parsing iOS file: ${iosFile.path} ...")

    iosFile.forEachLine {
        if (it.isNotBlank()) {
            "\"(.+?)\" = \"(.+?)\";".toRegex().findAll(it).forEach {
                val key = it.groupValues[1]
                val value = it.groupValues[2]
                iosStringsPool[key] = value
            }
        }
    }
    val iosStrings = iosStringsPool.toMap()
    println("iOS strings: ${iosStrings.size}")


    findIdenticalValues(androidStringsPool, iosStringsPool)
    findSimilarValues(androidStringsPool, iosStringsPool)
    writeRemaining(androidStringsPool, iosStringsPool)
}

fun String.toAndroidString(): String {
    return this.replace("$@", "\$s")
}

private fun findIdenticalValues(androidStringsPool: MutableMap<String, String>, iOsStringsPool: MutableMap<String, String>) {
    println("Looking for identical strings ...")
    val identicalStrings = mutableMapOf<StringKey, String>()
    androidStringsPool.forEach { androidKey, androidValue ->
        var key: StringKey? = null
        val value: String = androidValue

        iOsStringsPool.forEach loop@{ iosKey, iOsValue ->
            if (androidValue == iOsValue.toAndroidString()) {
                key = StringKey(androidKey, iosKey)
                return@loop
            }
        }

        key?.let { identicalStrings[it] = value }
    }

    println("Identical strings: ${identicalStrings.entries.size}. Separating from the remaining pools.")
    identicalStrings.forEach { key, value ->
        androidStringsPool.remove(key.androidKey)
        iOsStringsPool.remove(key.iosKey)
    }

    val identicalStringsFileWriter = Files.newBufferedWriter(Paths.get("csv/identical.csv"), Charset.forName("UTF-8"), StandardOpenOption.CREATE)
    try {
        val identicalStringsCsvWriter = CSVWriter(identicalStringsFileWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)
        val header = arrayOf("former keys", "new key", "value")
        identicalStringsCsvWriter.writeNext(header)
        identicalStrings.forEach { key, value ->
            identicalStringsCsvWriter.writeNext(arrayOf("${key.androidKey} / ${key.iosKey}", "", value))
        }
    } finally {
        identicalStringsFileWriter.flush()
    }
}

private fun findSimilarValues(androidStringsPool: MutableMap<String, String>, iOsStringsPool: MutableMap<String, String>) {
    val maxPercent = 10
    println("Looking for similar strings with tolerance of $maxPercent% ...")
    val similarStrings = mutableMapOf<StringKey, StringValue>()
    androidStringsPool.forEach { androidKey, androidValue ->
        iOsStringsPool.forEach loop@{ iosKey, iOsValue ->
            val levenshtein = Levenshtein.calculate(androidValue, iOsValue.toAndroidString())
            val percent = levenshtein.toFloat() / androidValue.length
            if (percent < (maxPercent.toFloat() / 100.0)) {
                val key = StringKey(androidKey, iosKey)
//                println("Similarity percent for $androidValue and $iOsValue: $percent")
                similarStrings[key] = StringValue(androidValue, iOsValue)
                return@loop
            }
        }
    }

    println("Similar strings: ${similarStrings.entries.size}. Separating from the remaining pools.")
    similarStrings.forEach { key, value ->
        androidStringsPool.remove(key.androidKey)
        iOsStringsPool.remove(key.iosKey)
    }

    val identicalStringsFileWriter = Files.newBufferedWriter(Paths.get("csv/similar.csv"), Charset.forName("UTF-8"), StandardOpenOption.CREATE)
    try {
        val identicalStringsCsvWriter = CSVWriter(identicalStringsFileWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)
        val header = arrayOf("android key", "iOS key", "android value", "iOS value")
        identicalStringsCsvWriter.writeNext(header)
        similarStrings.forEach { key, value ->
            identicalStringsCsvWriter.writeNext(arrayOf(key.androidKey, key.iosKey, value.androidValue, value.iosValue))
        }
    } finally {
        identicalStringsFileWriter.flush()
    }
}

private fun writeRemaining(androidStringsPool: Map<String, String>, iOsStringsPool: Map<String, String>) {
    println("Writing remaining strings to csv ...")
    val remainingStringsFileWriter = Files.newBufferedWriter(Paths.get("csv/remaining.csv"), Charset.forName("UTF-8"), StandardOpenOption.CREATE)
    try {
        val remainingStringsCsvWriter = CSVWriter(remainingStringsFileWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)
        val header = arrayOf("android key", "iOS key", "android value", "iOS value")
        remainingStringsCsvWriter.writeNext(header)

        androidStringsPool.forEach { key, value ->
            remainingStringsCsvWriter.writeNext(arrayOf(key, "", value, ""))
        }

        iOsStringsPool.forEach { key, value ->
            remainingStringsCsvWriter.writeNext(arrayOf("", key, "", value))
        }
    } finally {
        remainingStringsFileWriter.flush()
    }
}

