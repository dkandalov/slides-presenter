package slidespresenter

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import org.intellij.images.editor.ImageZoomModel
import javax.swing.JScrollPane

class FullScreenImageComponent: ApplicationComponent {
    override fun initComponent() {
        val listener = object: FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
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


    fun <T> accessField(o: Any, possibleFieldNames: List<String>, fieldClass: Class<T>): T {
        possibleFieldNames.forEach { fieldName ->
            try {
                val result = accessField(o, fieldName, fieldClass)
                if (result != null) return result
            } catch (ignored: Exception) {
            }
        }
        throw IllegalStateException("Didn't find any of the fields [${possibleFieldNames.joinToString(",")}] " +
                                        "(with class ${fieldClass.canonicalName}) in object $o")
    }

    fun <T> accessField(o: Any, fieldName: String, fieldClass: Class<T>): T {
        var aClass: Class<Any>? = o.javaClass
        val allClasses = ArrayList<Class<Any>?>()
        while (aClass != null && aClass != Object::javaClass) {
            allClasses.add(aClass)
            aClass = aClass.superclass as Class<Any>?
        }
        val allFields = allClasses.filterNotNull().flatMap { it.declaredFields.toList() }

        allFields.forEach { field ->
            if (field.name == fieldName && fieldClass.isAssignableFrom(field.type)) {
                field.isAccessible = true
                return field.get(o) as T
            }
        }
        throw IllegalStateException("Didn't find field '$fieldName' (with class ${fieldClass.canonicalName}) in object $o")
    }

}