package com.example.vehiclemanager.core.data.notification

import com.example.vehiclemanager.core.domain.notification.NotificationPermissionChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationDataModule {

    @Binds
    @Singleton
    abstract fun bindNotificationPermissionChecker(
        implementation: NotificationPermissionCheckerImpl,
    ): NotificationPermissionChecker

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        implementation: NotificationRepositoryImpl,
    ): NotificationRepository
}
