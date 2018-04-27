package slidespresenter

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

val presentationKey = Key<Presentation>("presentation")

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

class NextSlideAction: AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.switchSlide(Direction.next)
    }
}

class PreviousSlideAction: AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.switchSlide(Direction.previous)
    }
}

fun Project.switchSlide(direction: Direction) {
    val presentation = (getUserData(presentationKey) ?: loadSlides() ?: return).moveSlide(direction)
    putUserData(presentationKey, presentation)

    val openedFile = openInEditor(presentation.currentSlide, this)
    if (openedFile == null) showNotification("Slide not found ${presentation.currentSlide}")
}

fun showNotification(message: String) {
    val groupDisplayId = "Slides Presenter"
    val title = "Slides Presenter"
    val notification = Notification(groupDisplayId, title, message, NotificationType.INFORMATION)
    ApplicationManager.getApplication().messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
}

fun openInEditor(fileUrl: String, project: Project): VirtualFile? {
    val fileManager = VirtualFileManager.getInstance()
    val urlsToTry = listOf(
        "file://$fileUrl",
        "file://" + project.basePath + File.separator + fileUrl
    )
    val virtualFile = urlsToTry.asSequence()
        .mapNotNull {
            // note that it has to be refreshAndFindFileByUrl (not just findFileByUrl) otherwise VirtualFile might be null
            fileManager.refreshAndFindFileByUrl(it)
        }
        .firstOrNull() ?: return null

    FileEditorManager.getInstance(project).openFile(virtualFile, true, true)
    return virtualFile
}

private fun Project.loadSlides(): Presentation? {
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

private fun List<String>.parseAsPresentation(): Presentation? {
    val slidePaths = map { it.trim() }.filterNot { it.isEmpty() || it.startsWith("//") || it.startsWith("#") }

    return if (slidePaths.isEmpty()) null
    else Presentation(slidePaths).expandSlidesWithTemplate()
}

private fun Presentation.expandSlidesWithTemplate(): Presentation {
    var counter = 1
    fun String.expandIntoMultipleSlides(): List<String> {
        val i1 = indexOf("{{next ")
        val i2 = i1 + "{{next ".length
        val i3 = indexOf("}}")
        val i4 = i3 + "}}".length
        val amount = substring(i2, i3).toInt()

        val from = counter
        val to = counter + amount
        counter = to

        return from.until(to).map {
            val index = String.format("%03d", it)
            replaceRange(i1, i4, index)
        }
    }
    return copy(slides = slides.flatMap {
        if (it.contains("{{next ")) it.expandIntoMultipleSlides() else listOf(it)
    })
}