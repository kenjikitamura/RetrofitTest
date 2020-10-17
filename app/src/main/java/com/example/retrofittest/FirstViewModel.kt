package com.example.retrofittest

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.retrofittest.repository.GithubRepository
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class FirstViewModel(
    private val githubRepository: GithubRepository
): ViewModel() {
    suspend fun test() {

        val test = TestImpl()
        val proxy = Proxy.newProxyInstance(
            Test::class.java.classLoader,
            arrayOf(Test::class.java),
            Handler(test)
        ) as Test
        proxy.test()
        val result = githubRepository.getSearchRepositories()
        Log.d("Test", "TEST!!! $result")
    }

    class Handler(val obj: Any): InvocationHandler {
        override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
            Log.d("Test", "Before $args")
            val ret = if (args == null) {
                method!!.invoke(obj)
            } else {
                method!!.invoke(obj, *args)
            }
            Log.d("Test", "After")
            return ret
        }
    }
}

interface Test {
    fun test()
}

class TestImpl: Test {
    override fun test() {
        Log.d("Test","HOGEHOGE!!")
    }
}