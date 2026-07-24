package com.opencode.thin.data.repository

import com.opencode.thin.data.model.FileContent
import com.opencode.thin.data.model.FileNode
import com.opencode.thin.data.model.SearchResult
import com.opencode.thin.data.model.VcsInfo
import com.opencode.thin.data.remote.OpencodeApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val api: OpencodeApi,
) {
    suspend fun listFiles(path: String = ""): Result<List<FileNode>> = runCatching {
        api.listFiles(path)
    }

    suspend fun readFile(path: String): Result<FileContent> = runCatching {
        api.readFile(path)
    }

    suspend fun searchText(pattern: String): Result<List<SearchResult>> = runCatching {
        api.searchText(pattern)
    }

    suspend fun findFiles(query: String): Result<List<String>> = runCatching {
        api.findFiles(query)
    }

    suspend fun getVcsInfo(): Result<VcsInfo> = runCatching {
        api.getVcsInfo()
    }

    suspend fun getPath(): Result<com.opencode.thin.data.model.ServerPath> = runCatching {
        api.getPath()
    }
}
