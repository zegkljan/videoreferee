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

/**
 * In this [Fragment] we let users pick a camera, size and FPS to use for high
 * speed video recording
 */
class SelectorFragment : Fragment() {

    /** Android ViewBinding */
    private lateinit var fragmentSelectorBinding: FragmentSelectorBinding

    private var selectedCamera: CameraInfo? = null
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
        val prefsVersion = prefs.getInt(CAMERA_SELECTOR_PREFS_VERSION_KEY, 0)
        if (prefsVersion < CAMERA_SELECTOR_PREFS_VERSION) {
            prefs.edit()
                .remove(CAMERA_ID_KEY)
                .remove(FPS_KEY)
                .remove(RESOLUTION_KEY)
                .putInt(CAMERA_SELECTOR_PREFS_VERSION_KEY, CAMERA_SELECTOR_PREFS_VERSION)
                .apply()
        }

        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val settings = enumerateSettings(cameraManager, this)

        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            settings.keys.toList()
        ).also { adapter ->
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
                selectedCamera = parent!!.getItemAtPosition(position) as CameraInfo
                prefs.edit().putString(CAMERA_ID_KEY, selectedCamera!!.id).apply()

                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, settings[selectedCamera]!!.keys.toList()).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    fragmentSelectorBinding.fpsSpinner.adapter = adapter
                }
                fragmentSelectorBinding.fpsSpinner.isEnabled = true

                val storedFps = prefs.getInt(FPS_KEY, -1)
                if (storedFps != -1) {
                    for (i in 0 until fragmentSelectorBinding.fpsSpinner.adapter.count) {
                        val item = fragmentSelectorBinding.fpsSpinner.adapter.getItem(i) as Int
                        if (storedFps == item) {
                            fragmentSelectorBinding.fpsSpinner.setSelection(i)
                            break
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCamera = null
                selectedFps = null
                isSelectedFpsHighSpeed = null

                fragmentSelectorBinding.fpsSpinner.isEnabled = false
                fragmentSelectorBinding.fpsSpinner.adapter = null

                fragmentSelectorBinding.resolutionSpinner.isEnabled = false
                fragmentSelectorBinding.resolutionSpinner.adapter = null

                fragmentSelectorBinding.continueButton.isEnabled = false
            }
        }
        val storedCameraId = prefs.getString(CAMERA_ID_KEY, null)
        if (storedCameraId != null) {
            for (i in 0 until fragmentSelectorBinding.cameraSpinner.adapter.count) {
                val item = fragmentSelectorBinding.cameraSpinner.adapter.getItem(i) as CameraInfo
                if (storedCameraId == item.id) {
                    fragmentSelectorBinding.cameraSpinner.setSelection(i)
                    break
                }
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
                val fps = parent!!.getItemAtPosition(position) as Int
                selectedFps = fps
                isSelectedFpsHighSpeed = settings[selectedCamera!!]!![fps]!!.first.isHighSpeed
                prefs.edit().putInt(FPS_KEY, fps).apply()

                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    settings[selectedCamera]!![fps]!!.second
                ).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    fragmentSelectorBinding.resolutionSpinner.adapter = adapter
                }
                fragmentSelectorBinding.resolutionSpinner.isEnabled = true

                val storedResolution = prefs.getString(RESOLUTION_KEY, null)
                if (storedResolution != null) {
                    for (i in 0 until fragmentSelectorBinding.resolutionSpinner.adapter.count) {
                        val item = fragmentSelectorBinding.resolutionSpinner.adapter.getItem(i) as Size
                        if (storedResolution == item.toString()) {
                            fragmentSelectorBinding.resolutionSpinner.setSelection(i)
                            break
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedFps = null
                isSelectedFpsHighSpeed = null
                selectedResolution = null

                fragmentSelectorBinding.resolutionSpinner.isEnabled = false
                fragmentSelectorBinding.resolutionSpinner.adapter = null

                fragmentSelectorBinding.continueButton.isEnabled = false
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
                val res = parent!!.getItemAtPosition(position) as Size
                selectedResolution = res
                prefs.edit().putString(RESOLUTION_KEY, res.toString()).apply()

                fragmentSelectorBinding.continueButton.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedResolution = null

                fragmentSelectorBinding.continueButton.isEnabled = false
            }
        }

        fragmentSelectorBinding.continueButton.isEnabled = false
        fragmentSelectorBinding.continueButton.setOnClickListener {
            val dir = if (isSelectedFpsHighSpeed!!) {
                SelectorFragmentDirections.actionSelectorToHighSpeedCamera(
                    selectedCamera!!.id,
                    selectedResolution!!.width,
                    selectedResolution!!.height,
                    selectedFps!!
                )
            } else {
                SelectorFragmentDirections.actionSelectorToNormalSpeedCamera(
                    selectedCamera!!.id,
                    selectedResolution!!.width,
                    selectedResolution!!.height,
                    selectedFps!!
                )
            }
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(dir)
        }
    }

    companion object {
        const val CAMERA_SELECTOR_PREFS_VERSION_KEY = "camera-selector-prefs-version"
        const val CAMERA_SELECTOR_PREFS_VERSION = 1
        const val CAMERA_ID_KEY = "camera-id"
        const val RESOLUTION_KEY = "resolution"
        const val FPS_KEY = "fps"

        private data class CameraInfo(
            val id: String,
            val orientation: String
        ) {
            override fun toString(): String {
                return "$orientation ($id)"
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as CameraInfo

                if (id != other.id) return false

                return true
            }

            override fun hashCode(): Int {
                return id.hashCode()
            }
        }

        private data class FpsInfo(
            val fps: Int,
            val isHighSpeed: Boolean
        ) {
            override fun toString(): String {
                return fps.toString()
            }
        }

        /** Converts a lens orientation enum into a human-readable string */
        private fun lensOrientationString(value: Int, fragment: Fragment) = when(value) {
            CameraCharacteristics.LENS_FACING_BACK -> fragment.getString(R.string.camera_back)
            CameraCharacteristics.LENS_FACING_FRONT -> fragment.getString(R.string.camera_front)
            CameraCharacteristics.LENS_FACING_EXTERNAL -> fragment.getString(R.string.camera_external)
            else -> fragment.getString(R.string.camera_unknown)
        }

        private fun enumerateSettings(cameraManager: CameraManager, fragment: Fragment): Map<CameraInfo, Map<Int, Pair<FpsInfo, List<Size>>>> {
            val sizeComp: Comparator<Size> = Comparator { s1, s2 ->
                if (s1.width == s2.width) {
                    s1.height.compareTo(s2.height)
                } else {
                    s1.width.compareTo(s2.width)
                }
            }
            val res = linkedMapOf<CameraInfo, Map<Int, Pair<FpsInfo, List<Size>>>>()
            cameraManager.cameraIdList.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation = lensOrientationString(characteristics.get(CameraCharacteristics.LENS_FACING)!!, fragment)
                val cameraInfo = CameraInfo(id, orientation)
                val submap = linkedMapOf<Int, Pair<FpsInfo, MutableList<Size>>>()
                val capabilities =
                    characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                val config =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)) {
                    config.getOutputSizes(MediaRecorder::class.java).sortedWith(sizeComp).forEach { size ->
                        val secondsPerFrame = config.getOutputMinFrameDuration(
                            MediaRecorder::class.java,
                            size
                        ) / 1_000_000_000.0
                        val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                        submap.putIfAbsent(fps, Pair(FpsInfo(fps, false), mutableListOf(size)))?.second?.add(size)
                    }
                }
                if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO)) {
                    config.highSpeedVideoFpsRanges.sortedBy { it.upper }.forEach { range ->
                        val fps = range.upper
                        submap[fps] = Pair(
                            FpsInfo(fps, true),
                            config.getHighSpeedVideoSizesFor(range).toMutableList().apply { sortWith(sizeComp) }
                        )
                    }
                }
                res[cameraInfo] = submap
            }
            return res
        }
    }
}
