package com.back.says.dto

data class PageResult<T>(
    val items: List<T>,
    val currentPage: Int,
    val totalPages: Int,
    val pageSize: Int = 5,
    val totalItems: Int? = null
)