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
                Log.d("Test", "invoke before $method ${arguments}")
                val ret = method.invoke(origApi, *arguments.toTypedArray())
                Log.d("Test", "invoke after ${ret}")
                ret
            }
            GithubRepository(api)
        }

        viewModel { FirstViewModel(get()) }
    }

    /*
    class Converter<T> {
        @Suppress("UNCHECKED_CAST")
        fun conv(retrofit: Retrofit, clazz: Class<T>): T {
            val api = retrofit.create(clazz)
            val proxy = Proxy.newProxyInstance(
                clazz.classLoader,
                arrayOf(clazz),
                Handler(api)
            )

            return proxy as? T ?: throw IllegalStateException("proxy cannot cast target class type.")
        }

        inner class Handler(private val obj: T): InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                Log.d("Test", "Before method=$method args=$args")
                try {
                    val ret = if (args == null) {
                        method!!.invoke(obj)
                    } else {
                        method!!.invoke(obj, *args)
                    }
                    Log.d("Test", "After $ret")
                    return ret
                } catch (e: Throwable) {
                    throw MyException(e)
                }
            }
        }
    }
     */

    private interface SuspendFunction {
        suspend fun invoke(): Any?
    }

    private val SuspendRemover = SuspendFunction::class.java.methods[0]

    @Suppress("UNCHECKED_CAST")
    fun <C : Any> proxy(target: Any, contract: Class<C>, invoker: SuspendInvoker): C =
        Proxy.newProxyInstance(contract.classLoader, arrayOf(contract)) { _, method, arguments ->
            Log.d("Test", "proxy 1")
            val continuation = arguments.last() as Continuation<*>
            val argumentsWithoutContinuation = arguments.take(arguments.size)
            Log.d("Test", "proxy 2 arguments=${arguments.size} $arguments")
            val a = try {
                val ret = SuspendRemover.invoke(object : SuspendFunction {
                    override suspend fun invoke(): C {
                        Log.d("Test", "before invoker")
                        val a = try {
                            Log.d("Test", "before invoker 1 method=$method")
                            val c = suspendCoroutine<C> {
                                val b = invoker(method, argumentsWithoutContinuation) as C
                            }

                            Log.d("Test", "before invoker 2")
                            c
                        } catch (e: Throwable) {
                            Log.d("Test", "error invoker $e")
                            throw MyException(e)
                        }finally {
                            Log.d("Test", "before invoker 3")
                        }
                        Log.d("Test", "after invoker $a")
                        return a
                    }
                }, continuation)
                Log.d("Test", "proxy 2 invoke Success! ret=${ret.javaClass.name}")
                // val ret = method.invoke(target, *argumentsWithoutContinuation.toTypedArray())
                ret
            } catch (e: Exception) {
                Log.d("Test", "proxy 2 invoke Fail! $e")
                val e2 = if (e is InvocationTargetException) e.cause ?: e else e
                throw MyException(e2)
            }
            Log.d("Test", "proxy 3")
            a
        } as C

    interface Adder {
        suspend fun add(a: Int, b: Int): Int
    }
}

class MyException(e: Throwable): RuntimeException(e)

typealias SuspendInvoker = suspend (method: Method, arguments: List<Any?>) -> Any?