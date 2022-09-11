/*
 * Copyright 2020 The Android Open Source Project
 * Modifications copyright 2021 Jan Žegklitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.zegkljan.videoreferee.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import cz.zegkljan.videoreferee.R

private const val PERMISSIONS_REQUEST_CODE = 10
private val PERMISSIONS_REQUIRED = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)
private val PERMISSIONS_REQUIRED_OLD = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)

/**
 * This [Fragment] requests permissions and, once granted, it will navigate to the next fragment
 */
class PermissionsFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (hasPermissions(requireContext())) {
            // If permissions have already been granted, proceed
            val activity = requireActivity()
            val navController = Navigation.findNavController(activity, R.id.fragment_container)
            navController.navigate(PermissionsFragmentDirections.actionPermissionsToSelector())
        } else {
            // Request permissions
            requestPermissions(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    PERMISSIONS_REQUIRED
                } else {
                    PERMISSIONS_REQUIRED_OLD
                }, PERMISSIONS_REQUEST_CODE
            )
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Takes the user to the success fragment when permission is granted
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    PermissionsFragmentDirections.actionPermissionsToSelector()
                )
            } else {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
                requireActivity().finishAndRemoveTask()
            }
        }
    }

    companion object {
        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PERMISSIONS_REQUIRED
        } else {
            PERMISSIONS_REQUIRED_OLD
        }.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
