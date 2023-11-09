package com.xrbpowered.diff.ui;

import static com.xrbpowered.diff.ui.FileDiffView.bgColors;
import static com.xrbpowered.diff.ui.FileDiffView.colorMarginText;
import static com.xrbpowered.diff.ui.FileDiffView.fgColors;
import static com.xrbpowered.diff.ui.FileDiffView.marginColors;
import static com.xrbpowered.diff.ui.FileDiffView.numberWidth;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import com.xrbpowered.diff.DiffType;
import com.xrbpowered.diff.FolderDiff;
import com.xrbpowered.diff.Ignore;
import com.xrbpowered.diff.FolderDiff.DiffItem;
import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.icons.IconPalette;
import com.xrbpowered.zoomui.icons.SvgIcon;
import com.xrbpowered.zoomui.std.UIListBox;
import com.xrbpowered.zoomui.std.UIListItem;
import com.xrbpowered.zoomui.std.UIToolButton;
import com.xrbpowered.zoomui.std.text.UITextBox;

public class FolderDiffView extends UIListBox {

	public static final Font font = UITextBox.font;
	
	public static final SvgIcon fileIcon = new SvgIcon(UIToolButton.iconPath+"file.svg", 160, UIToolButton.palette);
	public static final SvgIcon folderIcon = new SvgIcon(UIToolButton.iconPath+"folder.svg", 160, UIToolButton.palette);

	public static final SvgIcon[] diffIcons = {
			null,
			new SvgIcon(UIToolButton.iconPath+"o_minus.svg", 160, new IconPalette(new Color[][] {
				{new Color(0xeeeeee), new Color(0xeecccc), new Color(0xaa0000), Color.RED}
			})),
			new SvgIcon(UIToolButton.iconPath+"o_plus.svg", 160, new IconPalette(new Color[][] {
				{new Color(0xeeeeee), new Color(0xcceecc), new Color(0x007700), new Color(0x00ee00)}
			})),
			new SvgIcon(UIToolButton.iconPath+"dot.svg", 160, new IconPalette(new Color[][] {
				{new Color(0xeeeeee), new Color(0xeeddbb), new Color(0xdd5500), new Color(0xffaa00)}
			}))
	};

	private static final float itemHeight = 24f;
	private static final int nameX = 24; 

	public class FolderDiffItem extends UIListItem {
		public FolderDiffItem(int index, FolderDiff.DiffItem diff) {
			super(FolderDiffView.this, index, diff);
			setSize(0, itemHeight);
		}
		
		@Override
		public void paint(GraphAssist g) {
			FolderDiff.DiffItem diff = (FolderDiff.DiffItem) object;
			
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics();
			if(itemMargin==0)
				itemMargin = numberWidth(fm, maxSize, "+%d", 12);
			
			int type = diff.type.ordinal();
			boolean sel = (index==list.getSelectedIndex());
			g.fill(this, sel ? colorSelection : hover ? colorHighlight : bgColors[type]);
			
			String name = diff.path.getFileName().toString();
			int nameWidth = fm.stringWidth(name);
			int maxw = (int)getWidth() - nameWidth - itemMargin - nameX;
			Path dir = diff.path.getParent();
			String dirName = null;
			int dirNameWidth = 0;
			boolean cut = false;
			while(dir!=null) {
				dirName = dir.toString().replace(File.separator, "/")+(cut ? "/..." : "")+"/";
				dirNameWidth = fm.stringWidth(dirName);
				if(dirNameWidth<maxw)
					break;
				dir = dir.getParent();
				cut = true;
			}
			
			int x = 0;
			int rightx = (int)getWidth() - itemMargin;
			if(rightx>0) {
				boolean clip = g.pushClip(0, 0, rightx, getHeight());
				x = nameX;
				g.setColor(sel ? colorSelectedText : colorMarginText);
				if(dir!=null || cut) {
					if(dir==null) {
						dirName = ".../";
						dirNameWidth = fm.stringWidth(dirName);
					}
					g.drawString(dirName, x, getHeight()/2, GraphAssist.LEFT, GraphAssist.CENTER);
					x += dirNameWidth;
				}
				if(!diff.isDir)
					g.setColor(sel ? colorSelectedText : Color.BLACK);
				g.drawString(name, x, getHeight()/2, GraphAssist.LEFT, GraphAssist.CENTER);
				(diff.isDir ? folderIcon : fileIcon).paint(g.graph, sel ? 1 : 0, 4, 4, 16, getPixelScale(), true);
				if(clip) g.popClip();
			}
			
			if(diff.isDir) {
				g.fillRect(rightx, 0, itemMargin, getHeight(), sel ? fgColors[type] : marginColors[type]);
				g.setColor(sel ? Color.WHITE : fgColors[type]);
				g.drawString(String.format("%+d", diff.type==DiffType.deleted ? -diff.size : diff.size),
						getWidth()-4, getHeight()/2, GraphAssist.RIGHT, GraphAssist.CENTER);
			}
			else {
				SvgIcon icon = diffIcons[type];
				if(icon!=null)
					icon.paint(g.graph, 0, getWidth()-20, 4, 16, getPixelScale(), true);
			}
			
			if(rightx>0 && x+nameWidth>rightx)
				g.line(rightx, 0, rightx, getHeight(), colorBorder);

		}
	}
	
	protected int maxSize = 0;
	protected int itemMargin = 0;
	
	public Path pathA = strToPath(".");
	public Path pathB = strToPath(".");
	
	public FolderDiffView(UIContainer parent) {
		super(parent);
	}
	
	private static Path strToPath(String path) {
		return new File(path).toPath().toAbsolutePath().normalize();
	}
	
	public void setDiff(String pathA, String pathB) {
		getView().removeAllChildren();
		
		ArrayList<DiffItem> res = new ArrayList<>();
		if(pathA!=null && pathB!=null) {
			Path rootA = FolderDiff.makeRoot(pathA);
			Path rootB = FolderDiff.makeRoot(pathB);
			FolderDiff.compareFolders(rootA, rootA.toFile(), rootB, rootB.toFile(), Ignore.defaultIgnore, res);
			this.pathA = rootA;
			this.pathB = rootB;
		}
		
		maxSize = 0;
		itemMargin = 0;
		setItems(res);
	}
	
	public void setDiff(Path rootA, Path rootB, ArrayList<DiffItem> res) {
		this.pathA = rootA;
		this.pathB = rootB;
		
		maxSize = 0;
		itemMargin = 0;
		setItems(res);
	}

	@Override
	protected UIListItem createItem(int index, Object object) {
		FolderDiff.DiffItem i = (FolderDiff.DiffItem) object;
		if(i.size>maxSize)
			maxSize = i.size;
		return new FolderDiffItem(index, i);
	}

	@Override
	protected void paintBorder(GraphAssist g) {
		g.hborder(this, GraphAssist.TOP, colorBorder);
	}
	
}
