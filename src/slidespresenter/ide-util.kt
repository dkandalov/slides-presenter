package slidespresenter

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File


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