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


    println("Looking for identical strings ...")
    //find identical strings
    val identicalStrings = mutableMapOf<StringKey, String>()
    androidStrings.forEach { androidKey, androidValue ->
        var key: StringKey? = null
        val value: String = androidValue

        iosStrings.forEach loop@{ iosKey, iOsValue ->
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
        iosStringsPool.remove(key.iosKey)
    }

//    val identicalStringsCsvFile = createCsvFile("identical.csv")
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


    //find similar strings
}

//private fun createCsvFile(fileName: String): File {
//    val file = File("csv", fileName)
//    if (file.exists()) file.delete()
//    file.createNewFile()
//    return file
//}

fun String.toAndroidString(): String {
    return this.replace("$@", "\$s")
}