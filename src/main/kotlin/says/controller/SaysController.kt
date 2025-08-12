package com.back.says.controller

import com.back.says.service.SaysService

class SaysController(private val saysService: SaysService) {

    /** 콘솔 앱 메인 루프 실행 */
    fun run() {
        while (true) {
            print("\n명령) ")
            val raw = readlnOrNull()?.trim() ?: return

            when {
                raw == "종료" -> return
                raw == "등록" -> addSays()
                raw == "빌드" -> buildJsonFile()
                raw.startsWith("삭제?id=") -> deleteByQuery(raw)
                raw.startsWith("수정?id=") -> updateByQuery(raw)
                raw == "목록" -> listSays(page = 1, keywordType = null, keyword = null)
                raw.startsWith("목록") -> listCommand(raw)
                else -> println("알 수 없는 명령입니다.")
            }
        }
    }

    /** 목록 명령 쿼리 파싱 후 목록 출력 */
    private fun listCommand(input: String) {
        if (!input.contains("?")) {
            listSays(page = 1, keywordType = null, keyword = null)
            return
        }
        val q = input.substringAfter("?", "")
            .split("&")
            .mapNotNull { it.substringBefore("=", "").takeIf(String::isNotBlank)?.let { k ->
                k to input.substringAfter("$k=", "").substringBefore("&")
            } }
            .toMap()

        val page = q["page"]?.toIntOrNull() ?: 1
        val keywordType = q["keywordType"]
        val keyword = q["keyword"]

        listSays(page = page, keywordType = keywordType, keyword = keyword)
    }

    /** 페이징 및 선택적 검색조건으로 명언 목록 출력 */
    private fun listSays(page: Int, keywordType: String?, keyword: String?) {
        if (!keywordType.isNullOrBlank() && !keyword.isNullOrBlank()) {
            println(
                """
                ----------------------
                검색타입 : $keywordType
                검색어 : $keyword
                ----------------------
                """.trimIndent()
            )
        }

        println("번호 / 작가 / 명언")
        println("----------------------")

        val result = saysService.listPaged(page = page, pageSize = 5, keywordType = keywordType, keyword = keyword)
        result.items.forEach { println("${it.id} / ${it.author} / ${it.content}") }

        println("----------------------")
        println(buildPagesLine(result.currentPage, result.totalPages))
    }

    /** 신규 명언 등록 */
    private fun addSays() {
        val content = ask("명언")
        val author = ask("작가")
        val says = saysService.createSays(author, content)
        println("${says.id}번 명언이 등록되었습니다.")
    }

    /** 쿼리에서 ID 추출 후 삭제 수행 */
    private fun deleteByQuery(raw: String) {
        val id = raw.substringAfter("삭제?id=").toLongOrNull()
        if (id == null) {
            println("id를 정확히 입력해주세요.")
            return
        }
        deleteSays(id)
    }

    /** ID로 명언 삭제 */
    private fun deleteSays(id: Long) {
        if (saysService.deleteSaysById(id)) {
            println("${id}번 명언이 삭제되었습니다.")
        } else {
            println("${id}번 명언은 존재하지 않습니다.")
        }
    }

    /** 쿼리에서 ID 추출 후 수정 수행 */
    private fun updateByQuery(raw: String) {
        val id = raw.substringAfter("수정?id=").toLongOrNull()
        if (id == null) {
            println("id를 정확히 입력해주세요.")
            return
        }
        updateSays(id)
    }

    /** ID로 명언 수정 */
    private fun updateSays(id: Long) {
        val says = saysService.getSaysById(id)
        if (says == null) {
            println("${id}번 명언은 존재하지 않습니다.")
            return
        }

        println("명언(기존) : ${says.content}")
        val content = ask("명언")

        println("작가(기존) : ${says.author}")
        val author = ask("작가")

        if (saysService.updateSays(id, author, content)) {
            println("${id}번 명언이 수정되었습니다.")
        } else {
            println("${id}번 명언 수정 중 오류가 발생했습니다.")
        }
    }

    /** 모든 명언 data.json 내보내기 */
    private fun buildJsonFile() {
        if (saysService.exportToJsonFile("data.json")) {
            println("data.json 파일의 내용이 갱신되었습니다.")
        } else {
            println("파일 저장 중 오류가 발생했습니다.")
        }
    }

    /** “라벨 : ” 프롬프트 출력 및 입력값 에코 후 반환 */
    private fun ask(label: String): String {
        println("$label : ")
        val value = readlnOrNull().orEmpty()
        println("$label : $value")
        return value
    }

    /** 페이지 라인을 ‘[현재 페이지] / ...’ 형식으로 만든다 */
    private fun buildPagesLine(current: Int, total: Int): String =
        buildString {
            append("페이지 : ")
            for (p in 1..total) {
                if (p > 1) append(" / ")
                if (p == current) append("[$p]") else append(p)
            }
        }
}
