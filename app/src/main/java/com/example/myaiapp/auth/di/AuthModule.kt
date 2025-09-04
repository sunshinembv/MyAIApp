package com.example.myaiapp.auth.di

import com.example.myaiapp.auth.presentation.AuthViewModel
import com.example.myaiapp.auth.presentation.state.AuthActor
import com.example.myaiapp.auth.presentation.state.AuthReducer
import com.example.myaiapp.auth.presentation.state.AuthState
import dagger.Module
import dagger.Provides

@Module
class AuthModule {

    @Provides
    @AuthScope
    fun provideAuthViewModelAssistedFactory(
        reducer: AuthReducer,
        actor: AuthActor,
    ): AuthViewModel.AssistedFactory {
        return AuthViewModel.AssistedFactory(reducer, actor)
    }

    @Provides
    @AuthScope
    fun provideAuthReducer(): AuthReducer {
        return AuthReducer(state = AuthState())
    }
}
