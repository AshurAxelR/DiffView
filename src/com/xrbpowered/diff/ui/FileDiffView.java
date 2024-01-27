package com.xrbpowered.diff.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.util.ArrayList;

import com.xrbpowered.diff.Diff;
import com.xrbpowered.diff.DiffType;
import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.UIElement;
import com.xrbpowered.zoomui.base.UIPanView;
import com.xrbpowered.zoomui.std.UIArrowButton;
import com.xrbpowered.zoomui.std.UIScrollContainer;
import com.xrbpowered.zoomui.std.text.UITextBox;

public class FileDiffView extends UIElement {

	public static final Color colorRed = new Color(0xdd0000);
	public static final Color colorAmber = new Color(0xdd9900);
	public static final Color colorGreen = new Color(0x009900);

	public static final Color colorBgRed = new Color(0xfff7f0);
	public static final Color colorBgAmber = new Color(0xfffdee);
	public static final Color colorBgGreen = new Color(0xeeffee);

	public static final Color colorMarginText = new Color(0xaaaaaa);

	public static final Color[] bgColors = {Color.WHITE, colorBgRed, colorBgGreen, Color.WHITE};
	public static final Color[] marginColors = {new Color(0xf5f5f5), new Color(0xffeadd), new Color(0xddffdd), new Color(0xfffbdd)};
	public static final Color[] fgColors = {Color.BLACK, colorRed, colorGreen, colorAmber};
	public static final Color[] borderColors = {colorMarginText, colorRed, colorGreen, colorAmber};
	public static final String[] linePrefixes = {" ", "-", "+", "~"};

	public class Line {
		public DiffType type;
		public int indexA, indexB;
		public int width;
		
		public Line(DiffType type, int indexA, int indexB) {
			this.type = type;
			this.indexA = indexA;
			this.indexB = indexB;
		}
		
		public String text() {
			String s = indexA>=0 ? linesA[indexA] : linesB[indexB];
			return s.replace("\t", "    ");
		}
	}

	protected Font font = UITextBox.font;
	protected float fontSizeUnscaled = UITextBox.font.getSize();

	public static class Area extends UIScrollContainer {
		public final FileDiffView viewer;
		
		public Area(UIContainer parent) {
			super(parent);
			this.viewer = new FileDiffView(getView());
		}
		
		@Override
		protected void paintBackground(GraphAssist g) {
			g.fill(this, Color.WHITE);
		}
		
		@Override
		protected void paintBorder(GraphAssist g) {
			g.hborder(this, GraphAssist.TOP, colorBorder);
			viewer.paintMarginMarkers(g, (int)getHeight());
		}
		
		@Override
		protected float layoutView() {
			viewer.setPosition(0, 0);
			viewer.updateSize();
			return 0;
		}
	}
	
	protected int displayLine = 0;
	protected int lastDisplayLine = 0;
	protected float pixelScale = 0;
	protected int lineHeight = 0;
	protected int descent = 0;
	protected int page = 0;
	protected int tabWidth = 0;
	protected int x0, y0;
	protected int xmargin, wpref;
	protected Rectangle clipBounds = new Rectangle();
	protected int minx, maxx, maxy;
	protected boolean updateSize = false;
	
	protected FontMetrics fm = null;
	protected float fontSize = 0f;
	
	private static final String[] empty = { };
	public String[] linesA = empty;
	public String[] linesB = empty;
	protected ArrayList<Line> lines = new ArrayList<>();
	
	protected int firstDeletion, firstInsertion;
	protected int lastDeletion, lastInsertion;
	
	public FileDiffView(UIContainer parent) {
		super(parent);
		// setFont(new Font("Consolas", Font.PLAIN, 11), 11f);
		setFont(new Font("Verdana", Font.PLAIN, 10), 10f);
	}

	public void updateSize() {
		this.updateSize = true;
	}
	
	public UIPanView panView() {
		return (UIPanView) getParent();
	}

