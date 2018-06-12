package slidespresenter

import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.UISettingsListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.options.OptionsManager
import org.intellij.images.options.TransparencyChessboardOptions.ATTR_BLACK_COLOR
import org.intellij.images.options.TransparencyChessboardOptions.ATTR_WHITE_COLOR
import org.intellij.images.options.ZoomOptions.ATTR_SMART_ZOOMING
import java.awt.Color
import javax.swing.JScrollPane

class FullScreenImageComponent: ApplicationComponent {
    override fun initComponent() {
        initGlobalUISettingsToggle()
        initOpenFileListener()
    }

    private fun initOpenFileListener() {
        val listener = object: FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                if (!UISettings.instance.presentationMode) return

                val editor = source.getSelectedEditor(file)
                if (editor?.component?.javaClass?.name?.contains("ImageEditorUI") == true) {
                    val ui = editor.component

                    // Remove top panel with buttons
                    ui.remove(0)

                    // Change background color of the scrollpane around image so that it blurs with it
                    val scrollPane = accessField(ui, listOf("g", "myScrollPane"), JScrollPane::class.java)
                    scrollPane.viewport.background = JBColor.white

                    val zoomModel = accessField(ui, "zoomModel", ImageZoomModel::class.java)
                    zoomModel.zoomFactor = 1.1
                }

    //                val relativePath = file.canonicalPath.replace(slidesBasePath, "")
    //                val i = slides.indexOf(relativePath)
    //                if (i != -1) currentSlide = i
            }
        }
        val application = ApplicationManager.getApplication()
        application.messageBus.connect(application).subscribe(FILE_EDITOR_MANAGER, listener)
    }

    private fun initGlobalUISettingsToggle() {
        var savedWhiteColor: Color? = null
        var savedBlackColor: Color? = null
        var savedSmartZooming: Boolean? = null

        ApplicationManager.getApplication().messageBus.connect().subscribe(UISettingsListener.TOPIC, UISettingsListener {
            val options = OptionsManager.getInstance().options
            val chessboardOptions = options.editorOptions.transparencyChessboardOptions
            val zoomOptions = options.editorOptions.zoomOptions

            if (UISettings.instance.presentationMode) {
                savedWhiteColor = chessboardOptions.whiteColor
                savedBlackColor = chessboardOptions.blackColor
                savedSmartZooming = zoomOptions.isSmartZooming

                chessboardOptions.setOption(ATTR_WHITE_COLOR, JBColor.white)
                chessboardOptions.setOption(ATTR_BLACK_COLOR, JBColor.white) // set to white so that there is no border around image
                zoomOptions.setOption(ATTR_SMART_ZOOMING, false) // disable so that zoomFactor can be set in the code below
            } else {
                if (savedWhiteColor != null) chessboardOptions.setOption(ATTR_WHITE_COLOR, savedWhiteColor)
                if (savedBlackColor != null) chessboardOptions.setOption(ATTR_BLACK_COLOR, savedBlackColor)
                if (savedSmartZooming != null) zoomOptions.setOption(ATTR_SMART_ZOOMING, savedSmartZooming)

                savedWhiteColor = null
                savedBlackColor = null
                savedSmartZooming = null
            }
        })
    }

}