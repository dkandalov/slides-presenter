package slidespresenter

import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.UISettingsListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.options.OptionsManager
import org.intellij.images.options.ZoomOptions.ATTR_SMART_ZOOMING
import org.intellij.images.ui.ImageComponent
import java.awt.Color
import javax.swing.JScrollPane

class FullScreenImageComponent {
    init {
        initGlobalUISettingsToggle()
        initOpenFileListener()
    }

    private fun initOpenFileListener() {
        val listener = object: FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                if (!source.project.containsSlide(file)) return

                val editor = source.getSelectedEditor(file)
                if (editor?.component?.javaClass?.name?.contains("ImageEditorUI") == true) {
                    val ui = editor.component // instance of ImageEditorUI class which is not public and cannot be accessed here directly

                    // Remove top panel with buttons
                    if (ui.componentCount == 2) ui.remove(0)

                    // Change background color so that image blurs with scrollpane and doesn't have a border
                    val scrollPane = ui.accessField(listOf("myScrollPane", "g", ""), JScrollPane::class.java)
                    val imageComponent = ui.accessField(listOf("imageComponent", ""), ImageComponent::class.java)
                    val backgroundColor = imageComponent.document.value.getRGB(0, 0).let { rgbInt: Int ->
                        val r = rgbInt.shr(16).and(0xFF)
                        val g = rgbInt.shr(8).and(0xFF)
                        val b = rgbInt.and(0xFF)
                        Color(r, g, b)
                    }
                    imageComponent.setTransparencyChessboardBlankColor(backgroundColor)
                    scrollPane.viewport.background = backgroundColor

                    // Zoom in so that image takes more space on screen
                    val zoomModel = ui.accessField(listOf("zoomModel", ""), ImageZoomModel::class.java)
                    zoomModel.zoomFactor = Registry.get("slides.presenter.zoom.factor").asDouble()
                }
            }
        }
        val application = ApplicationManager.getApplication()
        application.messageBus.connect(application).subscribe(FILE_EDITOR_MANAGER, listener)
    }

    private fun initGlobalUISettingsToggle() {
        var savedSmartZooming: Boolean? = null

        ApplicationManager.getApplication().messageBus.connect().subscribe(UISettingsListener.TOPIC, UISettingsListener {
            val zoomOptions = OptionsManager.getInstance().options.editorOptions.zoomOptions

            if (UISettings.instance.presentationMode) {
                savedSmartZooming = zoomOptions.isSmartZooming
                zoomOptions.setOption(ATTR_SMART_ZOOMING, false) // disable so that zoomFactor can be set by this plugin
            } else {
                if (savedSmartZooming != null) zoomOptions.setOption(ATTR_SMART_ZOOMING, savedSmartZooming)
                savedSmartZooming = null
            }
        })
    }
}