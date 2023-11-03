package com.xrbpowered.diff.ui;

import java.awt.Color;
import java.io.File;

import com.xrbpowered.diff.DiffView;
import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.UIModalWindow;
import com.xrbpowered.zoomui.UIModalWindow.ResultHandler;
import com.xrbpowered.zoomui.base.UIButtonBase;
import com.xrbpowered.zoomui.std.UIToolButton;
import com.xrbpowered.zoomui.std.file.UIFileBrowser;
import com.xrbpowered.zoomui.std.text.UITextBox;

public class FileDiffBase extends UIContainer {

	private static UIModalWindow<File> openDlgA, openDlgB;

	private static UIModalWindow<File> createOpenDialog(String title, UITextBox txtPath) {
		return UIFileBrowser.createDialog(title, new ResultHandler<File>() {
			@Override
			public void onResult(File result) {
				txtPath.editor.setText(result.getPath());
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
	public final FileDiffView.Area diffView;
	
	public FileDiffBase(UIContainer parent) {
		super(parent);
		txtPathA = new UITextBox(this) {
			@Override
			public boolean onEnter() {
				diffView.viewer.setDiff(DiffView.loadLines(editor.getText(), diffView.viewer.linesA), diffView.viewer.linesB);
				return true;
			}
		};
		openDlgA = createOpenDialog("Select original file", txtPathA);
		btnBrowseA = new UIToolButton(this, UIToolButton.iconPath+"folder.svg", 16, 2) {
			public void onAction() {
				openDlgA.show();
			}
		};
		txtPathB = new UITextBox(this) {
			@Override
			public boolean onEnter() {
				diffView.viewer.setDiff(diffView.viewer.linesA, DiffView.loadLines(editor.getText(), diffView.viewer.linesB));
				return true;
			}
		};
		openDlgB = createOpenDialog("Select updated file", txtPathB);
		btnBrowseB = new UIToolButton(this, UIToolButton.iconPath+"folder.svg", 16, 2) {
			public void onAction() {
				openDlgB.show();
			}
		};
		diffView = new FileDiffView.Area(this);
	}
	
	public void setPaths(String pathA, String pathB) {
		txtPathA.editor.setText(pathA==null ? "" : pathA);
		txtPathB.editor.setText(pathB==null ? "" : pathB);
		diffView.viewer.setDiff(
				DiffView.loadLines(txtPathA.editor.getText(), diffView.viewer.linesA),
				DiffView.loadLines(txtPathB.editor.getText(), diffView.viewer.linesB)
			);
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
		txtPathA.setSize(getWidth()-72-btnBrowseA.getWidth(), txtPathA.getHeight());
		txtPathA.setLocation(64, 4);
		btnBrowseA.setLocation(txtPathA.getX()+txtPathA.getWidth()+4, txtPathA.getY());
		txtPathB.setSize(txtPathA.getWidth(), txtPathA.getHeight());
		txtPathB.setLocation(txtPathA.getX(), txtPathA.getHeight()+8);
		btnBrowseB.setLocation(txtPathB.getX()+txtPathB.getWidth()+4, txtPathB.getY());
		diffView.setLocation(0, txtPathA.getHeight()*2+16);
		diffView.setSize(getWidth(), getHeight()-diffView.getY());
		super.layout();
	}

}
