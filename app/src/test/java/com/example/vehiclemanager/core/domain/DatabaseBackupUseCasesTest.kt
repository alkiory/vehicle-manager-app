package com.example.vehiclemanager.core.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseBackupUseCasesTest {
    @Test
    fun exportAndImportRoundTripPreservesSnapshotAndElectricBicycle() = runBlocking {
        val snapshot = snapshot()
        val source = FakeBackupRepository(snapshot)
        val exported = ExportDatabaseUseCase(source)()
        val destination = FakeBackupRepository(
            VehicleBackupSnapshot(null, emptyList(), emptyList(), emptyList(), emptyList()),
        )

        ImportDatabaseUseCase(destination)(exported)

        assertEquals(snapshot, destination.snapshot)
        assertTrue(exported.contains("\"schemaVersion\":2"))
        assertTrue(exported.contains("\"vehicleType\":\"ELECTRIC_BICYCLE\""))
        assertTrue(exported.contains("\"activeVehicleId\":7"))
    }

    @Test
    fun importSchemaV1FixtureAssignsCarAndPreservesRelationships() = runBlocking {
        val destination = FakeBackupRepository(emptySnapshot())

        ImportDatabaseUseCase(destination)(fixture("backup-v1.json"))

        val imported = destination.snapshot
        assertEquals(7L, imported.activeVehicleId)
        assertEquals(VehicleType.CAR, imported.vehicles.single().vehicleType)
        assertEquals(1, imported.fuelRecords.size)
        assertEquals(1, imported.maintenanceRecords.size)
        assertEquals(1, imported.maintenanceSchedules.size)
        assertEquals(7L, imported.fuelRecords.single().vehicleId)
    }

    @Test
    fun importSharedSchemaV2FixturePreservesElectricBicycle() = runBlocking {
        val destination = FakeBackupRepository(emptySnapshot())

        ImportDatabaseUseCase(destination)(fixture("backup-v2-electric-bicycle.json"))

        val imported = destination.snapshot
        assertEquals(1L, imported.activeVehicleId)
        assertEquals(VehicleType.ELECTRIC_BICYCLE, imported.vehicles.single().vehicleType)
        assertEquals(FuelType.ELECTRIC, imported.vehicles.single().fuelType)
        assertEquals(1L, imported.maintenanceRecords.single().vehicleId)
    }

    @Test
    fun invalidReferenceIsRejectedBeforeRepositoryMutation() = runBlocking {
        val source = FakeBackupRepository(snapshot())
        val exported = ExportDatabaseUseCase(source)()
            .replace("\"vehicleId\":7", "\"vehicleId\":999")
        val destination = FakeBackupRepository(snapshot())

        val error = runCatching { ImportDatabaseUseCase(destination)(exported) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(snapshot(), destination.snapshot)
    }

    @Test
    fun futureSchemaVersionIsRejectedBeforeRepositoryMutation() = runBlocking {
        val exported = ExportDatabaseUseCase(FakeBackupRepository(snapshot()))()
            .replace("\"schemaVersion\":2", "\"schemaVersion\":3")
        val destination = FakeBackupRepository(snapshot())

        val error = runCatching { ImportDatabaseUseCase(destination)(exported) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("Unsupported backup schema version: 3"))
        assertEquals(snapshot(), destination.snapshot)
    }

    @Test
    fun schemaV2MissingNullEmptyBlankAndUnknownVehicleTypesAreRejected() = runBlocking {
        val exported = ExportDatabaseUseCase(FakeBackupRepository(snapshot()))()
        val invalidValues = listOf(
            null,
            "\"\"",
            "\"   \"",
            "\"SPACESHIP\"",
            "\"electric_bicycle\"",
        )

        for (invalid in invalidValues) {
            val json = if (invalid == null) {
                exported.replace(",\"vehicleType\":\"ELECTRIC_BICYCLE\"", "")
            } else {
                exported.replace("\"vehicleType\":\"ELECTRIC_BICYCLE\"", "\"vehicleType\":$invalid")
            }
            val destination = FakeBackupRepository(snapshot())
            val error = runCatching { ImportDatabaseUseCase(destination)(json) }.exceptionOrNull()

            assertTrue("Expected rejection for $invalid", error is IllegalArgumentException)
            assertTrue(error?.message.orEmpty().contains("vehicle type"))
            assertEquals(snapshot(), destination.snapshot)
        }
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/$name")) { "Missing fixture $name" }.readText()

    private fun emptySnapshot() = VehicleBackupSnapshot(
        activeVehicleId = null,
        vehicles = emptyList(),
        fuelRecords = emptyList(),
        maintenanceRecords = emptyList(),
        maintenanceSchedules = emptyList(),
    )

    private fun snapshot() = VehicleBackupSnapshot(
        activeVehicleId = 7,
        vehicles = listOf(
            Vehicle(
                id = 7,
                name = "Electric bicycle",
                make = "Make",
                model = "Model",
                year = 2024,
                licensePlate = "TEST",
                vin = null,
                fuelType = FuelType.ELECTRIC,
                primaryOdometerKm = 12_000,
                vehicleType = VehicleType.ELECTRIC_BICYCLE,
            ),
        ),
        fuelRecords = listOf(
            FuelRecord(
                id = 11,
                vehicleId = 7,
                timestampMs = 100,
                odometerKm = 11_900,
                litersX100 = 4_000,
                pricePerLiterCents = 180,
                totalCostCents = 7_200,
                isFullTank = true,
                stationName = "Station",
                notes = null,
            ),
        ),
        maintenanceRecords = listOf(
            MaintenanceRecord(
                id = 12,
                vehicleId = 7,
                title = "Oil change",
                category = MaintenanceCategory.OIL_CHANGE,
                costCents = 10_000,
                odometerKm = 11_500,
                timestampMs = 200,
                notes = null,
                performedBy = "Garage",
            ),
        ),
        maintenanceSchedules = listOf(
            MaintenanceSchedule(
                id = 13,
                vehicleId = 7,
                serviceTitle = "Oil change",
                intervalKm = 10_000,
                intervalMonths = 12,
                lastPerformedKm = 11_500,
                lastPerformedDateMs = 200,
            ),
        ),
    )

    private class FakeBackupRepository(
        var snapshot: VehicleBackupSnapshot,
    ) : VehicleBackupRepository {
        override suspend fun readSnapshot(): VehicleBackupSnapshot = snapshot

        override suspend fun replaceSnapshot(snapshot: VehicleBackupSnapshot) {
            this.snapshot = snapshot
        }
    }
}
