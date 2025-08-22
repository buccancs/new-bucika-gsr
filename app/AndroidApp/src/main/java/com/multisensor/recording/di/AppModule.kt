package com.multisensor.recording.di

import com.multisensor.recording.controllers.ControllerConnectionManager
import com.multisensor.recording.controllers.PcControllerConnectionManager
import com.multisensor.recording.handsegmentation.HandSegmentationAdapter
import com.multisensor.recording.handsegmentation.HandSegmentationInterface
import com.multisensor.recording.handsegmentation.HandSegmentationManager
import com.multisensor.recording.security.PrivacyInterface
import com.multisensor.recording.security.PrivacyManager
import com.multisensor.recording.security.PrivacyManagerAdapter
import com.multisensor.recording.streaming.NetworkPreviewStreamer
import com.multisensor.recording.streaming.PreviewStreamingInterface
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindControllerConnectionManager(
        pcControllerConnectionManager: PcControllerConnectionManager
    ): ControllerConnectionManager

    @Binds
    @Singleton
    abstract fun bindPreviewStreamingInterface(
        networkPreviewStreamer: NetworkPreviewStreamer
    ): PreviewStreamingInterface

    companion object {
        @Provides
        @Singleton
        fun provideHandSegmentationInterface(
            handSegmentationManager: HandSegmentationManager
        ): HandSegmentationInterface {
            return HandSegmentationAdapter(handSegmentationManager)
        }

        @Provides
        @Singleton
        fun providePrivacyInterface(
            privacyManager: PrivacyManager
        ): PrivacyInterface {
            return PrivacyManagerAdapter(privacyManager)
        }
    }
}
