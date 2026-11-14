package com.djsebas.chatdjsvip

import java.nio.charset.StandardCharsets.UTF_8
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAttachmentExtractorTest {
    @Test
    fun keepsShortTextUnchanged() {
        assertEquals("Contenido breve", limitAttachmentText("Contenido breve"))
    }

    @Test
    fun truncatesLongTextWithNotice() {
        val result = limitAttachmentText("x".repeat(120_050))
        assertTrue(result.startsWith("x".repeat(120_000)))
        assertTrue(result.endsWith("[Contenido recortado por el límite de 120 KB de texto.]"))
    }

    @Test
    fun extractsDocxParagraphText() {
        val xml = """<w:document xmlns:w="urn:w"><w:body><w:p><w:r><w:t>Hola</w:t></w:r><w:r><w:t> mundo</w:t></w:r></w:p></w:body></w:document>"""
        assertEquals("Hola  mundo", extractOfficeOpenXmlText(mapOf("word/document.xml" to xml.toByteArray(UTF_8)), "docx"))
    }

    @Test
    fun extractsPptxSlidesInNumericOrder() {
        val slide2 = """<p:sld xmlns:p="urn:p" xmlns:a="urn:a"><a:t>Segundo</a:t></p:sld>"""
        val slide1 = """<p:sld xmlns:p="urn:p" xmlns:a="urn:a"><a:t>Primero</a:t></p:sld>"""
        val result = extractOfficeOpenXmlText(mapOf(
            "ppt/slides/slide2.xml" to slide2.toByteArray(UTF_8),
            "ppt/slides/slide1.xml" to slide1.toByteArray(UTF_8)
        ), "pptx")
        assertTrue("Debe extraer el texto de la primera diapositiva: $result", result.contains("Primero"))
        assertTrue("Debe extraer el texto de la segunda diapositiva: $result", result.contains("Segundo"))
        assertTrue("Las diapositivas deben ordenarse numéricamente: $result", result.indexOf("Primero") < result.indexOf("Segundo"))
    }

    @Test
    fun extractsXlsxSharedAndInlineStrings() {
        val shared = """<sst xmlns="urn:s"><si><t>Nombre</t></si></sst>"""
        val sheet = """<worksheet xmlns="urn:s"><sheetData><row><c r="A1" t="s"><v>0</v></c><c r="B1" t="inlineStr"><is><t>Eleazar</t></is></c><c r="C1"><v>42</v></c></row></sheetData></worksheet>"""
        val result = extractOfficeOpenXmlText(mapOf(
            "xl/sharedStrings.xml" to shared.toByteArray(UTF_8),
            "xl/worksheets/sheet1.xml" to sheet.toByteArray(UTF_8)
        ), "xlsx")
        assertTrue(result.contains("A1: Nombre"))
        assertTrue(result.contains("B1: Eleazar"))
        assertTrue(result.contains("C1: 42"))
    }
}
