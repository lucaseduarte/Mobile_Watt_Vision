package com.example.watt_vision_mobile

/** Política local de navegação; não substitui a autorização da API. */
internal object AppAccessPolicy {
    fun canOpenContent(debugBuild: Boolean, authenticated: Boolean, loginRequested: Boolean): Boolean =
        authenticated || (debugBuild && !loginRequested)

    fun canShowDemo(debugBuild: Boolean, demoSelected: Boolean): Boolean =
        debugBuild && demoSelected
}
