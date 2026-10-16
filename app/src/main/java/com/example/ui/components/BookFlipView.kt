package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class FlipDirection {
  NONE,
  FORWARD,
  BACKWARD
}

/**
 * Controller allowing external triggers (e.g. navigation buttons) to initiate 3D book flips.
 */
class BookFlipController {
  internal var onFlipForward: (() -> Unit)? = null
  internal var onFlipBackward: (() -> Unit)? = null

  fun flipForward() {
    onFlipForward?.invoke()
  }

  fun flipBackward() {
    onFlipBackward?.invoke()
  }
}

@Composable
fun rememberBookFlipController(): BookFlipController {
  return remember { BookFlipController() }
}

/**
 * Realistic 3D Book-Like Page Turning Container.
 * Features:
 * - 3D rotation with authentic perspective around spine binding (TransformOrigin(0f, 0.5f))
 * - Dynamic light/shadow modeling during page bend (cast shadows on underlying page, specular page curl highlight)
 * - Front and Back of the turning page rendered with physical book paper styling
 * - Seamless page transition without flickering or abrupt snaps
 * - Interactive edge drag gestures with threshold detection & spring bounce
 * - Corner curl dog-ear affordance for intuitive page turning
 * - Disables page turn gesture during active text selection to prevent accidental flips
 */
