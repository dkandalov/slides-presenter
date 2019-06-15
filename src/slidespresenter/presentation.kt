package slidespresenter

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.*
import java.io.File

enum class Direction(val value: Int) {
    previous(-1),
    next(1)
}

data class Presentation(
    val slides: List<String>,
    val currentSlide: String = "",
    private val currentSlideIndex: Int = slides.indexOf(currentSlide)
) {
    fun moveSlide(direction: Direction, stepSize: Int = 1): Presentation {
        var i = currentSlideIndex + (stepSize * direction.value)
        if (i < 0) i = 0
        if (i >= slides.size) i = slides.size - 1
        return copy(currentSlide = slides[i], currentSlideIndex = i)
    }

    fun loadStateFrom(that: Presentation?): Presentation {
        if (that == null) return this
        val i = slides.indexOf(that.currentSlide)
        return if (i != -1) Presentation(slides, that.currentSlide, i) else this
    }
}

fun Project.containsSlide(file: VirtualFile): Boolean {
    val presentation = getUserData(presentationKey) ?: return false
    return presentation.slides.contains(file.path.replace("$basePath/", ""))
}

fun Project.switchSlide(direction: Direction, stepSize: Int = 1) {
    val presentation = (getUserData(presentationKey) ?: return).moveSlide(direction, stepSize)
    putUserData(presentationKey, presentation)

    val virtualFile = presentation.currentSlide.toVirtualFile(basePath!!)
        ?: return showNotification("Slide not found ${presentation.currentSlide}")

    // Call `openFile` twice because it seems to solve the problem
    // of slides with images not being fullscreen when using Full HD (1920×1080 px) resolution.
    FileEditorManager.getInstance(this).openFile(virtualFile, true, true)
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
        val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath ?: return null)
        val slidesFile = baseDir?.findChild("slides.txt") ?: return null

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
            showNotification("The following slides could not be found:<br/>$missingSlides")
        }
    }

    private fun initSlidesFileModificationListener(slidesFile: VirtualFile, project: Project) {
        VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileListener {
            override fun contentsChanged(event: VirtualFileEvent) {
                if (event.file != slidesFile) return

                val lines = event.file.inputStream.reader().readLines()
                var updatedPresentation = lines.parseAsPresentation()
                if (updatedPresentation != null) {
                    val presentation = project.getUserData(presentationKey)
                    updatedPresentation = updatedPresentation.loadStateFrom(presentation)
                }
                project.putUserData(presentationKey, updatedPresentation)
            }
        }, project)
    }
}