package com.example.retrofittest.api

import com.example.retrofittest.entity.SearchRepositoriesResult
import retrofit2.http.GET
import retrofit2.http.Query

interface GithubApi {
    /**
     * https://docs.github.com/en/free-pro-team@latest/rest/reference/search#search-repositories
     */
    @GET("/search/repositories")
    suspend fun getSearchRepositories(
        @Query("q") query: String
    ) : SearchRepositoriesResult
}