@Composable
fun BookFlipContainer(
  pageIndex: Int,
  totalPages: Int,
  themeMode: AppThemeMode,
  isSelectingText: Boolean,
  onPageChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
  controller: BookFlipController = rememberBookFlipController(),
  currentPageContent: @Composable () -> Unit,
  nextPageContent: (@Composable () -> Unit)? = null,
  prevPageContent: (@Composable () -> Unit)? = null
) {
  val coroutineScope = rememberCoroutineScope()
  val rotationAnim = remember { Animatable(0f) }
  var flipDirection by remember { mutableStateOf(FlipDirection.NONE) }
  var isAnimating by remember { mutableStateOf(false) }
  var dragCumulative by remember { mutableFloatStateOf(0f) }

  val pageBgColor = when (themeMode) {
    AppThemeMode.WARM_PAPER -> Color(0xFFFBF4E8)
    AppThemeMode.DARK -> Color(0xFF1E1A27)
    else -> Color(0xFFFFFDF9)
  }

  val pageBorderColor = when (themeMode) {
    AppThemeMode.DARK -> Color(0xFF383244)
    else -> Color(0xFFE2D8C7)
  }

  // Register external controller callbacks for bottom navigation buttons
  LaunchedEffect(controller, pageIndex, totalPages, isAnimating) {
    controller.onFlipForward = {
      if (!isAnimating && pageIndex < totalPages - 1) {
        isAnimating = true
        flipDirection = FlipDirection.FORWARD
        coroutineScope.launch {
          rotationAnim.snapTo(0f)
          rotationAnim.animateTo(-180f, animationSpec = tween(380))
          onPageChange(pageIndex + 1)
          rotationAnim.snapTo(0f)
          flipDirection = FlipDirection.NONE
          isAnimating = false
        }
      }
    }

    controller.onFlipBackward = {
      if (!isAnimating && pageIndex > 0) {
        isAnimating = true
        flipDirection = FlipDirection.BACKWARD
        coroutineScope.launch {
          rotationAnim.snapTo(-180f)
          rotationAnim.animateTo(0f, animationSpec = tween(380))
          onPageChange(pageIndex - 1)
          rotationAnim.snapTo(0f)
          flipDirection = FlipDirection.NONE
          isAnimating = false
        }
      }
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .testTag("book_flip_container")
  ) {
    val containerWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(pageIndex, totalPages, isAnimating, isSelectingText, containerWidthPx) {
          // Do not initiate page flip drag while user is actively selecting text
          if (isSelectingText) return@pointerInput

          detectHorizontalDragGestures(
            onHorizontalDrag = { change, dragAmount ->
              if (isAnimating) return@detectHorizontalDragGestures
              change.consume()
              dragCumulative += dragAmount

              val fraction = (dragCumulative / (containerWidthPx * 0.75f)).coerceIn(-1f, 1f)
              if (fraction < 0f && pageIndex < totalPages - 1) {
                // Swiping forward (right to left)
                flipDirection = FlipDirection.FORWARD
                val targetRot = fraction * 180f
                coroutineScope.launch {
                  rotationAnim.snapTo(targetRot)
                }
              } else if (fraction > 0f && pageIndex > 0) {
                // Swiping backward (left to right)
                flipDirection = FlipDirection.BACKWARD
                val targetRot = -180f + (fraction * 180f)
                coroutineScope.launch {
                  rotationAnim.snapTo(targetRot)
                }
              }
            },
            onDragEnd = {
              if (isAnimating) return@detectHorizontalDragGestures
              val currentRot = rotationAnim.value
              val threshold = 35f // degrees threshold to commit flip

              if (flipDirection == FlipDirection.FORWARD) {
                if (currentRot < -threshold && pageIndex < totalPages - 1) {
                  isAnimating = true
                  coroutineScope.launch {
                    rotationAnim.animateTo(-180f, animationSpec = tween(220))
                    onPageChange(pageIndex + 1)
                    rotationAnim.snapTo(0f)
                    dragCumulative = 0f
                    flipDirection = FlipDirection.NONE
                    isAnimating = false
                  }
                } else {
                  // Cancel and spring back
                  coroutineScope.launch {
                    rotationAnim.animateTo(0f, animationSpec = spring())
                    dragCumulative = 0f
                    flipDirection = FlipDirection.NONE
                  }
                }
              } else if (flipDirection == FlipDirection.BACKWARD) {
                if (currentRot > -180f + threshold && pageIndex > 0) {
                  isAnimating = true
                  coroutineScope.launch {
                    rotationAnim.animateTo(0f, animationSpec = tween(220))
                    onPageChange(pageIndex - 1)
                    rotationAnim.snapTo(0f)
                    dragCumulative = 0f
                    flipDirection = FlipDirection.NONE
                    isAnimating = false
                  }
                } else {
                  // Cancel and spring back
                  coroutineScope.launch {
                    rotationAnim.animateTo(-180f, animationSpec = spring())
                    dragCumulative = 0f
                    flipDirection = FlipDirection.NONE
                  }
                }
              } else {
                dragCumulative = 0f
              }
            },
            onDragCancel = {
              coroutineScope.launch {
                rotationAnim.animateTo(0f, animationSpec = spring())
                dragCumulative = 0f
                flipDirection = FlipDirection.NONE
              }
            }
          )
        }
    ) {
      val rot = rotationAnim.value

      // 1. LAYER UNDERNEATH (Base Revealed Page)
      if (flipDirection == FlipDirection.FORWARD) {
        // Next page revealed underneath
        PhysicalPageCard(
          themeMode = themeMode,
          bgColor = pageBgColor,
          borderColor = pageBorderColor
        ) {
          if (nextPageContent != null) {
            nextPageContent()
          } else {
            Box(modifier = Modifier.fillMaxSize())
          }
        }

        // Dynamic Cast Shadow on revealed page as the turning page lifts
        val castShadowAlpha = (sin(abs(rot) * PI / 180f).toFloat() * 0.42f).coerceIn(0f, 0.45f)
        if (castShadowAlpha > 0.01f) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(12.dp))
              .background(
                Brush.horizontalGradient(
                  colors = listOf(
                    Color.Black.copy(alpha = castShadowAlpha * 1.2f),
                    Color.Black.copy(alpha = castShadowAlpha * 0.5f),
                    Color.Transparent
                  )
                )
              )
          )
        }
      } else if (flipDirection == FlipDirection.BACKWARD) {
        // Current page sits at the base while previous page turns in from left
        PhysicalPageCard(
          themeMode = themeMode,
          bgColor = pageBgColor,
          borderColor = pageBorderColor
        ) {
          currentPageContent()
        }
      } else {
        // IDLE STATE: Just render the current page cleanly
        PhysicalPageCard(
          themeMode = themeMode,
          bgColor = pageBgColor,
          borderColor = pageBorderColor
        ) {
          currentPageContent()
        }
      }

      // 2. TURNING PAGE LAYER (Transforms in 3D around spine)
      if (flipDirection != FlipDirection.NONE) {
        val density = LocalDensity.current.density
        val isFrontVisible = rot > -90f

        Box(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
              rotationY = rot
              cameraDistance = 16f * density
              transformOrigin = TransformOrigin(0f, 0.5f) // Binding edge along left spine
            }
        ) {
          if (flipDirection == FlipDirection.FORWARD) {
            if (isFrontVisible) {
              // FRONT FACE of turning page (Page N)
              PhysicalPageCard(
                themeMode = themeMode,
                bgColor = pageBgColor,
                borderColor = pageBorderColor
              ) {
                currentPageContent()
              }

              // Dynamic highlight & curl shadow on turning face
              val foldDarken = (abs(rot) / 90f * 0.22f)
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .clip(RoundedCornerShape(12.dp))
                  .background(
                    Brush.horizontalGradient(
                      colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = foldDarken * 0.4f),
                        Color.White.copy(alpha = foldDarken * 0.15f),
                        Color.Black.copy(alpha = foldDarken)
                      )
                    )
                  )
              )
            } else {
              // BACK FACE of turning page (Page N reverse)
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer {
                    rotationY = 180f // Counteract mirror inversion
                  }
              ) {
                PhysicalBackPageCard(
                  themeMode = themeMode,
                  bgColor = pageBgColor,
                  borderColor = pageBorderColor
                )
              }
            }
          } else if (flipDirection == FlipDirection.BACKWARD) {
            if (isFrontVisible) {
              // FRONT FACE of previous page settling down
              PhysicalPageCard(
                themeMode = themeMode,
                bgColor = pageBgColor,
                borderColor = pageBorderColor
              ) {
                if (prevPageContent != null) {
                  prevPageContent()
                } else {
                  Box(modifier = Modifier.fillMaxSize())
                }
              }
            } else {
              // BACK FACE of previous page swinging into view
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .graphicsLayer {
                    rotationY = 180f
                  }
              ) {
                PhysicalBackPageCard(
                  themeMode = themeMode,
                  bgColor = pageBgColor,
                  borderColor = pageBorderColor
                )
              }
            }
          }
        }
      }

      // 3. PHYSICAL CORNER CURL AFFORDANCE (Dog-ear at bottom right)
      // Visual indicator that this is a physical page that can be turned
      if (!isAnimating && pageIndex < totalPages - 1 && !isSelectingText) {
        PageCornerCurl(
          themeMode = themeMode,
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 4.dp, bottom = 4.dp)
            .clickable {
              controller.flipForward()
            }
        )
      }

      // Optional Previous Page Corner Curl at bottom left
      if (!isAnimating && pageIndex > 0 && !isSelectingText) {
        PageCornerCurl(
          themeMode = themeMode,
          isLeft = true,
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 4.dp, bottom = 4.dp)
            .clickable {
              controller.flipBackward()
            }
        )
      }
    }
  }
}

