package com.example.retrofittest.repository

import android.util.Log
import com.example.retrofittest.api.GithubApi
import com.example.retrofittest.entity.SearchRepositoriesResult
import retrofit2.HttpException

class GithubRepository(val githubApi: GithubApi) {
    suspend fun getSearchRepositories() : SearchRepositoriesResult {
        try {
            return githubApi.getSearchRepositories("kotlin")
        } catch (e: HttpException) {
            Log.e("Test", "error!!!", e)
        }
        return SearchRepositoriesResult(999, emptyList())
    }
}