package says.service

import com.back.main
import org.junit.jupiter.api.*
import java.io.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText

@TestMethodOrder(MethodOrderer.DisplayName::class)
class ServiceTest {

    private val dbDir: Path = Paths.get("db", "wiseSaying")

    @BeforeEach fun cleanBefore() = deleteRecursively(dbDir.toFile())
    @AfterEach  fun cleanAfter()  = deleteRecursively(dbDir.toFile())

    private fun runApp(vararg lines: String): String {
        val input = lines.joinToString("\n") + "\n"
        val oldIn = System.`in`
        val oldOut = System.out
        try {
            System.setIn(ByteArrayInputStream(input.toByteArray(Charsets.UTF_8)))
            val baos = ByteArrayOutputStream()
            System.setOut(PrintStream(baos, true, Charsets.UTF_8))
            main()
            return baos.toString(Charsets.UTF_8)
        } finally {
            System.setIn(oldIn)
            System.setOut(oldOut)
        }
    }

    private fun deleteRecursively(file: File?) {
        if (file == null || !file.exists()) return
        if (file.isDirectory) file.listFiles()?.forEach { deleteRecursively(it) }
        file.delete()
    }

    @Test @DisplayName("01단계 - 종료")
    fun step01_exit() {
        val out = runApp("종료")
        out shouldContain "== 명언 앱 =="
        out shouldContain "명령) "
    }

    @Test @DisplayName("02단계 - 등록 입력 흐름")
    fun step02_register_input_flow() {
        val out = runApp(
            "등록",
            "현재를 사랑하라.",
            "작자미상",
            "종료"
        )
        out shouldContain "명령) "
        out shouldContain "명언 : "
        out shouldContain "작가 : "
    }

    @Test @DisplayName("03단계 - 등록시 번호 노출")
    fun step03_register_shows_id() {
        val out = runApp("등록", "현재를 사랑하라.", "작자미상", "종료")
        out shouldContain "1번 명언이 등록되었습니다."
    }

    @Test @DisplayName("04단계 - 등록시 번호 증가")
    fun step04_incrementing_ids() {
        val out = runApp(
            "등록", "현재를 사랑하라.", "작자미상",
            "등록", "과거에 집착하지 마라.", "작자미상",
            "종료"
        )
        out shouldContain "1번 명언이 등록되었습니다."
        out shouldContain "2번 명언이 등록되었습니다."
    }

    @Test @DisplayName("05단계 - 목록 출력 최신순")
    fun step05_list() {
        val out = runApp(
            "등록", "현재를 사랑하라.", "작자미상",
            "등록", "과거에 집착하지 마라.", "작자미상",
            "목록",
            "종료"
        )
        out shouldContainInOrder listOf(
            "번호 / 작가 / 명언",
            "----------------------",
            "2 / 작자미상 / 과거에 집착하지 마라.",
            "1 / 작자미상 / 현재를 사랑하라."
        )
    }

    @Test @DisplayName("06단계 - 삭제")
    fun step06_delete() {
        val out = runApp(
            "등록", "현재를 사랑하라.", "작자미상",
            "등록", "과거에 집착하지 마라.", "작자미상",
            "목록",
            "삭제?id=1",
            "종료"
        )
        out shouldContain "1번 명언이 삭제되었습니다."
    }

    @Test @DisplayName("07단계 - 존재하지 않는 삭제 및 번호 재사용 금지")
    fun step07_delete_not_found_and_no_reuse() {
        val out = runApp(
            "등록", "현재를 사랑하라.", "작자미상",
            "등록", "과거에 집착하지 마라.", "작자미상",
            "삭제?id=1",
            "삭제?id=1",
            "등록", "새 명언", "새 작가",
            "목록",
            "종료"
        )
        out shouldContain "1번 명언이 삭제되었습니다."
        out shouldContain "1번 명언은 존재하지 않습니다."
        out shouldContain "3 / 새 작가 / 새 명언"
    }

    @Test @DisplayName("08단계 - 수정 플로우")
    fun step08_modify() {
        val out = runApp(
            "등록", "현재를 사랑하라.", "작자미상",
            "등록", "과거에 집착하지 마라.", "작자미상",
            "삭제?id=1",
            "수정?id=3",
            "수정?id=2",
            "현재와 자신을 사랑하라.",
            "홍길동",
            "목록",
            "종료"
        )
        out shouldContain "3번 명언은 존재하지 않습니다."
        out shouldContainInOrder listOf(
            "명언(기존) : 과거에 집착하지 마라.",
            "명언 : 현재와 자신을 사랑하라.",
            "작가(기존) : 작자미상",
            "작가 : 홍길동"
        )
        out shouldContain "2 / 홍길동 / 현재와 자신을 사랑하라."
    }

