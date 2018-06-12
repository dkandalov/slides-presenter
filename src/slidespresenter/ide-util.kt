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

@Suppress("USELESS_CAST", "UNCHECKED_CAST")
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
