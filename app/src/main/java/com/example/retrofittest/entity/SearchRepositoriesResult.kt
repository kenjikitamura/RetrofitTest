package com.example.retrofittest.entity

import com.squareup.moshi.Json

data class SearchRepositoriesResult(
    @Json(name = "total_count")
    val totalCount: Int,
    val items : List<SearchRepositoryItem>
)

data class SearchRepositoryItem (
    val id: Int,
    val name: String
)