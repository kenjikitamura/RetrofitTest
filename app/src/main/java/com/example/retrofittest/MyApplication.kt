package com.example.retrofittest

import android.app.Application
import com.example.retrofittest.api.GithubApi
import com.example.retrofittest.repository.GithubRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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
            val api = retrofit.create(GithubApi::class.java)
            GithubRepository(api)
        }

        viewModel { FirstViewModel(get()) }
    }
}