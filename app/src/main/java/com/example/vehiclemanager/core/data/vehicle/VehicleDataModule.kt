package com.example.vehiclemanager.core.data.vehicle

import com.example.vehiclemanager.core.domain.VehicleDeletionRepository
import com.example.vehiclemanager.core.domain.VehicleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VehicleDataModule {
    @Binds
    @Singleton
    abstract fun bindVehicleRepository(
        implementation: VehicleRepositoryImpl,
    ): VehicleRepository

    @Binds
    @Singleton
    abstract fun bindVehicleDeletionRepository(
        implementation: VehicleDeletionRepositoryImpl,
    ): VehicleDeletionRepository
}
