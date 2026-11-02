package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StationAccessTest {

    @Test
    fun elevatorWinsOverEverythingElse() {
        assertEquals(AccessWay.ELEVATOR, StationAccess(elevators = 2, escalators = 4, ramps = 1, stairs = 4).way)
    }

    @Test
    fun maracanauGoesByLift() {
        assertEquals(AccessWay.LIFT, StationAccess(elevators = 0, escalators = 0, ramps = 2, stairs = 2, lifts = 2).way)
    }

    @Test
    fun streetLevelStationGoesByRamp() {
        assertEquals(AccessWay.RAMP, StationAccess(elevators = 0, escalators = 0, ramps = 1, stairs = 2).way)
    }

    @Test
    fun onlyStairsNeedsStaff() {
        assertEquals(AccessWay.STAIRS_ONLY, StationAccess(elevators = 0, escalators = 0, ramps = 0, stairs = 2).way)
    }
}
