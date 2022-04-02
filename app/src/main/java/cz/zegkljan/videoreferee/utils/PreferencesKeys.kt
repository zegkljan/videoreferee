package cz.zegkljan.videoreferee.utils

/**
 * A preferences key under which the last version that should be ignored when checking for new versions is stored.
 * Only versions newer than the one stored under this key is offered to the user.
 */
const val LAST_IGNORED_VERSION_KEY = "last-ignored-version"

const val CAMERA_SELECTOR_PREFS_VERSION = 2
const val CAMERA_SELECTOR_PREFS_VERSION_KEY = "camera-selector-prefs-version"
const val CAMERA_ID_KEY = "camera-id"
const val RESOLUTION_KEY = "resolution"
const val FPS_KEY = "fps"
const val BOUT_COUNTER_KEY = "bout-counter"
const val EXCHANGE_COUNTER_KEY = "exchange-counter"
