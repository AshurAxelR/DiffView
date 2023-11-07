package com.xrbpowered.diff.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.xrbpowered.diff.DiffView;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.base.UILayersContainer;

public class FileDiffBase extends UIContainer implements DiffListener {

	public final FileSelectionPane fileSel;
	public final FileDiffView.Area diffView;
	public final ErrorOverlay error;
	
	private final UILayersContainer box;
	
	public FileDiffBase(UIContainer parent) {
		super(parent);
		fileSel = new FileSelectionPane(parent, false).setDiffListener(this);
		box = new UILayersContainer(this);
		diffView = new FileDiffView.Area(box);
		error = new ErrorOverlay(box);
		error.setVisible(false);
	}
	
	@Override
	public void setDiff(String pathA, String pathB) {
		setDiff(diffView.viewer, pathA, pathB, error);
	}
		
	@Override
	public void layout() {
		fileSel.layout();
		box.setLocation(0, fileSel.getHeight());
		box.setSize(getWidth(), getHeight()-fileSel.getHeight());
		box.layout();
	}
	
	private static String[] loadLines(String path, String[] old, ErrorOverlay error) {
		if(path==null)
			return old;
		if(path.isEmpty())
			return new String[] {};
		try {
			String text = DiffView.loadString(path, true);
			return text.replace("\r", "").split("\n");
		}
		catch (UnsupportedEncodingException e) {
			error.show("Not a valid UTF-8 text.");
			return null;
		}
		catch (IOException e) {
			error.show("Cannot read file.");
			return null;
		}
	}
	
	public static void setDiff(FileDiffView viewer, String pathA, String pathB, ErrorOverlay error) {
		error.setVisible(false);
		String[] linesA = loadLines(pathA, viewer.linesA, error);
		String[] linesB = loadLines(pathB, viewer.linesB, error);
		viewer.setDiff(linesA, linesB);
	}

}
