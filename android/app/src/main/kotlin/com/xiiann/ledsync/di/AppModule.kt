package com.xiiann.ledsync.di

import com.xiiann.ledsync.data.executor.IRootExecutor
import com.xiiann.ledsync.data.executor.RootExecutor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindRootExecutor(
        rootExecutor: RootExecutor
    ): IRootExecutor
}
