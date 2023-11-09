package com.xrbpowered.diff.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import com.xrbpowered.diff.DiffType;
import com.xrbpowered.diff.FolderDiff;
import com.xrbpowered.diff.FolderDiff.DiffItem;
import com.xrbpowered.diff.Ignore;
import com.xrbpowered.utils.TaskInterruptedException;
import com.xrbpowered.utils.UISafeThread;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.std.UIListItem;
import com.xrbpowered.zoomui.std.UISplitContainer;

public class FolderDiffBase extends UIContainer implements DiffListener {

	private final class TaskThread extends UISafeThread {
		public final Path rootA, rootB;
		ArrayList<DiffItem> res = new ArrayList<>();
		
		public TaskThread(String pathA, String pathB) {
			this.rootA = FolderDiff.makeRoot(pathA);
			this.rootB = FolderDiff.makeRoot(pathB);
		}
		
		@Override
		public void run() {
			try {
				FolderDiff.compareFolders(rootA, rootA.toFile(), rootB, rootB.toFile(), Ignore.defaultIgnore, res);
				safeUIRunAsync();
			}
			catch (TaskInterruptedException e) {
			}
		}
		
		@Override
		protected void uiRun() {
			folderDiffView.setDiff(rootA, rootB, res);
			diffView.viewer.setDiff(null, null);
			error.show("Select a file in the list to view the difference.");
			repaint();
		}
	}
	
	public final FileSelectionPane fileSel;
	public final FolderDiffView folderDiffView;
	public final FileDiffView.Area diffView;
	public final ErrorOverlay error;
	
	private final UISplitContainer split;
	private TaskThread taskThread = null;
	
	public FolderDiffBase(UIContainer parent) {
		super(parent);
		fileSel = new FileSelectionPane(parent, true).setDiffListener(this);
		split = new UISplitContainer(this, false, 0.25f);
		
		folderDiffView = new FolderDiffView(split.first) {
			@Override
			public void onItemSelected(UIListItem item) {
				FolderDiff.DiffItem diff = (FolderDiff.DiffItem) item.object;
				if(diff.isDir) {
					diffView.viewer.setDiff(null, null);
					error.show(diff.path.getFileName().toString() + " is a directory.");
				}
				else {
					String fileA = diff.type==DiffType.inserted ? "" : new File(pathA.toFile(), diff.path.toString()).toPath().toString();
					String fileB = diff.type==DiffType.deleted ? "" : new File(pathB.toFile(), diff.path.toString()).toPath().toString();
					FileDiffBase.setDiff(diffView.viewer, fileA, fileB, error);
				}
			}
		};
		
		diffView = new FileDiffView.Area(split.second);
		error = new ErrorOverlay(split.second);
	}
	
	@Override
	public void setDiff(String pathA, String pathB) {
		if(pathA==null)
			pathA = folderDiffView.pathA.toString();
		if(pathB==null)
			pathB = folderDiffView.pathB.toString();
		
		diffView.viewer.setDiff(null, null);
		if(pathA!=null && pathB!=null) {
			if(taskThread!=null)
				taskThread.interrupt();
			taskThread = new TaskThread(pathA, pathB);
			taskThread.start();
		}
		else {
			folderDiffView.setDiff(pathA, pathB);
			error.show("Select a file in the list to view the difference.");
		}
	}
		
	@Override
	public void layout() {
		fileSel.layout();
		split.setLocation(0, fileSel.getHeight());
		split.setSize(getWidth(), getHeight()-fileSel.getHeight());
		split.layout();
	}

}
