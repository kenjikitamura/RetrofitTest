package com.example.retrofittest.repository

import com.example.retrofittest.api.GithubApi
import com.example.retrofittest.entity.SearchRepositoriesResult
import retrofit2.HttpException

class GithubRepository(private val githubApi: GithubApi) {
    suspend fun getSearchRepositories() : SearchRepositoriesResult {
        return try {
            githubApi.getSearchRepositories("kotlin")
        } catch (e: HttpException) {
            return SearchRepositoriesResult(999, emptyList())
        }
    }
}