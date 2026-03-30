package com.github.mihanizzm.ultistats.service

import FileStorageService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID


@Service
@Primary
class LocalFileStorageService : FileStorageService {
    private val root: Path = Paths.get("uploads")

    override fun upload(file: MultipartFile?): String? {
        try {
            Files.createDirectories(root)

            val key: String = UUID.randomUUID().toString() + "_" + file!!.originalFilename
            val path: Path = root.resolve(key)

            Files.copy(file.inputStream, path)

            return "/uploads/$key"
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    override fun delete(key: String?) {
        try {
            Files.deleteIfExists(root.resolve(key))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    override fun getUrl(key: String?): String? {
        return "/uploads/$key"
    }
}