    @Test @DisplayName("09단계 - 파일 영속성 및 재시작 후 목록")
    fun step09_file_persistence() {
        val out1 = runApp(
            "등록", "현재를 사랑하라.", "작자미상",
            "등록", "과거에 집착하지 마라.", "작자미상",
            "목록",
            "종료"
        )
        out1 shouldContain "2 / 작자미상 / 과거에 집착하지 마라."
        out1 shouldContain "1 / 작자미상 / 현재를 사랑하라."

        val out2 = runApp("목록", "종료")
        out2 shouldContain "2 / 작자미상 / 과거에 집착하지 마라."
        out2 shouldContain "1 / 작자미상 / 현재를 사랑하라."

        Assertions.assertTrue(Paths.get("db/wiseSaying/1.json").exists())
        Assertions.assertTrue(Paths.get("db/wiseSaying/2.json").exists())
        Assertions.assertTrue(Paths.get("db/wiseSaying/lastId.txt").exists())
    }

    @Test @DisplayName("10단계 - 빌드 data.json 생성/갱신")
    fun step10_build_data_json() {
        val out = runApp(
            "등록", "현재를 사랑하라.", "작자미상",
            "등록", "과거에 집착하지 마라.", "작자미상",
            "삭제?id=1",
            "수정?id=2",
            "현재와 자신을 사랑하라.",
            "홍길동",
            "빌드",
            "종료"
        )
        out shouldContain "data.json 파일의 내용이 갱신되었습니다."

        val dataJson = Paths.get("data.json")
        Assertions.assertTrue(dataJson.exists(), "루트에 data.json 생성")

        val json = dataJson.readText()
        json shouldContain "\"id\": 2"
        json shouldContain "\"author\": \"홍길동\""
        json shouldContain "\"content\": \"현재와 자신을 사랑하라.\""

        dataJson.deleteIfExists()
    }

    @Test @DisplayName("13단계 - 검색: content")
    fun step13_search_content() {
        val out = runApp(
            "등록", "현재를 사랑하라.", "작자미상",
            "등록", "과거에 집착하지 마라.", "작자미상",
            "목록?keywordType=content&keyword=과거",
            "종료"
        )
        out shouldContainInOrder listOf(
            "----------------------",
            "검색타입 : content",
            "검색어 : 과거",
            "----------------------",
            "번호 / 작가 / 명언",
            "----------------------",
            "2 / 작자미상 / 과거에 집착하지 마라."
        )
    }

    @Test @DisplayName("13단계 - 검색: author")
    fun step13_search_author() {
        val out = runApp(
            "등록", "현재를 사랑하라.", "작자미상",
            "등록", "과거에 집착하지 마라.", "작자미상",
            "목록?keywordType=author&keyword=작자",
            "종료"
        )
        out shouldContainInOrder listOf(
            "----------------------",
            "검색타입 : author",
            "검색어 : 작자",
            "----------------------",
            "번호 / 작가 / 명언",
            "----------------------",
            "2 / 작자미상 / 과거에 집착하지 마라.",
            "1 / 작자미상 / 현재를 사랑하라."
        )
    }

    @Test @DisplayName("14단계 - 페이징: 1페이지")
    fun step14_paging_page1() {
        val cmds = mutableListOf<String>()
        repeat(10) { i ->
            cmds += "등록"; cmds += "명언 ${i + 1}"; cmds += "작자미상 ${i + 1}"
        }
        cmds += "목록"; cmds += "종료"

        val out = runApp(*cmds.toTypedArray())
        out shouldContainInOrder listOf(
            "번호 / 작가 / 명언",
            "----------------------",
            "10 / 작자미상 10 / 명언 10",
            "9 / 작자미상 9 / 명언 9",
            "8 / 작자미상 8 / 명언 8",
            "7 / 작자미상 7 / 명언 7",
            "6 / 작자미상 6 / 명언 6",
            "----------------------",
            "페이지 : [1] / 2"
        )
    }

    @Test @DisplayName("14단계 - 페이징: 2페이지")
    fun step14_paging_page2() {
        val cmds = mutableListOf<String>()
        repeat(10) { i ->
            cmds += "등록"; cmds += "명언 ${i + 1}"; cmds += "작자미상 ${i + 1}"
        }
        cmds += "목록?page=2"; cmds += "종료"

        val out = runApp(*cmds.toTypedArray())
        out shouldContainInOrder listOf(
            "번호 / 작가 / 명언",
            "----------------------",
            "5 / 작자미상 5 / 명언 5",
            "4 / 작자미상 4 / 명언 4",
            "3 / 작자미상 3 / 명언 3",
            "2 / 작자미상 2 / 명언 2",
            "1 / 작자미상 1 / 명언 1",
            "----------------------",
            "페이지 : 1 / [2]"
        )
    }

    private infix fun String.shouldContain(expected: String) {
        Assertions.assertTrue(
            this.contains(expected),
            "\n[실패] 아래 문자열이 출력에 포함되어야 합니다:\n$expected\n--- 실제 출력 ---\n$this\n"
        )
    }
    private infix fun String.shouldContainInOrder(lines: List<String>) {
        var idx = 0
        lines.forEach { part ->
            val foundAt = this.indexOf(part, idx)
            Assertions.assertTrue(
                foundAt >= 0,
                "\n[실패] 순서 요소를 찾을 수 없음:\n$part\n--- 실제 출력 ---\n$this\n"
            )
            idx = foundAt + part.length
        }
    }
}
