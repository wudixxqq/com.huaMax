package com.huaMax.data.geo

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CoordinatePoint(
    val latitude: Double,
    val longitude: Double
)

object CoordinateTransform {
    private const val PI = 3.1415926535897932384626
    private const val AXIS = 6378245.0
    private const val OFFSET = 0.00669342162296594323

    fun wgs84ToGcj02(latitude: Double, longitude: Double): CoordinatePoint {
        if (isOutsideChina(latitude, longitude)) return CoordinatePoint(latitude, longitude)

        var dLat = transformLatitude(longitude - 105.0, latitude - 35.0)
        var dLon = transformLongitude(longitude - 105.0, latitude - 35.0)
        val radLat = latitude / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - OFFSET * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = dLat * 180.0 / ((AXIS * (1 - OFFSET)) / (magic * sqrtMagic) * PI)
        dLon = dLon * 180.0 / (AXIS / sqrtMagic * cos(radLat) * PI)
        return CoordinatePoint(latitude + dLat, longitude + dLon)
    }

    fun gcj02ToWgs84(latitude: Double, longitude: Double): CoordinatePoint {
        if (isOutsideChina(latitude, longitude)) return CoordinatePoint(latitude, longitude)

        val gcj = wgs84ToGcj02(latitude, longitude)
        return CoordinatePoint(
            latitude = latitude * 2 - gcj.latitude,
            longitude = longitude * 2 - gcj.longitude
        )
    }

    private fun isOutsideChina(latitude: Double, longitude: Double): Boolean =
        longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271

    private fun transformLatitude(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLongitude(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
