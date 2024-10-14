package com.example.matemateca

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    lateinit var sceneView: ARSceneView
    lateinit var loadingView: View
    lateinit var instructionText: TextView

    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    var anchorNode: AnchorNode? = null
        set(value) {
            if (field != value) {
                field = value
                updateInstructions()
            }
        }

    var anchorNodeView: View? = null

    var trackingFailureReason: TrackingFailureReason? = null
        set(value) {
            if (field != value) {
                field = value
                updateInstructions()
            }
        }

    fun updateInstructions() {
        instructionText.text = trackingFailureReason?.let {
            it.getDescription(this)
        } ?: if (anchorNode == null) {
            "Aponte a câmera para baixo."
        } else {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        instructionText = findViewById(R.id.instructionText)
        loadingView = findViewById(R.id.loadingView)
        sceneView = findViewById<ARSceneView>(R.id.sceneView).apply {
            lifecycle = this@MainActivity.lifecycle
            planeRenderer.isEnabled = true
            configureSession{ session, config ->
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            }
            onSessionUpdated = {_, frame ->
                if (anchorNode == null) {
                    frame.getUpdatedPlanes()
                        .firstOrNull {it.type == Plane.Type.HORIZONTAL_UPWARD_FACING}
                        ?.let { plane -> addAnchorNode(plane.createAnchor(plane.centerPose)) }
                }
            }
            onTrackingFailureChanged = { reason ->
                this@MainActivity.trackingFailureReason = reason
            }
        }
    }

    fun addAnchorNode(anchor: Anchor) {
        sceneView.addChildNode(
            AnchorNode(sceneView.engine, anchor)
                .apply {
                    isEditable = true
                    lifecycleScope.launch {
                        isLoading = true
                        buildModelNode()?.let { addChildNode(it) }
                        isLoading = false
                    }
                    anchorNode = this
                }
        )
    }

    suspend fun buildModelNode(): ModelNode? {
        return try {
            sceneView.modelLoader.loadModelInstance("models/model.glb")
                ?.let { modelInstance ->
                    ModelNode(
                        modelInstance = modelInstance,
                        scaleToUnits = 0.5f,
                        centerOrigin = Position(y = -0.5f)
                    ).apply {
                        isEditable = true
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}