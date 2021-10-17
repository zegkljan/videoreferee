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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import cz.zegkljan.videoreferee.R
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONTokener

/**
 * This [Fragment] requests permissions and, once granted, it will navigate to the next fragment
 */
class VersionCheckFragment : Fragment() {
    private lateinit var launcher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            leave()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val activity = requireActivity()
        val info = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_ACTIVITIES)

        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        val lastVersion = prefs.getString(LAST_VERSION_KEY, null)
        if (info.versionName.equals(lastVersion)) {
            leave()
            return null
        }

        val version = Version.fromVersionString(info.versionName)

        lifecycleScope.launch {
            val latest = getLatestRelease()
            if (latest == null) {
                leave()
                return@launch
            }

            if (version < latest.version) {
                AlertDialog.Builder(requireActivity()).apply {
                    setTitle(R.string.new_version_title)
                    setMessage(getString(R.string.new_version_message, latest.version, version))
                    setPositiveButton(R.string.yes) { _, _ ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latest.url))
                        launcher.launch(intent)
                    }
                    setNeutralButton(R.string.no) { _, _ ->
                        leave()
                    }
                    setNegativeButton(R.string.no_dont_ask) { _, _ ->
                        prefs.edit {
                            putString(LAST_VERSION_KEY, info.versionName)
                            apply()
                        }
                        leave()
                    }
                }.show()
            } else {
                leave()
            }
        }

        return null
    }

    private fun leave() {
        Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(VersionCheckFragmentDirections.actionVersionCheckToPermissions())
    }

    private suspend fun getLatestRelease(): Release? {
        val client = HttpClient(Android) {
            engine {
                connectTimeout = 500
                socketTimeout = 500
            }
        }

        val res: String
        try {
            val response: HttpResponse = client.request(GIT_RELEASES) {
                method = HttpMethod.Get
                header("Accept", "application/vnd.github.v3+json")
            }
            res = response.receive()
        } catch (e: Exception) {
            return null
        }
        val jsonReleases = JSONTokener(res).nextValue() as JSONArray
        val releases = mutableListOf<Release>()
        for (i in 0 until jsonReleases.length()) {
            val jsonRelease = jsonReleases.getJSONObject(i)
            val ver = jsonRelease.getString("tag_name").removePrefix("v")
            val url = jsonRelease.getString("html_url")
            releases.add(Release(ver, url))
        }
        releases.sort()
        return releases.last()
    }

    companion object {
        const val GIT_RELEASES = "https://api.github.com/repos/zegkljan/videoreferee/releases"
        const val LAST_VERSION_KEY = "last-version"

        data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
            override fun compareTo(other: Version): Int {
                if (major < other.major) {
                    return -1
                } else if (major > other.major) {
                    return 1
                }
                if (minor < other.minor) {
                    return -1
                } else if (minor > other.minor) {
                    return 1
                }
                if (patch < other.patch) {
                    return -1
                } else if (patch > other.patch) {
                    return 1
                }
                return 0
            }

            override fun toString(): String {
                return "$major.$minor.$patch"
            }

            companion object {
                fun fromVersionString(versionString: String): Version {
                    val parts = versionString.split(".")
                    val major: Int
                    if (parts.isNotEmpty()) {
                        major = parts[0].toInt()
                    } else {
                        throw IllegalArgumentException("empty version")
                    }
                    val minor: Int = if (parts.size >= 2) {
                        parts[1].toInt()
                    } else {
                        0
                    }
                    val patch: Int = if (parts.size >= 3) {
                        parts[2].toInt()
                    } else {
                        0
                    }

                    return Version(major, minor, patch)
                }
            }
        }

        data class Release(val version: Version, val url: String) : Comparable<Release> {
            constructor(versionString: String, url: String) : this(Version.fromVersionString(versionString), url)

            override fun compareTo(other: Release): Int {
                return version.compareTo(other.version)
            }
        }
    }
}
