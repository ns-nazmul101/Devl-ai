package com.example.control

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FileItemInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

data class FileOperationResult(
    val success: Boolean,
    val message: String,
    val fileCount: Int = 0
)

class FileManager(private val context: Context) {

    private fun getBaseDirectory(): File {
        val externalStorage = Environment.getExternalStorageDirectory()
        return if (externalStorage != null && externalStorage.canWrite()) {
            externalStorage
        } else {
            context.getExternalFilesDir(null) ?: context.filesDir
        }
    }

    suspend fun createFolder(folderName: String): FileOperationResult = withContext(Dispatchers.IO) {
        val sanitized = folderName.trim().replace("/", "").replace("..", "")
        if (sanitized.isEmpty()) {
            return@withContext FileOperationResult(false, "Invalid folder name.")
        }

        val base = getBaseDirectory()
        val target = File(base, sanitized)
        if (target.exists()) {
            return@withContext FileOperationResult(true, "Folder \"$sanitized\" already exists at ${target.absolutePath}")
        }

        val created = target.mkdirs()
        if (created) {
            FileOperationResult(true, "Created folder \"$sanitized\" successfully at ${target.absolutePath}")
        } else {
            // Fallback to Documents folder or app documents
            val docDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: base
            val docTarget = File(docDir, sanitized)
            if (docTarget.mkdirs()) {
                FileOperationResult(true, "Created folder \"$sanitized\" in app storage: ${docTarget.absolutePath}")
            } else {
                FileOperationResult(false, "Could not create folder. Storage permission may be required.")
            }
        }
    }

    suspend fun searchFiles(query: String): List<FileItemInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItemInfo>()
        val base = getBaseDirectory()
        val normalizedQuery = query.lowercase().trim()

        fun searchRecursive(dir: File, depth: Int) {
            if (depth > 4 || !dir.canRead() || results.size >= 50) return
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.name.lowercase().contains(normalizedQuery)) {
                    results.add(
                        FileItemInfo(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = file.isDirectory,
                            sizeBytes = if (file.isFile) file.length() else 0L,
                            lastModified = file.lastModified()
                        )
                    )
                }
                if (file.isDirectory && !file.name.startsWith(".")) {
                    searchRecursive(file, depth + 1)
                }
            }
        }

        try {
            searchRecursive(base, 0)
        } catch (_: Exception) {}

        results
    }

    suspend fun listDirectory(dirPath: String? = null): List<FileItemInfo> = withContext(Dispatchers.IO) {
        val dir = if (dirPath != null) File(dirPath) else getBaseDirectory()
        val files = dir.listFiles() ?: emptyArray()
        files.map {
            FileItemInfo(
                name = it.name,
                path = it.absolutePath,
                isDirectory = it.isDirectory,
                sizeBytes = if (it.isFile) it.length() else 0L,
                lastModified = it.lastModified()
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun deleteFileOrFolder(path: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val target = File(path)
            if (!target.exists()) {
                return@withContext FileOperationResult(false, "Target path does not exist: $path")
            }
            val deleted = if (target.isDirectory) {
                target.deleteRecursively()
            } else {
                target.delete()
            }
            if (deleted) {
                FileOperationResult(true, "Successfully deleted: ${target.name}")
            } else {
                FileOperationResult(false, "Could not delete: ${target.name}. Check file permissions.")
            }
        } catch (e: Exception) {
            FileOperationResult(false, "Error deleting file: ${e.localizedMessage}")
        }
    }
}
