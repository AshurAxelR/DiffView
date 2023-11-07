package com.xrbpowered.diff.ui;

import java.awt.Color;
import java.awt.Font;

import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.UIElement;
import com.xrbpowered.zoomui.std.UIListBox;
import com.xrbpowered.zoomui.std.text.UITextBox;

public class ErrorOverlay extends UIElement {
	
	public static final Font font = UITextBox.font;

	public static final Color colorBackground = UIListBox.colorBackground;
	public static final Color colorBorder = UIListBox.colorBorder;
	public static final Color colorMessage = new Color(0xaaaaaa);
	
	private String message = null;
	
	public ErrorOverlay(UIContainer parent) {
		super(parent);
	}

	@Override
	public void paint(GraphAssist g) {
		g.fill(this, colorBackground);
		g.hborder(this, GraphAssist.TOP, colorBorder);
		
		if(message!=null && !message.isEmpty()) {
			g.setColor(colorMessage);
			g.setFont(font);
			g.drawString(message, getWidth()/2, getHeight()/2, GraphAssist.CENTER, GraphAssist.CENTER);
		}
	}
	
	public void show(String message) {
		this.message = message;
		setVisible(true);
	}

}
