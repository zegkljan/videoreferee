/*
 * Copyright 2020 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import cz.zegkljan.videoreferee.R
import cz.zegkljan.videoreferee.databinding.FragmentSelectorBinding
import io.ktor.http.cio.websocket.*

/**
 * In this [Fragment] we let users pick a camera, size and FPS to use for high
 * speed video recording
 */
class SelectorFragment : Fragment() {

    /** Android ViewBinding */
    private lateinit var fragmentSelectorBinding: FragmentSelectorBinding

    private var selectedCameraId: String? = null
    private var selectedResolution: Size? = null
    private var selectedFps: Int? = null
    private var isSelectedFpsHighSpeed: Boolean? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        fragmentSelectorBinding = FragmentSelectorBinding.inflate(inflater, container, false)
        return fragmentSelectorBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val storedCameraId = prefs.getString(CAMERA_ID_KEY, null)

        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, enumerateCameras(cameraManager)).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            fragmentSelectorBinding.cameraSpinner.adapter = adapter
        }
        fragmentSelectorBinding.cameraSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val cameraInfo = parent!!.getItemAtPosition(position) as CameraInfo
                selectedCameraId = cameraInfo.cameraId
                prefs.edit().putString(CAMERA_ID_KEY, cameraInfo.cameraId).apply()

                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, enumerateResolutions(cameraManager, cameraInfo.cameraId)).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    fragmentSelectorBinding.resolutionSpinner.adapter = adapter
                }
                fragmentSelectorBinding.resolutionSpinner.isEnabled = true

                val storedResolution = prefs.getString(RESOLUTION_KEY, null)
                if (storedResolution != null) {
                    for (i in 0 until fragmentSelectorBinding.resolutionSpinner.adapter.count) {
                        val item = fragmentSelectorBinding.resolutionSpinner.adapter.getItem(i) as ResolutionInfo
                        if (storedResolution == item.size.toString()) {
                            fragmentSelectorBinding.resolutionSpinner.setSelection(i)
                            break
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCameraId = null
                selectedResolution = null
                selectedFps = null
                isSelectedFpsHighSpeed = null

                fragmentSelectorBinding.resolutionSpinner.isEnabled = false
                fragmentSelectorBinding.resolutionSpinner.adapter = null

                fragmentSelectorBinding.fpsSpinner.isEnabled = false
                fragmentSelectorBinding.fpsSpinner.adapter = null

                fragmentSelectorBinding.continueButton.isEnabled = false
            }
        }
        if (storedCameraId != null) {
            for (i in 0 until fragmentSelectorBinding.cameraSpinner.adapter.count) {
                val item = fragmentSelectorBinding.cameraSpinner.adapter.getItem(i) as CameraInfo
                if (storedCameraId == item.cameraId) {
                    fragmentSelectorBinding.cameraSpinner.setSelection(i)
                    break
                }
            }
        }

        fragmentSelectorBinding.resolutionSpinner.isEnabled = false
        fragmentSelectorBinding.resolutionSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val resInfo = parent!!.getItemAtPosition(position) as ResolutionInfo
                selectedResolution = resInfo.size
                prefs.edit().putString(RESOLUTION_KEY, resInfo.size.toString()).apply()

                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, enumerateFramerates(cameraManager, selectedCameraId!!, resInfo)).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    fragmentSelectorBinding.fpsSpinner.adapter = adapter
                }
                fragmentSelectorBinding.fpsSpinner.isEnabled = true

                val storedFps = prefs.getInt(FPS_KEY, -1)
                if (storedFps != -1) {
                    for (i in 0 until fragmentSelectorBinding.fpsSpinner.adapter.count) {
                        val item = fragmentSelectorBinding.fpsSpinner.adapter.getItem(i) as FramerateInfo
                        if (storedFps == item.fps) {
                            fragmentSelectorBinding.fpsSpinner.setSelection(i)
                            break
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedResolution = null
                selectedFps = null
                isSelectedFpsHighSpeed = null

                fragmentSelectorBinding.fpsSpinner.isEnabled = false
                fragmentSelectorBinding.fpsSpinner.adapter = null

                fragmentSelectorBinding.continueButton.isEnabled = false
            }
        }

        fragmentSelectorBinding.fpsSpinner.isEnabled = false
        fragmentSelectorBinding.fpsSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val fpsInfo = parent!!.getItemAtPosition(position) as FramerateInfo
                selectedFps = fpsInfo.fps
                isSelectedFpsHighSpeed = fpsInfo.isHighSpeed
                prefs.edit().putInt(FPS_KEY, fpsInfo.fps).apply()

                fragmentSelectorBinding.continueButton.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedFps = null
                isSelectedFpsHighSpeed = null

                fragmentSelectorBinding.continueButton.isEnabled = false
            }
        }

        fragmentSelectorBinding.continueButton.isEnabled = false
        fragmentSelectorBinding.continueButton.setOnClickListener {
            val dir = if (isSelectedFpsHighSpeed!!) {
                SelectorFragmentDirections.actionSelectorToHighSpeedCamera(selectedCameraId!!, selectedResolution!!.width, selectedResolution!!.height, selectedFps!!)
            } else {
                SelectorFragmentDirections.actionSelectorToNormalSpeedCamera(selectedCameraId!!, selectedResolution!!.width, selectedResolution!!.height, selectedFps!!)
            }
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(dir)
        }
    }

    companion object {
        const val CAMERA_ID_KEY = "camera-id"
        const val RESOLUTION_KEY = "resolution"
        const val FPS_KEY = "fps"

        private data class CameraInfo(
            val orientation: String,
            val cameraId: String
        ) {
            override fun toString(): String {
                return "$orientation ($cameraId)"
            }
        }

        private data class ResolutionInfo(
            val size: Size,
            val isHighSpeed: Boolean,
            val isNormalSpeed: Boolean
        ) {
            override fun toString(): String = size.toString()
        }

        private data class FramerateInfo(
            val fps: Int,
            val isHighSpeed: Boolean
        ) {
            override fun toString(): String = fps.toString()
        }

        /** Converts a lens orientation enum into a human-readable string */
        private fun lensOrientationString(value: Int) = when(value) {
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }

        private fun enumerateCameras(cameraManager: CameraManager): List<CameraInfo>
            = cameraManager.cameraIdList.map { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation = lensOrientationString(characteristics.get(CameraCharacteristics.LENS_FACING)!!)
                val info = CameraInfo(orientation, id)
                info
            }

        private fun enumerateResolutions(cameraManager: CameraManager, cameraId: String): List<ResolutionInfo> {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
            val cameraConfig = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            val sizes = mutableSetOf<ResolutionInfo>()
            if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)) {
                sizes.addAll(cameraConfig.getOutputSizes(MediaRecorder::class.java).map { ResolutionInfo(it, isHighSpeed = false, isNormalSpeed = true) })
            }
            if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO)) {
                cameraConfig.highSpeedVideoSizes.forEach {
                    val nsRes = ResolutionInfo(it, isHighSpeed = false, isNormalSpeed = true)
                    if (sizes.contains(nsRes)) {
                        sizes.remove(nsRes)
                        sizes.add(ResolutionInfo(it, isHighSpeed = true, isNormalSpeed = true))
                    } else {
                        sizes.add(ResolutionInfo(it, isHighSpeed = false, isNormalSpeed = true))
                    }
                }
            }
            return sizes.toMutableList().sortedWith { o1, o2 ->
                if (o1.size.width == o2.size.width) {
                    return@sortedWith o1.size.height.compareTo(o2.size.height)
                }
                return@sortedWith o1.size.width.compareTo(o2.size.width)
            }
        }

        private fun enumerateFramerates(cameraManager: CameraManager, cameraId: String, res: ResolutionInfo): List<FramerateInfo> {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val cameraConfig = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            val fpss = mutableSetOf<FramerateInfo>()
            if (res.isNormalSpeed) {
                val secondsPerFrame = cameraConfig.getOutputMinFrameDuration(MediaRecorder::class.java, res.size) / 1_000_000_000.0
                val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                fpss.add(FramerateInfo(fps, false))
            }
            if (res.isHighSpeed) {
                cameraConfig.getHighSpeedVideoFpsRangesFor(res.size).forEach { fpsRange ->
                    val fps = fpsRange.upper
                    fpss.add(FramerateInfo(fps, true))
                }
            }
            return fpss.toMutableList().sortedWith { o1, o2 -> o1.fps.compareTo(o2.fps) }
        }

    }
}
