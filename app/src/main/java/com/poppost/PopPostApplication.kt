package com.poppost

import android.app.Application
import com.poppost.data.local.AppDatabase
import com.poppost.data.repository.PostRepository

/**
 * Ponto central de DI manual.
 * O [PostRepository] é criado uma vez e reutilizado por toda a app.
 */
class PopPostApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { PostRepository(database.postDao()) }
}

