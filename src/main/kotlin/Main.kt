package com.back

import com.back.says.controller.SaysController
import com.back.says.repository.SaysRepository
import com.back.says.service.SaysService

fun main() {
    val repository = SaysRepository()
    val service = SaysService(repository)
    val controller = SaysController(service)

    println("== 명언 앱 ==")
    controller.run()
}
