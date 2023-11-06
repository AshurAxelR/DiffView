package com.xrbpowered.diff.ui;

import java.io.File;

import com.xrbpowered.diff.DiffType;
import com.xrbpowered.diff.FolderDiff;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.std.UIListItem;
import com.xrbpowered.zoomui.std.UISplitContainer;

public class FolderDiffBase extends UIContainer implements DiffListener {

	public final FileSelectionPane fileSel;
	public final FolderDiffView folderDiffView;
	public final FileDiffView.Area diffView;
	
	private final UISplitContainer split;
	
	public FolderDiffBase(UIContainer parent) {
		super(parent);
		fileSel = new FileSelectionPane(parent, true).setDiffListener(this);
		split = new UISplitContainer(this, false, 0.25f);
		
		folderDiffView = new FolderDiffView(split.first) {
			@Override
			public void onItemSelected(UIListItem item) {
				FolderDiff.DiffItem diff = (FolderDiff.DiffItem) item.object;
				if(diff.isDir)
					diffView.viewer.setDiff(null, null);
				else {
					String fileA = diff.type==DiffType.inserted ? "" : new File(pathA.toFile(), diff.path.toString()).toPath().toString();
					String fileB = diff.type==DiffType.deleted ? "" : new File(pathB.toFile(), diff.path.toString()).toPath().toString();
					FileDiffBase.setDiff(diffView.viewer, fileA, fileB);
				}
			}
		};
		
		diffView = new FileDiffView.Area(split.second);
	}
	
	@Override
	public void setDiff(String pathA, String pathB) {
		if(pathA==null)
			pathA = folderDiffView.pathA.toString();
		if(pathB==null)
			pathB = folderDiffView.pathB.toString();
		folderDiffView.setDiff(pathA, pathB);
		diffView.viewer.setDiff(null, null);
	}
		
	@Override
	public void layout() {
		fileSel.layout();
		split.setLocation(0, fileSel.getHeight());
		split.setSize(getWidth(), getHeight()-fileSel.getHeight());
		split.layout();
	}

}
