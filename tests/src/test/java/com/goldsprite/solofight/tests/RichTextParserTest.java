package com.goldsprite.solofight.tests;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.ui.widget.richtext.RichElement;
import com.goldsprite.gdengine.ui.widget.richtext.RichTextParser;
import com.goldsprite.gdengine.ui.widget.richtext.RichStyle;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class RichTextParserTest {

	@Test
	public void testPlainText() {
		String input = "Hello World";
		List<RichElement> elements = RichTextParser.parse(input, new RichStyle());
		
		Assert.assertEquals(1, elements.size());
		Assert.assertEquals("Hello World", elements.get(0).text);
		Assert.assertEquals(RichElement.Type.TEXT, elements.get(0).type);
	}

	@Test
	public void testColorTag() {
		String input = "A[color=red]B[/color]C";
		List<RichElement> elements = RichTextParser.parse(input, new RichStyle());
		
		Assert.assertEquals(3, elements.size());
		
		Assert.assertEquals("A", elements.get(0).text);
		
		Assert.assertEquals("B", elements.get(1).text);
		Assert.assertEquals(Color.RED, elements.get(1).style.color);
		
		Assert.assertEquals("C", elements.get(2).text);
		Assert.assertEquals(Color.WHITE, elements.get(2).style.color); // Default is white usually
	}

	@Test
	public void testHexColor() {
		String input = "[#00FF00]Green[/color]";
		List<RichElement> elements = RichTextParser.parse(input, new RichStyle());
		
		Assert.assertEquals(1, elements.size());
		Assert.assertEquals("Green", elements.get(0).text);
		Assert.assertEquals(Color.GREEN, elements.get(0).style.color);
	}

	@Test
	public void testNestedTags() {
		// [size=50]Big [color=red]Red[/color] Big[/size]
		String input = "[size=50]Big [color=red]Red[/color] Big[/size]";
		RichStyle def = new RichStyle();
		def.fontSize = 20;
		
		List<RichElement> elements = RichTextParser.parse(input, def);
		
		Assert.assertEquals(3, elements.size());
		
		// "Big " -> Size 50
		Assert.assertEquals("Big ", elements.get(0).text);
		Assert.assertEquals(50, elements.get(0).style.fontSize, 0.01f);
		
		// "Red" -> Size 50, Color Red
		Assert.assertEquals("Red", elements.get(1).text);
		Assert.assertEquals(50, elements.get(1).style.fontSize, 0.01f);
		Assert.assertEquals(Color.RED, elements.get(1).style.color);
		
		// " Big" -> Size 50
		Assert.assertEquals(" Big", elements.get(2).text);
		Assert.assertEquals(50, elements.get(2).style.fontSize, 0.01f);
		Assert.assertNotEquals(Color.RED, elements.get(2).style.color);
	}

	@Test
	public void testImageTag() {
		String input = "Icon: [img=test.png|32x32]";
		List<RichElement> elements = RichTextParser.parse(input, new RichStyle());
		
		Assert.assertEquals(2, elements.size());
		Assert.assertEquals("Icon: ", elements.get(0).text);
		
		Assert.assertEquals(RichElement.Type.IMAGE, elements.get(1).type);
		Assert.assertEquals("test.png", elements.get(1).imagePath);
		Assert.assertEquals(32, elements.get(1).imgWidth, 0.01f);
		Assert.assertEquals(32, elements.get(1).imgHeight, 0.01f);
	}
}
