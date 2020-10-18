package com.example.retrofittest

import android.app.Application
import com.example.retrofittest.api.GithubApi
import com.example.retrofittest.repository.GithubRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.RuntimeException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import kotlin.coroutines.suspendCoroutine

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // start Koin!
        startKoin {
            // Android context
            androidContext(this@MyApplication)
            // modules
            modules(myModule)
        }
    }

    private val myModule = module {
        // OkHttp
        single {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }

        // Retrofit
        single {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            Retrofit.Builder()
                .baseUrl("http://api.github.com")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(get())
                .build()
        }

        // GithubApi
        single {
            val retrofit = get<Retrofit>()
            val origApi = retrofit.create(GithubApi::class.java)
            val proxyApi = proxy(GithubApi::class.java) { method, arguments ->
                try {
                    method.invokeSuspend(origApi, *arguments.toTypedArray())
                } catch (e: Throwable) {
                    throw MyException(e)
                }
            }
            GithubRepository(proxyApi)
        }

        viewModel { FirstViewModel(get()) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <C : Any> proxy(contract: Class<C>, invoker: SuspendInvoker): C =
        Proxy.newProxyInstance(contract.classLoader, arrayOf(contract)) { _, method, arguments ->
            runBlocking {
                val argumentsWithoutContinuation = arguments.take(arguments.size -1)
                invoker.invoke(method, argumentsWithoutContinuation)
            }
        } as C
}

suspend fun Method.invokeSuspend(obj: Any, vararg args: Any?): Any? =
    suspendCoroutine { cont ->
        invoke(obj, *args, cont)
    }

class MyException(e: Throwable): RuntimeException(e)

typealias SuspendInvoker = suspend (method: Method, arguments: List<Any?>) -> Any?