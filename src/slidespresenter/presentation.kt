package slidespresenter

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.annotations.SystemIndependent
import java.io.File

enum class Direction(val value: Int) {
    previous(-1),
    next(1)
}

data class Presentation(val slides: List<String>, val currentSlide: String = "") {
    fun moveSlide(direction: Direction): Presentation {
        var i = slides.indexOf(currentSlide) + direction.value
        if (i < 0) i = 0
        if (i >= slides.size) i = slides.size - 1
        return copy(currentSlide = slides[i])
    }
}

fun Project.containsSlide(file: VirtualFile): Boolean {
    val presentation = getUserData(presentationKey) ?: return false
    return presentation.slides.contains(file.path.replace("$basePath/", ""))
}

fun Project.switchSlide(direction: Direction) {
    val presentation = (getUserData(presentationKey) ?: return).moveSlide(direction)
    putUserData(presentationKey, presentation)

    val virtualFile = presentation.currentSlide.toVirtualFile(basePath!!)
        ?: return showNotification("Slide not found ${presentation.currentSlide}")

    FileEditorManager.getInstance(this).openFile(virtualFile, true, true)
}

private fun String.toVirtualFile(basePath: @SystemIndependent String = ""): VirtualFile? {
    val filePath = "file://" + basePath + File.separator + this
    return VirtualFileManager.getInstance().refreshAndFindFileByUrl(filePath) ?: toVirtualFile(basePath = "")
}

private val presentationKey = Key<Presentation>("presentation")


class PresentationLoaderComponent(private val project: Project): ProjectComponent {
    override fun projectOpened() {
        val presentation = project.loadPresentation()
        project.putUserData(presentationKey, presentation)
    }

    private fun Project.loadPresentation(): Presentation? {
        val slidesFile = baseDir.findChild("slides.txt") ?: return null

        val document = FileDocumentManager.getInstance().getDocument(slidesFile)
        document?.addDocumentListener(object: DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val lines = event.document.text.split("\n")
                var updatedPresentation = lines.parseAsPresentation()
                if (updatedPresentation != null) {
                    val presentation = getUserData(presentationKey)
                    if (presentation != null && updatedPresentation.slides.contains(presentation.currentSlide)) {
                        updatedPresentation = updatedPresentation.copy(currentSlide = presentation.currentSlide)
                    }
                    putUserData(presentationKey, updatedPresentation)
                }
            }
        }, this)

        val lines = slidesFile.inputStream.reader().readLines()
        return lines.parseAsPresentation()
    }
}