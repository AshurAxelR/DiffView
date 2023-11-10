package com.xrbpowered.diff.ui;

import java.awt.Color;
import java.io.File;

import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.UIModalWindow;
import com.xrbpowered.zoomui.UIModalWindow.ResultHandler;
import com.xrbpowered.zoomui.base.UIButtonBase;
import com.xrbpowered.zoomui.std.UIToolButton;
import com.xrbpowered.zoomui.std.file.UIFileBrowser;
import com.xrbpowered.zoomui.std.text.UITextBox;

public class FileSelectionPane extends UIContainer {

	private UIModalWindow<File> openDlgA, openDlgB;

	private UIModalWindow<File> createOpenDialog(String title, final UITextBox txtPath) {
		return UIFileBrowser.createDialog(title, new ResultHandler<File>() {
			@Override
			public void onResult(File result) {
				if(folderMode && result!=null && !result.isDirectory())
					result = result.getParentFile();
				txtPath.editor.setText(result==null ? (folderMode ? "." : "") : result.getPath().replace(File.separator, "/"));
				txtPath.onEnter();
				txtPath.repaint();
			}
			@Override
			public void onCancel() {
			}
		});
	}
	
	public final UITextBox txtPathA, txtPathB;
	public final UIButtonBase btnBrowseA, btnBrowseB;

	public boolean folderMode;
	
	private DiffListener diff = null;
	
	public FileSelectionPane(UIContainer parent, boolean folder) {
		super(parent);
		this.folderMode = folder;
		
		txtPathA = new UITextBox(this) {
			@Override
			public boolean onEnter() {
				setPaths(editor.getText(), null);
				return true;
			}
		};
		openDlgA = createOpenDialog(folder ? "Select original directory" : "Select original file", txtPathA);
		btnBrowseA = new UIToolButton(this, UIToolButton.iconPath+"folder.svg", 16, 2) {
			public void onAction() {
				openA();
			}
		};
		
		txtPathB = new UITextBox(this) {
			@Override
			public boolean onEnter() {
				setPaths(null, editor.getText());
				return true;
			}
		};
		openDlgB = createOpenDialog(folder ? "Select updated directory" : "Select updated file", txtPathB);
		btnBrowseB = new UIToolButton(this, UIToolButton.iconPath+"folder.svg", 16, 2) {
			public void onAction() {
				openB();
			}
		};
	}

	public void openA() {
		showOpenDlg(openDlgA, txtPathA);
	}

	public void openB() {
		showOpenDlg(openDlgB, txtPathB);
	}

	private static void showOpenDlg(UIModalWindow<File> dlg, UITextBox text) {
		File dir = new File(text.editor.getText());
		if(!dir.isDirectory())
			dir = dir.getParentFile();
		UIFileBrowser.fromDialog(dlg).setDirectory(dir, false);
		dlg.show();
	}
	
	public FileSelectionPane setDiffListener(DiffListener diff) {
		this.diff = diff;
		return this;
	}
	
	public void setPaths(String pathA, String pathB) {
		if(pathA!=null && !txtPathA.editor.getText().equals(pathA))
			txtPathA.editor.setText(pathA);
		if(pathB!=null && !txtPathB.editor.getText().equals(pathB))
			txtPathB.editor.setText(pathB);
		if(diff!=null)
			diff.setDiff(pathA, pathB);
	}
	
	@Override
	protected void paintSelf(GraphAssist g) {
		g.fill(this, Color.WHITE); // new Color(0xf2f2f2));
		g.setFont(UITextBox.font);
		g.setColor(Color.BLACK);
		g.drawString("Original:", txtPathA.getX()-4, txtPathA.getY()+txtPathA.getHeight()/2, GraphAssist.RIGHT, GraphAssist.CENTER);
		g.drawString("Updated:", txtPathB.getX()-4, txtPathB.getY()+txtPathB.getHeight()/2, GraphAssist.RIGHT, GraphAssist.CENTER);
	}

	@Override
	public void layout() {
		setSize(getWidth(), txtPathA.getHeight()*2+16);
		txtPathA.setSize(getWidth()-72-btnBrowseA.getWidth(), txtPathA.getHeight());
		txtPathA.setLocation(64, 4);
		btnBrowseA.setLocation(txtPathA.getX()+txtPathA.getWidth()+4, txtPathA.getY());
		txtPathB.setSize(txtPathA.getWidth(), txtPathA.getHeight());
		txtPathB.setLocation(txtPathA.getX(), txtPathA.getHeight()+8);
		btnBrowseB.setLocation(txtPathB.getX()+txtPathB.getWidth()+4, txtPathB.getY());
		super.layout();
	}

}
