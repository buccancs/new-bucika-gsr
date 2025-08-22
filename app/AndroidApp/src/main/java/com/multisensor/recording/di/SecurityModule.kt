package com.multisensor.recording.di

import android.content.Context
import com.multisensor.recording.security.EncryptedFileManager
import com.multisensor.recording.security.PrivacyManager
import com.multisensor.recording.security.SecureLogger
import com.multisensor.recording.security.SecurityUtils
import com.multisensor.recording.util.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurityUtils(
        @ApplicationContext context: Context,
        logger: Logger
    ): SecurityUtils {
        return SecurityUtils(context, logger)
    }

    @Provides
    @Singleton
    fun provideEncryptedFileManager(
        @ApplicationContext context: Context,
        securityUtils: SecurityUtils,
        logger: Logger
    ): EncryptedFileManager {
        return EncryptedFileManager(context, securityUtils, logger)
    }

    @Provides
    @Singleton
    fun providePrivacyManager(
        @ApplicationContext context: Context,
        logger: Logger
    ): PrivacyManager {
        return PrivacyManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideSecureLogger(
        @ApplicationContext context: Context,
        baseLogger: Logger,
        securityUtils: SecurityUtils
    ): SecureLogger {
        return SecureLogger(context, baseLogger, securityUtils)
    }
}
