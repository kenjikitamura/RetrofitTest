package com.example.retrofittest

import android.app.Application
import android.util.Log
import com.example.retrofittest.api.GithubApi
import com.example.retrofittest.entity.SearchRepositoriesResult
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
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.Exception
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.jvm.kotlinFunction

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
            //val api = Converter<GithubApi>().conv(retrofit, GithubApi::class.java)
            val origApi = retrofit.create(GithubApi::class.java)
            val api = proxy(origApi, GithubApi::class.java) { method, arguments ->
                Log.d("Test", "ProxyBody start $method ${arguments}")
                val ret = method.invoke(origApi, *arguments.toTypedArray())
                Log.d("Test", "ProxyBody end ${ret}")
                ret
            }
            GithubRepository(api)
        }

        viewModel { FirstViewModel(get()) }
    }

    private interface SuspendFunction {
        suspend fun invoke(): Any?
    }

    private val SuspendRemover = SuspendFunction::class.java.methods[0]

    @Suppress("UNCHECKED_CAST")
    fun <C : Any> proxy(target: Any, contract: Class<C>, invoker: SuspendInvoker): C =
        Proxy.newProxyInstance(contract.classLoader, arrayOf(contract)) { _, method, arguments ->
            Log.d("Test", "Proxy 1 $method")
            val continuation = arguments.last() as Continuation<Any?>
            val argumentsWithoutContinuation = arguments.take(arguments.size -1)

            val a = runBlocking {
                SearchRepositoriesResult(999, emptyList())
            }

            Log.d("Test", "Proxy end")
            a
        } as C

    interface Adder {
        suspend fun add(a: Int, b: Int): Int
    }
}

suspend fun Method.invokeSuspend(obj: Any, vararg args: Any?): Any? =
    suspendCoroutine { cont ->
        invoke(obj, *args, cont)
    }

class MyException(e: Throwable): RuntimeException(e)

typealias SuspendInvoker = suspend (method: Method, arguments: List<Any?>) -> Any?