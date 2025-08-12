package com.back.says.service

import com.back.says.dto.PageResult
import com.back.says.entity.Says
import com.back.says.repository.SaysRepository
import java.io.File
import kotlin.math.max

class SaysService(private val repository: SaysRepository) {

    /** ID로 명언 단건 조회 */
    fun getSaysById(id: Long): Says? = repository.getById(id)

    /** 선택적 검색 조건 적용 */
    private fun applySearch(keywordType: String?, keyword: String?, source: List<Says>): List<Says> {
        if (keyword.isNullOrBlank() || keywordType.isNullOrBlank()) return source
        return when (keywordType) {
            "author"  -> source.filter { it.author.contains(keyword, ignoreCase = true) }
            "content" -> source.filter { it.content.contains(keyword, ignoreCase = true) }
            else      -> source
        }
    }

    /** 페이징 + 선택적 검색 결과 반환 */
    fun listPaged(
        page: Int?,
        pageSize: Int = 5,
        keywordType: String? = null,
        keyword: String? = null
    ): PageResult<Says> {
        val all = repository.getAll().sortedByDescending { it.id }
        val filtered = applySearch(keywordType, keyword, all)

        val safePage = (page ?: 1).coerceAtLeast(1)
        val totalPages = max(1, (filtered.size + pageSize - 1) / pageSize)

        val start = (safePage - 1) * pageSize
        val end = (start + pageSize).coerceAtMost(filtered.size)
        val items = if (start in 0..filtered.size) filtered.subList(start, end) else emptyList()

        return PageResult(
            items = items,
            currentPage = safePage.coerceAtMost(totalPages),
            totalPages = totalPages,
            pageSize = pageSize,
            totalItems = filtered.size
        )
    }

    /** 새 명언 생성 */
    fun createSays(author: String, content: String): Says {
        val id = repository.genNextId()
        return repository.save(Says(id, author, content))
    }

    /** 명언 수정 */
    fun updateSays(id: Long, author: String, content: String): Boolean =
        repository.update(id, author, content)

    /** 명언 삭제 */
    fun deleteSaysById(id: Long): Boolean =
        repository.deleteById(id)

    /** 모든 명언 data.json 파일화 */
    fun exportToJsonFile(filename: String): Boolean {
        return try {
            val json = repository.getAll()
                .sortedBy { it.id }
                .joinToString(
                    separator = ",\n  ",
                    prefix = "[\n  ",
                    postfix = "\n]"
                ) { s ->
                    """{"id": ${s.id}, "content": "${escape(s.content)}", "author": "${escape(s.author)}"}"""
                }

            // db/download 폴더 경로 생성
            val downloadDir = File("db/download")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            // 지정한 파일명으로 저장 (db/download 내부)
            val targetFile = downloadDir.resolve(filename)
            targetFile.writeText(json)

            true
        } catch (_: Exception) {
            false
        }
    }

    /** JSON 문자열 간단 이스케이프 */
    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")
}
