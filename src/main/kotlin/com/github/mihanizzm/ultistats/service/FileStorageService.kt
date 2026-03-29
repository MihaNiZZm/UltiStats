
import org.springframework.web.multipart.MultipartFile

interface FileStorageService {
    fun upload(file: MultipartFile?): String?

    fun delete(key: String?)

    fun getUrl(key: String?): String?
}