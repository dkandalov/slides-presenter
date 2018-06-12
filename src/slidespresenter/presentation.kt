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

private fun String.toVirtualFile(basePath: String = ""): VirtualFile? {
    val filePath = "file://" + basePath + File.separator + this
    val virtualFile = VirtualFileManager.getInstance().refreshAndFindFileByUrl(filePath)
    return if (basePath != "" && virtualFile == null) toVirtualFile(basePath = "") else virtualFile
}

private val presentationKey = Key<Presentation>("presentation")


class PresentationLoaderComponent(private val project: Project): ProjectComponent {
    override fun projectOpened() {
        val presentation = project.loadPresentation()
        if (presentation != null) validateSlides(presentation.slides)

        project.putUserData(presentationKey, presentation)
    }

    private fun Project.loadPresentation(): Presentation? {
        val slidesFile = baseDir.findChild("slides.txt") ?: return null

        initSlidesFileModificationListener(slidesFile, this)

        val lines = slidesFile.inputStream.reader().readLines()
        return lines.parseAsPresentation()
    }

    private fun validateSlides(slides: List<String>) {
        val missingSlides = slides
            .map { Pair(it, it.toVirtualFile(project.basePath!!)) }
            .filter { it.second == null }
            .joinToString("<br/>") { it.first }

        if (missingSlides.isNotEmpty()) {
            showNotification("The following slides could not be found<br/>$missingSlides")
        }
    }

    private fun initSlidesFileModificationListener(slidesFile: VirtualFile, project: Project) {
        val document = FileDocumentManager.getInstance().getDocument(slidesFile)
        document?.addDocumentListener(object: DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val lines = event.document.text.split("\n")
                var updatedPresentation = lines.parseAsPresentation()
                if (updatedPresentation != null) {
                    val presentation = project.getUserData(presentationKey)
                    if (presentation != null && updatedPresentation.slides.contains(presentation.currentSlide)) {
                        updatedPresentation = updatedPresentation.copy(currentSlide = presentation.currentSlide)
                    }
                    project.putUserData(presentationKey, updatedPresentation)
                }
            }
        }, project)
    }
}