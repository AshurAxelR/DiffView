package com.xrbpowered.diff.ui;

import com.xrbpowered.diff.DiffView;
import com.xrbpowered.zoomui.UIContainer;

public class FileDiffBase extends UIContainer implements DiffListener {

	public final FileSelectionPane fileSel;
	public final FileDiffView.Area diffView;
	
	public FileDiffBase(UIContainer parent) {
		super(parent);
		fileSel = new FileSelectionPane(parent, false).setDiffListener(this);
		diffView = new FileDiffView.Area(this);
	}
	
	@Override
	public void setDiff(String pathA, String pathB) {
		setDiff(diffView.viewer, pathA, pathB);
	}
		
	@Override
	public void layout() {
		fileSel.layout();
		diffView.setLocation(0, fileSel.getHeight());
		diffView.setSize(getWidth(), getHeight()-fileSel.getHeight());
		diffView.layout();
	}
	
	public static void setDiff(FileDiffView viewer, String pathA, String pathB) {
		String[] linesA = viewer.linesA;
		if(pathA!=null)
			linesA = DiffView.loadLines(pathA, linesA);
		String[] linesB = viewer.linesB;
		if(pathB!=null)
			linesB = DiffView.loadLines(pathB, linesB);
		viewer.setDiff(linesA, linesB);
	}

}
