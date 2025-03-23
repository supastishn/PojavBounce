package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.features.command.builder.LexedParameters.Companion.tokenizeCommand
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CommandLexerTest {

    @Test
    fun textTokenization() {
        assertEquals(LexedParameters(".friend", listOf(LexedParameters.Token(".friend", 0))), tokenizeCommand(".friend"))
        assertEquals(LexedParameters(".friend add", listOf(LexedParameters.Token(".friend", 0), LexedParameters.Token("add", 8))), tokenizeCommand(".friend add"))
        assertEquals(LexedParameters(".friend\" add", listOf(LexedParameters.Token(".friend\"", 0), LexedParameters.Token("add", 9))), tokenizeCommand(".friend\" add"))
        assertEquals(LexedParameters("\".friend\" add", listOf(LexedParameters.Token(".friend", 0), LexedParameters.Token("add", 10))), tokenizeCommand("\".friend\" add"))
        assertEquals(LexedParameters("\".friend add", listOf(LexedParameters.Token("\".friend add", 0))), tokenizeCommand("\".friend add"))
        assertEquals(LexedParameters("\\\".friend add", listOf(LexedParameters.Token("\".friend", 0), LexedParameters.Token("add", 10))), tokenizeCommand("\\\".friend add"))
        assertEquals(LexedParameters(".fri\"end add", listOf(LexedParameters.Token(".fri\"end", 0), LexedParameters.Token("add", 9))), tokenizeCommand(".fri\"end add"))
    }

    @Test
    fun testOffset() {
        val noOffset = tokenizeCommand("\\\".friend add asdf")
        val withOffset = noOffset.withOffset(1)

        assertEquals("\".friend", noOffset[0])
        assertEquals("asdf", noOffset[2])

        assertEquals("add", withOffset[0])
        assertEquals("asdf", withOffset[1])

        assertEquals("\\\".friend add asdf", noOffset.text)
        assertEquals("add asdf", withOffset.text)
    }

}
