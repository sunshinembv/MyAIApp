package com.example.myaiapp.data_provider.di

import com.example.myaiapp.data_provider.DataProvider
import com.example.myaiapp.data_provider.DataProviderImpl
import dagger.Binds
import dagger.Module

@Module
interface DataProviderModule {

    @Binds
    fun bindDataProvider(impl: DataProviderImpl): DataProvider
}