/**
 * Physical Card container representing the authentic look & texture of a book page.
 */
@Composable
private fun PhysicalPageCard(
  themeMode: AppThemeMode,
  bgColor: Color,
  borderColor: Color,
  content: @Composable () -> Unit
) {
  Card(
    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp),
    colors = CardDefaults.cardColors(containerColor = bgColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
    modifier = Modifier.fillMaxSize()
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Main Page Content
      content()

      // Book Spine Crease Gradient on Left Edge
      val spineShadowColor = when (themeMode) {
        AppThemeMode.DARK -> Color.Black.copy(alpha = 0.4f)
        else -> Color(0xFF4A3B2A).copy(alpha = 0.14f)
      }

      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .width(18.dp)
          .fillMaxHeight()
          .background(
            Brush.horizontalGradient(
              listOf(spineShadowColor, Color.Transparent)
            )
          )
      )

      // Layered Right Edge (Simulating page thickness & stacked book pages)
      val stackEdgeColor = when (themeMode) {
        AppThemeMode.DARK -> Color(0xFF2E273A)
        else -> Color(0xFFE8DFD0)
      }
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .width(2.dp)
          .fillMaxHeight()
          .background(stackEdgeColor)
      )
    }
  }
}

/**
 * Renders the realistic reverse/back face of a physical page when turned past 90 degrees.
 */
@Composable
private fun PhysicalBackPageCard(
  themeMode: AppThemeMode,
  bgColor: Color,
  borderColor: Color
) {
  Card(
    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 4.dp, bottomEnd = 4.dp),
    colors = CardDefaults.cardColors(containerColor = bgColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxSize()
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Shaded back paper with subtle spine crease
      val spineShadowColor = when (themeMode) {
        AppThemeMode.DARK -> Color.Black.copy(alpha = 0.35f)
        else -> Color(0xFF4A3B2A).copy(alpha = 0.12f)
      }

      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .width(18.dp)
          .fillMaxHeight()
          .background(
            Brush.horizontalGradient(
              listOf(Color.Transparent, spineShadowColor)
            )
          )
      )

      // Subtle diagonal paper grain texture
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.03f),
                Color.Transparent
              )
            )
          )
      )
    }
  }
}

/**
 * Visual Dog-Ear / Corner Curl Affordance at page corners.
 */
@Composable
private fun PageCornerCurl(
  themeMode: AppThemeMode,
  modifier: Modifier = Modifier,
  isLeft: Boolean = false
) {
  val curlColor = when (themeMode) {
    AppThemeMode.WARM_PAPER -> Color(0xFFEFE4D2)
    AppThemeMode.DARK -> Color(0xFF2C2538)
    else -> Color(0xFFEDE6DA)
  }

  val shadowColor = Color.Black.copy(alpha = 0.15f)

  Box(
    modifier = modifier
      .size(26.dp)
      .testTag(if (isLeft) "page_curl_prev" else "page_curl_next")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      val path = Path()
      if (!isLeft) {
        // Bottom-Right Corner Fold
        path.moveTo(0f, h)
        path.lineTo(w, 0f)
        path.lineTo(w, h)
        path.close()
      } else {
        // Bottom-Left Corner Fold
        path.moveTo(w, h)
        path.lineTo(0f, 0f)
        path.lineTo(0f, h)
        path.close()
      }

      drawPath(path = path, color = shadowColor)

      val foldPath = Path()
      if (!isLeft) {
        foldPath.moveTo(4f, h)
        foldPath.lineTo(w, 4f)
        foldPath.lineTo(w - 2f, h - 2f)
        foldPath.close()
      } else {
        foldPath.moveTo(w - 4f, h)
        foldPath.lineTo(0f, 4f)
        foldPath.lineTo(2f, h - 2f)
        foldPath.close()
      }
      drawPath(path = foldPath, color = curlColor)
    }
  }
}
