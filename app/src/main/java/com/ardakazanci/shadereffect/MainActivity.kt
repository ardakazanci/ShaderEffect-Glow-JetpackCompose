package com.ardakazanci.shadereffect

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.ardakazanci.shadereffect.ui.theme.Purple40
import com.ardakazanci.shadereffect.ui.theme.ShaderEffectTheme
import org.intellij.lang.annotations.Language

@Language("AGSL")
val glowingButtonShader = """
  uniform shader buttonShader;
  uniform float2 buttonSize;
  uniform float radius;
  
  uniform half4 glowEffect;
  
  float roundRectSDF(vec2 position, vec2 box, float radius) {
      vec2 q = abs(position) - box + radius;
      return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;   
  }
  
  float getGlow(float dist, float radius, float intensity){
      return pow(radius/dist, intensity);
  }
  
  half4 main(float2 coord) {
      float ratio = buttonSize.y / buttonSize.x;
      float2 normRect = float2(1.0, ratio);
      float2 normRectCenter = normRect - float2(0.5, 0.5 * ratio);
      float2 pos = coord.xy / buttonSize;
      pos.y = pos.y * ratio;
      pos = pos - normRectCenter;
      float normRadius = ratio / 2.0;
      float normDistance = roundRectSDF(pos, normRectCenter, normRadius);
      float2 rectangleCenter = buttonSize / 2.0;
      float2 adjustedCoord = coord - rectangleCenter;
      float distance = roundRectSDF(adjustedCoord, rectangleCenter, radius);
      half4 color = buttonShader.eval(coord);
      if (normDistance < 0.0) {
        return color;
      } 
      float glow = getGlow(normDistance, 0.3, 1.0);
      color = glow * glowEffect;
      color = color * smoothstep(-0.5, 0.5, normDistance);
      color = 0.9 - exp(-color);
      return color;
  }
""".trimIndent()

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShaderEffectTheme {
                val shaderEffect = remember { RuntimeShader(glowingButtonShader) }
                val buttonColor = remember { Purple40 }
                buttonColor.apply {
                    shaderEffect.setFloatUniform(
                        "glowEffect",
                        green,
                        red,
                        blue,
                        alpha,
                    )
                }
                var buttonWidth by remember { mutableFloatStateOf(0f) }
                var buttonHeight by remember { mutableFloatStateOf(0f) }
                val interactionState = remember { MutableInteractionSource() }
                var sliderPosition by remember { mutableFloatStateOf(250f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            valueRange = 100f..300f,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    with(shaderEffect) {
                                        setFloatUniform(
                                            "buttonSize",
                                            buttonWidth,
                                            buttonHeight
                                        )

                                    }
                                    renderEffect = RenderEffect
                                        .createRuntimeShaderEffect(shaderEffect, "buttonShader")
                                        .asComposeRenderEffect()

                                }
                                .width(sliderPosition.dp)
                                .height(100.dp)
                                .background(color = Color.White)
                                .clickable(
                                    interactionSource = interactionState,
                                    indication = null
                                ) {}
                                .onSizeChanged { size ->
                                    buttonWidth = size.width.toFloat()
                                    buttonHeight = size.height.toFloat()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                        }
                    }
                }

            }
        }
    }
}


