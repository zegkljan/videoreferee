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

import android.content.Context
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import cz.zegkljan.videoreferee.R
import cz.zegkljan.videoreferee.databinding.FragmentPlayerBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PlayerFragment : Fragment() {

    /** Android ViewBinding */
    private var _fragmentPlayerBinding: FragmentPlayerBinding? = null

    private val fragmentPlayerBinding get() = _fragmentPlayerBinding!!

    /** AndroidX navigation arguments */
    private val args: PlayerFragmentArgs by navArgs()

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentPlayerBinding = FragmentPlayerBinding.inflate(inflater, container, false)
        return fragmentPlayerBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentPlayerBinding.filename.text = args.filename
        fragmentPlayerBinding.fps.text = args.fps.toString()

        fragmentPlayerBinding.playPauseButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                fragmentPlayerBinding.state.text = "playing"
            } else {
                fragmentPlayerBinding.state.text = "paused"
            }
        }
        fragmentPlayerBinding.doneButton.setOnClickListener {
            navController.navigate(PlayerFragmentDirections.actionPlayerToCamera(args.cameraId, args.width, args.height, args.fps))
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDestroyView() {
        _fragmentPlayerBinding = null
        super.onDestroyView()
    }

    companion object {
        private val TAG = PlayerFragment::class.java.simpleName

        /**
         * FPS rate for preview-only requests, 30 is *guaranteed* by framework. See:
         * [StreamConfigurationMap.getHighSpeedVideoFpsRanges]
         */
        private const val FPS_PREVIEW_ONLY: Int = 30

        /** Creates a [File] named with the current date and time */
        private fun createFile(context: Context, extension: String): File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
            return File(context.filesDir, "VID_${sdf.format(Date())}.$extension")
        }
    }
}
