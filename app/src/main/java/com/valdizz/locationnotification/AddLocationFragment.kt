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
            throw ClassCastException(context.toString() + " must implement OnButtonClickListener.")

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btn_ok.setOnClickListener {
            if (et_latitude.text.isEmpty() || et_longitude.text.isEmpty()) {
                Toast.makeText(activity, getString(R.string.msg_incorrect_coordinates), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val latitude = et_latitude.text.toString().toDouble()
            val longitude = et_longitude.text.toString().toDouble()
            if (latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                onButtonClickListener?.addGeofenceButtonClick(LatLng(latitude, longitude))
                dismiss()
            }
            else {
                Toast.makeText(activity, getString(R.string.msg_incorrect_coordinates), Toast.LENGTH_SHORT).show()
            }
        }
        btn_cancel.setOnClickListener { dismiss() }
    }

    interface OnButtonClickListener {
        fun addGeofenceButtonClick(latLng: LatLng)
    }

    companion object {
        fun newInstance() = AddLocationFragment()
    }
}