	public void setDiff(String[] linesA, String[] linesB) {
		this.linesA = (linesA==null) ? empty : linesA;
		this.linesB = (linesB==null) ? empty : linesB;
		
		ArrayList<Diff.DiffChunk> diff = Diff.diffText(this.linesA, this.linesB);
		
		firstDeletion = -1;
		firstInsertion = -1;
		lastDeletion = -1;
		lastInsertion = -1;
		lines.clear();
		int index = 0;
		for(Diff.DiffChunk i : diff) {
			for(int j=0; j<i.length; j++) {
				lines.add(new Line(i.type,
						i.type==DiffType.inserted ? -1 : i.startA+j,
						i.type==DiffType.deleted ? -1 : i.startB+j));
				if(i.type==DiffType.deleted) {
					if(firstDeletion<0)
						firstDeletion = index;
					lastDeletion = index;
				}
				if(i.type==DiffType.inserted) {
					if(firstInsertion<0)
						firstInsertion = index;
					lastInsertion = index;
				}
				index++;
			}
		}
		updateSize = true;
		panView().setPan(0, 0);
	}
	
	@Override
	public boolean isVisible(Rectangle clip) {
		return isVisible();
	}
	
	public void setFont(Font font, float fontSizePt) {
		this.font = font;
		this.fontSizeUnscaled = 96f*fontSizePt/72f;
		this.fm = null;
	}
	
	protected void updateFont(GraphAssist g) {
		font = font.deriveFont(fontSize);
		fm = g.graph.getFontMetrics(font);
	}
	
	protected void updateMargins() {
		xmargin = numberWidth(fm, Math.max(linesA.length+1, linesB.length+1), null, (int)(8/pixelScale));
		x0 = xmargin*2+wpref;
	}
	
	protected void updateMetrics(GraphAssist g, float fontSize) {
		if(fm==null || fontSize!=this.fontSize) {
			this.fontSize = fontSize;
			updateFont(g);
			
			descent = fm.getDescent();
			lineHeight = fm.getAscent()+descent-1;
			tabWidth = fm.stringWidth("    ");
			
			wpref = 0;
			for(String s : linePrefixes)
				wpref = Math.max(wpref, fm.stringWidth(s));
			wpref += (int)(8/pixelScale);
			updateMargins();
		}
		
		y0 = lineHeight*(1+displayLine)-descent;
		g.graph.getClipBounds(clipBounds);
		minx = (int)Math.floor(clipBounds.getMinX());
		maxx = (int)Math.ceil(clipBounds.getMaxX());
		maxy = (int)Math.ceil(clipBounds.getMaxY());
		page = (maxy-y0)/lineHeight;
	}

	@Override
	public void paint(GraphAssist g) {
		if(lineHeight>0)
			displayLine = (int)(panView().getPanY() / pixelScale / lineHeight);
		
		pixelScale = g.startPixelMode(this);
		
		updateMetrics(g, Math.round(fontSizeUnscaled/pixelScale));
		if(updateSize)
			updateMargins();
		
		int y = y0;
		float w = 0;
		lastDisplayLine = displayLine;
		for(int lineIndex = 0; lineIndex<lines.size(); lineIndex++) {
			Line line = lines.get(lineIndex);
			if(lineIndex>=displayLine && y-lineHeight<maxy) {
				drawLine(g, lineIndex, y, line);
				lastDisplayLine = lineIndex;
				y += lineHeight;
			}

			if(line.width<0)
				line.width = fm.stringWidth(line.text());
			if(line.width>w)
				w = line.width;
		}
		if(y-lineHeight<maxy) {
			fillRemainder(g, y);
		}
		
		w = (w+x0*2)*pixelScale;
		float h = lineHeight*lines.size()*pixelScale;
		if(updateSize || getWidth()!=w || getHeight()!=h) {
			panView().setPanRangeForClient(w, h);
			if(w<getParent().getWidth())
				w = getParent().getWidth();
			if(h<getParent().getHeight())
				h = getParent().getHeight();
			setSize(w, h);
			updateSize = false;
		}
		
		g.finishPixelMode();
	}
	
