package com.valdizz.locationnotification

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.fragment_add_location.*

/**
 * DialogFragment to enter latitude and longitude of some location.
 *
 * @autor Vlad Kornev
 */
class AddLocationFragment : DialogFragment() {

    private var onButtonClickListener: OnButtonClickListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnButtonClickListener) {
            onButtonClickListener = context
        } else {
            throw ClassCastException("$context must implement OnButtonClickListener.")

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_add_location, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btnOk.setOnClickListener {
            if (etLatitude.text.isEmpty() || etLongitude.text.isEmpty()) {
                Toast.makeText(activity, getString(R.string.msg_incorrect_coordinates), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val latitude = etLatitude.text.toString().toDouble()
            val longitude = etLongitude.text.toString().toDouble()
            if (latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                onButtonClickListener?.addGeofenceButtonClick(LatLng(latitude, longitude))
                dismiss()
            } else {
                Toast.makeText(activity, getString(R.string.msg_incorrect_coordinates), Toast.LENGTH_SHORT).show()
            }
        }
        btnCancel.setOnClickListener { dismiss() }
    }

    interface OnButtonClickListener {
        fun addGeofenceButtonClick(latLng: LatLng)
    }

    companion object {
        fun newInstance() = AddLocationFragment()
    }
}