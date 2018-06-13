package slidespresenter

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import slidespresenter.Direction.next
import slidespresenter.Direction.previous

class PresentationTests {
    @Test fun `parse lines as presentation`() {
        assertThat(emptyList<String>().parseAsPresentation(), equalTo<Presentation>(null))

        assertThat(listOf("slide1.png").parseAsPresentation(), equalTo(Presentation(
            slides = listOf("slide1.png")
        )))

        assertThat(listOf("slide1.png", "", "slide2.png").parseAsPresentation(), equalTo(Presentation(
            slides = listOf("slide1.png", "slide2.png")
        )))

        assertThat(listOf("slide{{next 3}}.png", "some-slide.png", "slide{{next 3}}.png").parseAsPresentation(), equalTo(Presentation(
            slides = listOf(
                "slide001.png", "slide002.png", "slide003.png",
                "some-slide.png",
                "slide004.png", "slide005.png", "slide006.png"
            )
        )))
    }

    @Test fun `move to next, previous slide`() {
        val it = Presentation(slides = listOf("slide1", "slide2"))

        assertThat(it.currentSlide, equalTo(""))

        assertThat(it.moveSlide(previous).currentSlide, equalTo("slide1"))
        assertThat(it.moveSlide(next).currentSlide, equalTo("slide1"))
        assertThat(it.moveSlide(next).moveSlide(next).currentSlide, equalTo("slide2"))
        assertThat(it.moveSlide(next).moveSlide(next).moveSlide(next).currentSlide, equalTo("slide2"))
        assertThat(it.moveSlide(next).moveSlide(previous).currentSlide, equalTo("slide1"))
    }
}