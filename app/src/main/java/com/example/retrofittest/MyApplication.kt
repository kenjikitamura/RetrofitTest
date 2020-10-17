package com.example.retrofittest

import android.app.Application
import android.util.Log
import com.example.retrofittest.api.GithubApi
import com.example.retrofittest.repository.GithubRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit

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
            val api = Converter<GithubApi>().conv(retrofit, GithubApi::class.java)
            GithubRepository(api)
        }

        viewModel { FirstViewModel(get()) }
    }

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
}

class MyException(e: Throwable): Exception(e)