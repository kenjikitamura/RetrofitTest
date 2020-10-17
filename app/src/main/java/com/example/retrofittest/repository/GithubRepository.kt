package com.example.retrofittest.repository

import com.example.retrofittest.api.GithubApi
import com.example.retrofittest.entity.SearchRepositoriesResult

class GithubRepository(val githubApi: GithubApi) {
    suspend fun getSearchRepositories() : SearchRepositoriesResult {
        return githubApi.getSearchRepositories("kotlin")
    }
}