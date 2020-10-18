package com.example.retrofittest.repository

import android.util.Log
import com.example.retrofittest.api.GithubApi
import com.example.retrofittest.entity.SearchRepositoriesResult
import retrofit2.HttpException

class GithubRepository(val githubApi: GithubApi) {
    suspend fun getSearchRepositories() : SearchRepositoriesResult {
        try {
            Log.d("Test", "GithubRepository.getSearchRepositories  invoke API before")
            val ret = githubApi.getSearchRepositories("kotlin")
            Log.d("Test", "GithubRepository.getSearchRepositories  invoke API after API Success!")
            return ret
        } catch (e: HttpException) {
            Log.e("Test", "GithubRepository.getSearchRepositories  invoke API after ERROR", e)
        }
        return SearchRepositoriesResult(999, emptyList())
    }
}