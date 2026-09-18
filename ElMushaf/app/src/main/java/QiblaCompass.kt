package com.elmushaf.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.*
import android.location.*
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlin.time.Duration.Companion.milliseconds
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.*

internal fun normalizeBearing(value: Float): Float = ((value % 360f) + 360f) % 360f
internal fun qiblaArrowRotation(bearing: Float, magneticHeading: Float, declination: Float): Float =
    normalizeBearing(bearing - normalizeBearing(magneticHeading + declination) + 180f) - 180f

internal fun calculateQibla(latitude: Double, longitude: Double): Float {
    val kaabaLat = Math.toRadians(21.4225)
    val delta = Math.toRadians(39.8262 - longitude)
    val userLat = Math.toRadians(latitude)
    return normalizeBearing(Math.toDegrees(atan2(sin(delta) * cos(kaabaLat),
        cos(userLat) * sin(kaabaLat) - sin(userLat) * cos(kaabaLat) * cos(delta))).toFloat())
}

private fun hasLocationPermission(context: Context) = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

// Never use an old location from a previous city as the user's current position.
internal fun freshQiblaLocation(location: Location): Boolean =
    location.latitude.isFinite() && location.longitude.isFinite() &&
    location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 &&
    location.hasAccuracy() && location.accuracy in 0f..10000f &&
    SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos in 0L..120_000_000_000L

@SuppressLint("MissingPermission")
@Composable
fun QiblaCompassScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val manager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val sensors = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensors.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) }
    var permitted by remember { mutableStateOf(hasLocationPermission(context)) }
    var location by remember { mutableStateOf<Location?>(null) }
    var enabled by remember { mutableStateOf(LocationManagerCompat.isLocationEnabled(manager)) }
    var heading by remember { mutableStateOf<Float?>(null) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permitted = hasLocationPermission(context)
    }
    fun requestPermission() { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION)) }
    LaunchedEffect(Unit) { if (!permitted) requestPermission() }

    DisposableEffect(permitted, lifecycle) {
        val listener = object : LocationListener {
            override fun onLocationChanged(value: Location) {
                if (freshQiblaLocation(value)) location = value
            }
            override fun onProviderEnabled(provider: String) { enabled = LocationManagerCompat.isLocationEnabled(manager) }
            override fun onProviderDisabled(provider: String) {
                enabled = LocationManagerCompat.isLocationEnabled(manager)
                if (!enabled) location = null
            }
            @Deprecated("Deprecated in Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        fun start() {
            permitted = hasLocationPermission(context)
            enabled = LocationManagerCompat.isLocationEnabled(manager)
            if (!permitted) { location = null; return }
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            location = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .filter(::freshQiblaLocation).minByOrNull { it.accuracy }
            providers.forEach { provider -> runCatching {
                manager.requestLocationUpdates(provider, 1000L, 1f, listener, Looper.getMainLooper())
            } }
        }
        fun stop() { runCatching { manager.removeUpdates(listener) } }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) start()
            if (event == Lifecycle.Event.ON_PAUSE) stop()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose { lifecycle.removeObserver(observer); stop() }
    }
    // Expire a position if updates stop, rather than leaving a plausible but stale arrow.
    LaunchedEffect(location) {
        val value = location ?: return@LaunchedEffect
        val age = (SystemClock.elapsedRealtimeNanos() - value.elapsedRealtimeNanos) / 1_000_000L
        kotlinx.coroutines.delay((120_001L - age).coerceAtLeast(1L).milliseconds)
        if (location === value) location = null
    }
    DisposableEffect(sensor, lifecycle, view) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val matrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                val axes = when (view.display?.rotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                val corrected = FloatArray(9)
                if (!SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, corrected)) return
                val orientation = FloatArray(3)
                SensorManager.getOrientation(corrected, orientation)
                val value = normalizeBearing(Math.toDegrees(orientation[0].toDouble()).toFloat())
                if (value.isFinite()) heading = value
                accuracy = event.accuracy
            }
            override fun onAccuracyChanged(sensor: Sensor?, value: Int) { accuracy = value }
        }
        fun start() { heading = null; sensor?.let { sensors.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) } }
        fun stop() { sensors.unregisterListener(listener); heading = null }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) start()
            if (event == Lifecycle.Event.ON_PAUSE) stop()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose { lifecycle.removeObserver(observer); stop() }
    }
    val position = location?.takeIf { permitted && enabled && freshQiblaLocation(it) }
    val bearing = position?.let { calculateQibla(it.latitude, it.longitude) }
    val declination = position?.let { GeomagneticField(it.latitude.toFloat(), it.longitude.toFloat(),
        (if (it.hasAltitude()) it.altitude else 0.0).toFloat(), System.currentTimeMillis()).declination }
    val magneticHeading = heading
    val arrowRotation = if (bearing != null && declination != null && magneticHeading != null &&
        accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM && sensor != null) {
        qiblaArrowRotation(bearing, magneticHeading, declination)
    } else null
    val ready = arrowRotation != null
    val rotation = arrowRotation ?: 0f
    val status = when {
        !permitted -> "اسمح باستخدام الموقع لتحديد القبلة"
        !enabled -> "شغّل خدمة الموقع لتحديد القبلة"
        position == null -> "جاري تحديد موقعك الحالي…"
        sensor == null -> "الهاتف لا يدعم بوصلة الاتجاه؛ استخدم الزاوية الموضّحة مع بوصلة خارجية"
        heading == null -> "جاري قراءة البوصلة…"
        accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "حرّك الهاتف على شكل ٨ لمعايرة البوصلة، وابتعد عن المعادن والمغناطيس"
        else -> "أمسك الهاتف أفقيًا، ولفّه حتى يشير السهم لأعلى الشاشة"
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("اتجاه القبلة", fontSize = 30.sp, color = MushafGreen)
        Spacer(Modifier.height(24.dp))
        Text("🕋", fontSize = 60.sp)
        if (ready) {
            // The unrotated arrow points UP, matching the north-based bearing convention.
            Canvas(Modifier.size(180.dp).graphicsLayer { rotationZ = rotation }) {
                val path = Path().apply {
                    moveTo(size.width / 2, size.height * .08f)
                    lineTo(size.width * .78f, size.height * .48f)
                    lineTo(size.width * .59f, size.height * .42f)
                    lineTo(size.width * .59f, size.height * .90f)
                    lineTo(size.width * .41f, size.height * .90f)
                    lineTo(size.width * .41f, size.height * .42f)
                    lineTo(size.width * .22f, size.height * .48f)
                    close()
                }
                drawPath(path, MushafGreen)
            }
            if (abs(rotation) <= 3f) Text("أنت باتجاه القبلة", color = MushafGreen, fontSize = 22.sp)
        } else Spacer(Modifier.height(32.dp))
        bearing?.let { Text("القبلة: ${String.format(java.util.Locale.US, "%.1f", it)}° من الشمال الحقيقي",
            fontSize = 17.sp, textAlign = TextAlign.Center) }
        Spacer(Modifier.height(20.dp))
        Text(status, fontSize = 18.sp, textAlign = TextAlign.Center)
        if (!permitted) TextButton(onClick = { requestPermission() }) { Text("السماح بالموقع") }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("العودة للرئيسية") }
    }
}
