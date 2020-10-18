package com.example.retrofittest

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.retrofittest.repository.GithubRepository

class FirstViewModel(
    private val githubRepository: GithubRepository
): ViewModel() {
    suspend fun test() {
        Log.d("Test", "FirstViewModel.test invoke repository before")
        try {
            val result = githubRepository.getSearchRepositories()
            Log.d("Test", "FirstViewModel.test invoke repository after $result")
        } catch (e: MyException) {
            Log.d("Test", "FirstViewModel.test MyException!!")
        }
    }
}
