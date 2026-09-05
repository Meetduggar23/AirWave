package com.example.airwave

import com.example.airwave.model.ChatMessage
import com.example.airwave.util.MessageNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageNotifierTest {

    private fun incoming(name: String, text: String) =
        ChatMessage(senderName = name, text = text, isSent = false)

    private fun sent(name: String, text: String) =
        ChatMessage(senderName = name, text = text, isSent = true)

    @Test
    fun `only new messages after the seen count are reported`() {
        val all = listOf(incoming("Aman", "Hello"), incoming("Aman", "Are you there?"))
        val new = MessageNotifier.newIncomingMessages(1, all)
        assertEquals(1, new.size)
        assertEquals("Are you there?", new[0].text)
    }

    @Test
    fun `own sent messages never produce notifications`() {
        val all = listOf(sent("Me", "Hi"), incoming("Aman", "Hi!"))
        val new = MessageNotifier.newIncomingMessages(0, all)
        assertEquals(1, new.size)
        assertEquals("Aman", new[0].senderName)
    }

    @Test
    fun `empty or blank messages are ignored`() {
        val all = listOf(incoming("Aman", "   "), incoming("", "Hello"), incoming("Aman", "ok"))
        val new = MessageNotifier.newIncomingMessages(0, all)
        assertEquals(1, new.size)
        assertEquals("ok", new[0].text)
    }

    @Test
    fun `unchanged list reports nothing`() {
        val all = listOf(incoming("Aman", "Hello"))
        assertTrue(MessageNotifier.newIncomingMessages(1, all).isEmpty())
        assertTrue(MessageNotifier.newIncomingMessages(5, all).isEmpty())
    }

    @Test
    fun `cleared session reports nothing and is safe to reset`() {
        assertTrue(MessageNotifier.newIncomingMessages(3, emptyList()).isEmpty())
    }

    @Test
    fun `a burst of messages is not duplicated across emissions`() {
        var seen = 0
        val burst = listOf(incoming("Aman", "m1"), incoming("Aman", "m2"), incoming("Aman", "m3"))
        val first = MessageNotifier.newIncomingMessages(seen, burst)
        seen += first.size
        val second = MessageNotifier.newIncomingMessages(seen, burst + incoming("Aman", "m4"))
        assertEquals(3, first.size)
        assertEquals(1, second.size)
        assertEquals("m4", second[0].text)
    }
}