	protected void drawLine(GraphAssist g, int lineIndex, int y, Line line) {
		int typeIndex = line.type.ordinal();
		Color bg = bgColors[typeIndex];
		Color fg = fgColors[typeIndex];
		
		g.setFont(font);
		g.fillRect(0, y-lineHeight+descent, xmargin*2, lineHeight, marginColors[typeIndex]);
		g.fillRect(xmargin*2, y-lineHeight+descent, x0-xmargin*2, lineHeight, bg);
		g.setStroke(1/pixelScale);
		g.setColor(borderColors[typeIndex]);
		g.line(xmargin, y-lineHeight+descent, xmargin, y+descent);
		g.line(xmargin*2, y-lineHeight+descent, xmargin*2, y+descent);
		g.setColor(colorMarginText);
		if(line.indexA>=0)
			g.drawString(Integer.toString(line.indexA+1), xmargin-4/pixelScale, y, GraphAssist.RIGHT, GraphAssist.BOTTOM);
		if(line.indexB>=0)
			g.drawString(Integer.toString(line.indexB+1), xmargin*2-4/pixelScale, y, GraphAssist.RIGHT, GraphAssist.BOTTOM);
		
		g.setColor(fg);
		g.drawString(linePrefixes[typeIndex], x0-wpref/2, y, GraphAssist.CENTER, GraphAssist.BOTTOM);
		
		int x = drawString(g, line.text(), x0, y, bg, fg);
		drawRemainder(g, x, y, bg);
	}

	protected void drawRemainder(GraphAssist g, int x, int y, Color bg) {
		if(x<maxx)
			g.fillRect(x, y-lineHeight+descent, maxx-x, lineHeight, bg);
	}
	
	protected int drawString(GraphAssist g, String s, int x, int y, Color bg, Color fg) {
		int w = fm.stringWidth(s);
		if(x<maxx && x+w>minx) {
			g.fillRect(x, y-lineHeight+descent, w, lineHeight, bg);
			g.setColor(fg);
			g.setFont(font);
			g.drawString(s, x, y);
		}
		return x+w;
	}
	
	protected void fillRemainder(GraphAssist g, int y) {
		int h = maxy-y+lineHeight-descent;
		g.fillRect(minx, y-lineHeight+descent, maxx, h, bgColors[0]);

		g.fillRect(0, y-lineHeight+descent, xmargin*2, h, marginColors[0]);
		g.setStroke(1/pixelScale);
		g.setColor(borderColors[0]);
		g.line(xmargin, y-lineHeight+descent, xmargin, maxy);
		g.line(xmargin*2, y-lineHeight+descent, xmargin*2, maxy);
	}
	
	protected void paintMarginMarkers(GraphAssist g, int h) {
		// in parent coordinates, called from Area 
		float pix = getPixelSize();
		
		int x = (int)(xmargin*pix/2);
		g.setColor(colorRed);
		if(firstDeletion>=0 && firstDeletion<displayLine)
			UIArrowButton.drawUpArrow(g, x, UIArrowButton.arrowSpan);
		if(lastDeletion>=0 && lastDeletion>lastDisplayLine)
			UIArrowButton.drawDownArrow(g, x, h-UIArrowButton.arrowSpan);
		
		x = (int)(xmargin*3*pix/2);
		g.setColor(colorGreen);
		if(firstInsertion>=0 && firstInsertion<displayLine)
			UIArrowButton.drawUpArrow(g, x, UIArrowButton.arrowSpan);
		if(lastInsertion>=0 && lastInsertion>lastDisplayLine)
			UIArrowButton.drawDownArrow(g, x, h-UIArrowButton.arrowSpan);
	}

	public static int numberWidth(FontMetrics fm, int n, String format, int margin) {
		int d = 9;
		while(d<n)
			d = d*10+9;
		return fm.stringWidth(format==null ? Integer.toString(d) : String.format(format, d))+margin;
	}
	
}
