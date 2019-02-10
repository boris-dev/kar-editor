package ru.adhocapp.json

import org.junit.jupiter.api.Test
import java.io.File
import java.io.InputStream

internal class JsonWithLocalLinksAndFilesInZipTest {

    @Test
    fun zipFile() {

        JsonWithLocalLinksAndFilesInZip("vocaberry", File("src\\test\\resources\\vocaberry.json").inputStream()).zipFile()
    }


}