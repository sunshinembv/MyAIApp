package com.example.myaiapp.auth.di

import android.content.Context
import com.example.myaiapp.auth.presentation.AuthViewModel
import dagger.Component

@Component(
    modules = [
        AuthModule::class,
    ],
    dependencies = [AuthDeps::class]
)
@AuthScope
interface AuthComponent {

    @Component.Factory
    interface Factory {
        fun create(
            authDeps: AuthDeps
        ): AuthComponent
    }

    fun getAuthViewModelAssistedFactory(): AuthViewModel.AssistedFactory
}

interface AuthDeps {
    fun context(): Context
}
