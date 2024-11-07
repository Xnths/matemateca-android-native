package com.example.matemateca

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.addAugmentedImage
import io.github.sceneview.ar.arcore.getUpdatedAugmentedImages
import io.github.sceneview.ar.node.AugmentedImageNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode

class MainFragment: Fragment(R.layout.fragment_main) {
    lateinit var sceneView: ARSceneView

    val augmentedImageNodes = mutableListOf<AugmentedImageNode>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById<ARSceneView>(R.id.sceneView).apply {
            configureSession { session, config ->
                config.addAugmentedImage(
                    session, "rabbit",
                    requireContext().assets.open("augmentedimages/rabbit.jpg")
                        .use(BitmapFactory::decodeStream)
                )
                config.addAugmentedImage(
                    session, "qrcode",
                    requireContext().assets.open("augmentedimages/qrcode.png")
                        .use(BitmapFactory::decodeStream)
                )
            }
            onSessionUpdated = { session, frame ->
                frame.getUpdatedAugmentedImages().forEach { augmentedImage ->
                    if (augmentedImageNodes.none { it.imageName == augmentedImage.name }) {
                        val augmentedImageNode = AugmentedImageNode(engine, augmentedImage).apply { 
                            when (augmentedImage.name) {
                                "qrcode" -> addChildNode(
                                    ModelNode(
                                        modelInstance = modelLoader.createModelInstance(
                                            assetFileLocation = "models/model.glb"
                                        ),
                                        scaleToUnits = 0.1f,
                                        centerOrigin = Position(0f)
                                    )
                                )
                            }
                        }
                        addChildNode(augmentedImageNode)
                        augmentedImageNodes += augmentedImageNode
                    }
                }
            }
        }
    }

}