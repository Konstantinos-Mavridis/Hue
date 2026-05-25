package com.hue.data.pantone.di

import android.content.Context
import com.google.gson.Gson
import com.hue.data.pantone.db.HueDatabase
import com.hue.data.pantone.repository.HistoryRepositoryImpl
import com.hue.data.pantone.repository.PantoneRepositoryImpl
import com.hue.data.pantone.seeding.PantoneDatabaseSeeder
import com.hue.domain.repository.HistoryRepository
import com.hue.domain.repository.PantoneRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        seeder: PantoneDatabaseSeeder
    ): HueDatabase = HueDatabase.create(context, seeder)

    @Provides
    @Singleton
    fun providePantoneDatabaseSeeder(
        dbProvider: Provider<HueDatabase>
    ): PantoneDatabaseSeeder = PantoneDatabaseSeeder(dbProvider)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPantoneRepository(impl: PantoneRepositoryImpl): PantoneRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}
