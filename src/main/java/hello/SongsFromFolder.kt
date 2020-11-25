package hello

import com.google.gson.Gson
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FilenameUtils
import java.io.File

class SongsFromFolder(val recent: File) {
    fun json(): String {
        recent.mkdir()
        return Gson().toJson(
                recent.listFiles()
                        .filter { it.isFile }
                        .filter { it.extension.startsWith("kar") || it.extension.startsWith("mid") }
                        .sortedBy { it.lastModified() }
                        .reversed()
                        .take(50)
                        .map { Song(wellformName(it.name), base64(it)) }
                        .toList()
        ).toString()
    }

    fun wellformName(fileName: String): String {
        var result = FilenameUtils.removeExtension(fileName)
        result += ".kar"
        return result
    }

    fun base64(file: File): String {
        val finput = file.inputStream()
        val bytes = ByteArray(file.length().toInt())
        finput.read(bytes, 0, bytes.size)
        finput.close()
        return Base64.encodeBase64String(bytes)
    }
}


data class Song(val name: String, val base64: String)