package ru.adhocapp.json

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import org.apache.commons.codec.binary.Base64
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path


class JsonWithLocalLinksAndFilesInZip(private val fileName: String, private val inputStream: InputStream) {
    fun zipFile(): File {
        val tempDirectory = Files.createTempDirectory("temp")
        try {
            val json = JsonPath.parse(inputStream)
            val lesCount = json.read<Int>("$.lessons.length()")
            println(lesCount)

            for (i in 0 until lesCount) {
                val photoLinkWithBase64 = changeBase64PhotoToLink(json, i)
                photoLinkWithBase64.addToDirectory(tempDirectory, "photo")
                val exCount = json.read<Int>("$.lessons[$i].exercises.length()")
                for (j in 0 until exCount) {
                    val midiLinkWithBase64 = changeBase64MidiToLink(json, i, j)
                    midiLinkWithBase64.addToDirectory(tempDirectory, "midi")
                }
            }
            println(json.jsonString().take(1000))
            File("$tempDirectory/vocaberry.json").writeText(json.jsonString())

            val zipFile = File.createTempFile("temp", "$fileName.zip")
            zipFile.parentFile.mkdirs()
            ZipUtil.pack(tempDirectory.toFile(), zipFile)
            return zipFile
        } finally {
            tempDirectory.toFile().deleteRecursively()
        }

    }


    private fun changeBase64MidiToLink(json: DocumentContext, i: Int, j: Int): LinkWithBase64 {
        val fileName = "lesson_${i}_exercise_$j.mid"
        val jayPath = "$.lessons[$i].exercises[$j]"
        val jayPathBase64 = "$jayPath.midiBase64"
        val midiBase64Value = json.read<String>(jayPathBase64)
        json.delete(jayPathBase64)
        json.put(jayPath, "midiLocalLink", "midi/$fileName")
        return LinkWithBase64(fileName, midiBase64Value)
    }

    private fun changeBase64PhotoToLink(json: DocumentContext, i: Int): LinkWithBase64 {
        val fileName = "lesson_$i.png"
        val jayPath = "$.lessons[$i]"
        val jayPathBase64 = "$jayPath.photoBase64"
        val photoBase64Value = json.read<String>(jayPathBase64)
        json.delete(jayPathBase64)
        json.put(jayPath, "photoLocalLink", "photo/$fileName")
        return LinkWithBase64(fileName, photoBase64Value)
    }


}

data class LinkWithBase64(val fileName: String, val base64: String) {
    fun addToDirectory(tempDirectory: Path, folder: String) {
        val byteArray = Base64().decode(base64.toByteArray())
        val file = File("$tempDirectory/$folder/$fileName")
        file.parentFile.mkdirs()
        file.writeBytes(byteArray)
    }

}