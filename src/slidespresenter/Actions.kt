package slidespresenter

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
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

data class Presentation(val slides: List<String>, val currentSlide: String) {
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
    // note that it has to be refreshAndFindFileByUrl (not just findFileByUrl) otherwise VirtualFile might be null
    val virtualFile = urlsToTry.asSequence().mapNotNull { fileManager.refreshAndFindFileByUrl(it) }.firstOrNull() ?: return null

    FileEditorManager.getInstance(project).openFile(virtualFile, true, true)
    return virtualFile
}

private fun Project.loadSlides(): Presentation? {
    val slidesFile = baseDir.findChild("slides.txt") ?: return null
    val slidePaths = slidesFile
        .inputStream.reader().readLines()
        .map { it.trim() }
        .filterNot { it.isEmpty() || it.startsWith("//") || it.startsWith("#") }
    if (slidePaths.isEmpty()) return null

    return Presentation(slidePaths, slidePaths.first())
}
