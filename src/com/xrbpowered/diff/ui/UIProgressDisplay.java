package com.xrbpowered.diff.ui;

import java.awt.Color;

import com.xrbpowered.diff.FolderDiff;
import com.xrbpowered.utils.UISafeThread;
import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.UIElement;
import com.xrbpowered.zoomui.std.UIButton;

public class UIProgressDisplay extends UIElement {

	public static final Color colorBackground = Color.WHITE;
	public static final Color colorText = Color.BLACK;
	public static final Color colorPath = new Color(0xaaaaaa);
	
	private static final Color colorSpinDark = new Color(0x777777);
	private static final Color colorSpinLight = new Color(0xdddddd);
	
	public static final int width = 100;
	
	public final FolderDiff diff;
	
	private final UISafeThread repaintThread;
	private int spin = 0;
	
	public UIProgressDisplay(UIContainer parent, FolderDiff diff) {
		super(parent);
		this.diff = diff;
		
		repaintThread = new UISafeThread() {
			@Override
			protected void loop() throws InterruptedException {
				Thread.sleep(100);
				spin = (spin+1)%4;
				safeUIRunAsync();
			}
			@Override
			protected void uiRun() {
				repaint();
			}
		};
		repaintThread.start();
	}
	
	public void dismiss() {
		repaintThread.interrupt();
		getParent().removeChild(this);
	}

	@Override
	public void paint(GraphAssist g) {
		g.fill(this, colorBackground);
		float x = getWidth()/2 - width/2;
		float y = getHeight()/2;
		
		g.fillRect(x-24, y-7, 6, 6, spin==0 ? colorSpinDark : colorSpinLight);
		g.fillRect(x-24+8, y-7, 6, 6, spin==1 ? colorSpinDark : colorSpinLight);
		g.fillRect(x-24+8, y-7+8, 6, 6, spin==2 ? colorSpinDark : colorSpinLight);
		g.fillRect(x-24, y-7+8, 6, 6, spin==3 ? colorSpinDark : colorSpinLight);
		
		g.setFont(UIButton.font);
		g.setColor(colorText);
		g.drawString(String.format("%d files", diff.progress), x, y-2, GraphAssist.LEFT, GraphAssist.BOTTOM);
		g.setColor(colorPath);
		g.drawString(diff.currentDir, x, y+2, GraphAssist.LEFT, GraphAssist.TOP);
	}

}
