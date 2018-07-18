package com.schibsted.stringsmerger

import com.opencsv.CSVWriter
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardOpenOption

lateinit var exportFolder: File

fun main(args: Array<String>) {
    exportFolder = File(args[2])

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
    findIdenticalValuesCaseInsensitive(androidStringsPool, iosStringsPool)
    findSimilarValues(androidStringsPool, iosStringsPool)
    writeRemainingValues(androidStringsPool, iosStringsPool)
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

    val file = File(exportFolder, "identical.csv")
    if (file.exists()) file.delete()
    val fileWriter = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"), StandardOpenOption.CREATE)
    try {
        val csvWriter = getCsvWriter(fileWriter)
        val header = arrayOf("android key", "iOS key", "new key", "value")
        csvWriter.writeNext(header)
        identicalStrings.forEach { key, value ->
            csvWriter.writeNext(arrayOf(key.androidKey, key.iosKey, "", value))
        }
    } finally {
        fileWriter.flush()
    }
}

private fun findIdenticalValuesCaseInsensitive(androidStringsPool: MutableMap<String, String>, iOsStringsPool: MutableMap<String, String>) {
    println("Looking for identical strings case insensitive ...")
    val strings = mutableMapOf<StringKey, StringValue>()
    androidStringsPool.forEach { androidKey, androidValue ->

        iOsStringsPool.forEach loop@{ iosKey, iOsValue ->
            if (androidValue.toLowerCase() == iOsValue.toAndroidString().toLowerCase()) {
                val key = StringKey(androidKey, iosKey)
                val value = StringValue(androidValue, iOsValue)
                strings[key] = value
                return@loop
            }
        }
    }

    println("Identical case insensitive strings: ${strings.entries.size}. Separating from the remaining pools.")
    strings.forEach { key, value ->
        androidStringsPool.remove(key.androidKey)
        iOsStringsPool.remove(key.iosKey)
    }

    val file = File(exportFolder, "identical-cis.csv")
    if (file.exists()) file.delete()
    val fileWriter = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"), StandardOpenOption.CREATE)
    try {
        val csvWriter = getCsvWriter(fileWriter)
        val header = arrayOf("android key", "iOS key", "new key", "android value", "iOS value")
        csvWriter.writeNext(header)
        strings.forEach { key, value ->
            csvWriter.writeNext(arrayOf(key.androidKey, key.iosKey, "", value.androidValue, value.iosValue))
        }
    } finally {
        fileWriter.flush()
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

    val file = File(exportFolder, "similar.csv")
    if (file.exists()) file.delete()
    val fileWriter = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"), StandardOpenOption.CREATE)
    try {
        val scvWriter = getCsvWriter(fileWriter)
        val header = arrayOf("android key", "android value", "iOS key", "iOS value")
        scvWriter.writeNext(header)
        similarStrings.forEach { key, value ->
            scvWriter.writeNext(arrayOf(key.androidKey, value.androidValue, key.iosKey, value.iosValue))
        }
    } finally {
        fileWriter.flush()
    }
}

private fun writeRemainingValues(androidStringsPool: Map<String, String>, iOsStringsPool: Map<String, String>) {
    println("Writing remaining strings to csv ...")
    val file = File(exportFolder, "remaining.csv")
    if (file.exists()) file.delete()
    val fileWriter = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"), StandardOpenOption.CREATE)
    try {
        val csvWriter = getCsvWriter(fileWriter)
        val header = arrayOf("android key", "android value", "iOS key", "iOS value")
        csvWriter.writeNext(header)

        androidStringsPool.forEach { key, value ->
            csvWriter.writeNext(arrayOf(key, value, "", ""))
        }

        iOsStringsPool.forEach { key, value ->
            csvWriter.writeNext(arrayOf("", "", key, value))
        }
    } finally {
        fileWriter.flush()
    }
}

fun getCsvWriter(writer: Writer): CSVWriter {
    return CSVWriter(writer, '\t', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)
}

