package com.example.vehiclemanager.core.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VehicleDatabase::class.java,
    )

    @Test
    fun migrate5To6AddsVehicleTypeDefaultAndPreservesRows() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO vehicles
                    (id, name, make, model, year, licensePlate, vin, fuelType, primaryOdometerKm)
                VALUES
                    (1, 'Legacy car', 'Make', 'Model', 2024, 'TEST', NULL, 'PETROL', 1000)
                """.trimIndent(),
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)
        database.query("SELECT vehicleType FROM vehicles WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("CAR", cursor.getString(0))
        }
        database.close()
    }

    private companion object {
        const val TEST_DB = "vehicle-manager-migration-test"
    }
}
