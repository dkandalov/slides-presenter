package slidespresenter

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


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