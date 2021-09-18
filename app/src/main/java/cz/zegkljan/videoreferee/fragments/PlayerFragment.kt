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

import android.os.Bundle
import android.util.Log
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
            val file = File(args.filename)
            if (!file.delete()) {
                Log.e(TAG, "Failed to delete file $file")
            }
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
    }
}
