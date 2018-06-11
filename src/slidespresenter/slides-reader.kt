package slidespresenter


fun List<String>.parseAsPresentation(): Presentation? {
    val slidePaths = map { it.trim() }
        .filterNot { it.isEmpty() || it.startsWith("//") || it.startsWith("#") }

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