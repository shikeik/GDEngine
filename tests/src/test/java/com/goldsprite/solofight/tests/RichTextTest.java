package com.goldsprite.solofight.tests;

import com.goldsprite.gdengine.ui.widget.richtext.RichText;
import com.goldsprite.gdengine.ui.widget.richtext.RichElement;
import com.goldsprite.solofight.GdxTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.SnapshotArray;

@RunWith(GdxTestRunner.class)
public class RichTextTest {

	@Test
	public void testRichTextCreation() {
		// Run in Gdx thread
		String markup = "Hello [color=red]Red[/color]";
		RichText rt = new RichText(markup, 200);
		
		SnapshotArray<Actor> children = rt.getChildren();
		// Expected: "Hello ", "Red" (2 labels)
		// Note: NewLineActor might be inserted depending on logic, but basic text should be there.
		// My implementation: 
		// 1. "Hello " -> addTextActor
		// 2. "Red" -> addTextActor
		
		Assert.assertTrue(children.size >= 2);
	}
	
	@Test
	public void testWrapping() {
		// Create a RichText with very small width to force wrap
		// "A B" -> if width is small, it should wrap.
		// Note: Actual wrapping depends on font width calculation which requires FreeType.
		// In Headless backend, FreeType native libraries must be loaded.
		// GdxTestRunner usually sets up HeadlessApplication.
		
		String markup = "A B C D E";
		RichText rt = new RichText(markup, 10); // Very narrow
		rt.layout();
		
		// If wrapped, height should be > single line height
		float singleLineHeight = 30; // Approx default
		Assert.assertTrue("Should wrap multiple lines", rt.getPrefHeight() > singleLineHeight);
	}

	@Test
	public void testEventHandling() {
		// Just verify event string is attached
		// Since we can't easily click in unit test without crafting InputEvent,
		// we assume the parser and actor creation logic puts the listener there.
		// We can check if the actor has listeners.
		
		String markup = "[event=test]Click[/event]";
		RichText rt = new RichText(markup);
		
		Actor label = rt.getChildren().get(0);
		Assert.assertTrue("Should have click listener", label.getListeners().size > 0);
	}
}
