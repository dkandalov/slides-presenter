package slidespresenter

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

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

private val presentationKey = Key<Presentation>("presentation")

fun Project.switchSlide(direction: Direction) {
    val presentation = (getUserData(presentationKey) ?: loadPresentation() ?: return).moveSlide(direction)
    putUserData(presentationKey, presentation)

    val openedFile = openInEditor(presentation.currentSlide, this)
    if (openedFile == null) showNotification("Slide not found ${presentation.currentSlide}")
}

private fun Project.loadPresentation(): Presentation? {
    val slidesFile = baseDir.findChild("slides.txt") ?: return null

    val document = FileDocumentManager.getInstance().getDocument(slidesFile)
    document?.addDocumentListener(object : DocumentListener {
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
