package com.valdizz.locationnotification

import android.content.Context
import com.google.android.gms.location.GeofenceStatusCodes

/**
 * Contains function to receive an error message.
 *
 * @autor Vlad Kornev
 */
object GeofenceErrorMessages {

    fun getErrorString(context: Context, errorCode: Int): String {
        return when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> context.resources.getString(R.string.geofence_not_available)
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> context.resources.getString(R.string.geofence_too_many_geofences)
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> context.resources.getString(R.string.geofence_too_many_pending_intents)
            else -> context.resources.getString(R.string.unknown_geofence_error)
        }
    }
}