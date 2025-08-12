package com.back.says.repository

import com.back.says.entity.Says
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

class SaysRepository(
    private val baseDir: Path = Paths.get("db", "wiseSaying")
) {
    private val lastIdFile: Path = baseDir.resolve("lastId.txt")

    private val saysList = mutableListOf<Says>()
    private var lastId: Long = 0L

    /** 저장소 초기화 및 디스크 데이터 메모리 로드 */
    init {
        if (!baseDir.exists()) baseDir.createDirectories()
        loadAllFromDisk()
        loadLastId()
        if (lastId == 0L) {
            lastId = (saysList.maxOfOrNull { it.id } ?: 0L)
            saveLastId()
        }
    }

    /** 모든 명언 반환 */
    fun getAll(): List<Says> = saysList

    /** ID로 명언 단건 조회 */
    fun getById(id: Long): Says? = saysList.find { it.id == id }

    /** 다음 ID 생성 */
    fun genNextId(): Long = lastId + 1

    /** 명언 저장(신규/수정) 및 디스크 반영 */
    fun save(says: Says): Says {
        val idx = saysList.indexOfFirst { it.id == says.id }
        if (idx >= 0) saysList[idx] = says else saysList.add(says)
        saveSaysFile(says)
        if (says.id > lastId) {
            lastId = says.id
            saveLastId()
        }
        return says
    }

    /** ID로 명언 수정 */
    fun update(id: Long, author: String, content: String): Boolean {
        val s = getById(id) ?: return false
        s.author = author
        s.content = content
        save(s)
        return true
    }

    /** ID로 명언 삭제 및 디스크 파일 제거 */
    fun deleteById(id: Long): Boolean {
        val removed = saysList.removeIf { it.id == id }
        if (removed) {
            saysFile(id).deleteIfExists()
            saveLastId()
        }
        return removed
    }

    /** ID의 JSON 파일 경로 반환 */
    private fun saysFile(id: Long) = baseDir.resolve("$id.json")

    /** 명언 객체 JSON 직렬화 */
    private fun saysToJson(s: Says): String =
        """{"id": ${s.id}, "content": "${escape(s.content)}", "author": "${escape(s.author)}"}"""

    /** JSON 문자열 명언 객체 파싱 */
    private fun parseSaysJson(text: String): Says? {
        val regex = Regex(
            """\{\s*"id"\s*:\s*(\d+)\s*,\s*"content"\s*:\s*"(.*?)"\s*,\s*"author"\s*:\s*"(.*?)"\s*\}""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        val m = regex.find(text) ?: return null
        val id = m.groupValues[1].toLong()
        val content = unescape(m.groupValues[2])
        val author = unescape(m.groupValues[3])
        return Says(id, author, content)
    }

    /** 단일 명언 파일 디스크 저장 */
    private fun saveSaysFile(s: Says) {
        saysFile(s.id).writeText(saysToJson(s))
    }

    /** 모든 명언 파일 로드(디스크→메모리) */
    private fun loadAllFromDisk() {
        if (!baseDir.exists()) return
        saysList.clear()
        baseDir.toFile().listFiles { f -> f.isFile && f.name.matches(Regex("""\d+\.json""")) }
            ?.sortedBy { it.nameWithoutExtension.toLong() }
            ?.forEach { file ->
                runCatching {
                    val txt = file.readText()
                    val s = parseSaysJson(txt)
                    if (s != null) saysList.add(s)
                }
            }
        val unique = saysList.associateBy { it.id }.values.toMutableList()
        saysList.clear()
        saysList.addAll(unique)
    }

    /** lastId 파일에서 읽기 */
    private fun loadLastId() {
        lastId = if (lastIdFile.exists()) {
            lastIdFile.readText().trim().toLongOrNull() ?: 0L
        } else 0L
    }

    /** lastId 파일 저장 */
    private fun saveLastId() {
        if (!baseDir.exists()) baseDir.createDirectories()
        lastIdFile.writeText(lastId.toString())
    }

    /** 문자열 JSON 이스케이프 */
    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    /** JSON 이스케이프 복원 */
    private fun unescape(s: String): String =
        s.replace("\\\"", "\"").replace("\\\\", "\\")
}
