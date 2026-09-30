package com.sim.lavis.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The handful of Material icons the app uses, as inline vectors — cheaper than pulling in
 * the whole icon library for ~20 glyphs.
 */
object LavisIcons {
    val Play = icon("M8,5v14l11,-7z")
    val Pause = icon("M6,19h4V5H6v14zm8,-14v14h4V5h-4z")
    val SkipNext = icon("M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z")
    val SkipPrevious = icon("M6,6h2v12H6zm3.5,6l8.5,6V6z")
    val Shuffle = icon(
        "M10.59,9.17L5.41,4 4,5.41l5.17,5.17 1.42,-1.41zM14.5,4l2.04,2.04L4,18.59 5.41,20 " +
            "17.96,7.46 20,9.5V4h-5.5zm0.33,9.41l-1.41,1.41 3.13,3.13L14.5,20H20v-5.5l-2.04,2.04 -3.13,-3.13z"
    )
    val Repeat = icon("M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zm10,10H7v-3l-4,4 4,4v-3h12v-6h-2v4z")
    val RepeatOne = icon(
        "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zm10,10H7v-3l-4,4 4,4v-3h12v-6h-2v4zm-4,-2V9h-1l-2,1v1h1.5v4H13z"
    )
    val MusicNote = icon(
        "M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z"
    )
    val Library = icon(
        "M20,2H8c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zm-2,5h-3v5.5" +
            "c0,1.38 -1.12,2.5 -2.5,2.5S10,13.88 10,12.5s1.12,-2.5 2.5,-2.5c0.57,0 1.08,0.19 1.5,0.51V5h4v2z" +
            "M4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6z"
    )
    val Person = icon(
        "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zm0,2c-2.67,0 -8,1.34 -8,4v2h16v-2" +
            "c0,-2.66 -5.33,-4 -8,-4z"
    )
    val BarChart = icon("M5,9.2h3V19H5zM10.6,5h2.8v14h-2.8zm5.6,8H19v6h-2.8z")
    val Refresh = icon(
        "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6" +
            "h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78" +
            "L13,11h7V4l-2.35,2.35z"
    )
    val Back = icon("M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")
    val ExpandMore = icon("M16.59,8.59L12,13.17 7.41,8.59 6,10l6,6 6,-6z")
    val ChevronLeft = icon("M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z")
    val ChevronRight = icon("M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z")
    val Edit = icon(
        "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34" +
            "c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z"
    )
    val Add = icon("M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
    val Delete = icon("M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z")
    val Download = icon("M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z")
    val Check = icon("M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z")
    val Headphones = icon(
        "M12,1c-4.97,0 -9,4.03 -9,9v7c0,1.66 1.34,3 3,3h3v-8H5v-2c0,-3.87 3.13,-7 7,-7s7,3.13 7,7v2h-4v8h3" +
            "c1.66,0 3,-1.34 3,-3v-7c0,-4.97 -4.03,-9 -9,-9z"
    )
    val Folder = icon(
        "M10,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8l-2,-2z"
    )

    private fun icon(pathData: String): ImageVector =
        ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(Color.Black)
        ).build()
}
