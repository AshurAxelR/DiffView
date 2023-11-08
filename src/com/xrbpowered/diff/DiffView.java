package com.xrbpowered.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.xrbpowered.diff.ui.FileDiffBase;
import com.xrbpowered.diff.ui.FolderDiffBase;
import com.xrbpowered.utils.ParseParams;
import com.xrbpowered.zoomui.swing.SwingFrame;
import com.xrbpowered.zoomui.swing.SwingWindowFactory;

public class DiffView {

	private static final int testEncodingLimit = 4096;
	
	public static String loadString(String path, boolean testEncoding) throws IOException, UnsupportedEncodingException {
		FileInputStream f = new FileInputStream(path);
		byte[] buf = new byte[f.available()];
		f.read(buf);
		f.close();
		String s = new String(buf, StandardCharsets.UTF_8);
		
		if(testEncoding) {
			byte[] test = s.getBytes(StandardCharsets.UTF_8);
			boolean valid = true;
			if(test.length==buf.length) {
				for(int i=0; i<testEncodingLimit && i<test.length; i++) {
					if(buf[i]!=test[i]) {
						valid = false;
						break;
					}
				}
			}
			else {
				valid = false;
			}
			if(!valid)
				throw new UnsupportedEncodingException();
		}
		
		return s;
	}

	private static boolean folder = false;
	private static String pathA = null;
	private static String pathB = null;

	public static void main(String[] args) {
		ParseParams params = new ParseParams();
		params.addStrParam(v -> pathA = v, "original");
		params.addStrParam(v -> pathB = v, "updated");
		params.addFlagParam("-r", v -> folder = v, "directory diff");
		params.addFlagParam("-gitignore", v -> FolderDiff.loadGitIgnore = v, "load .gitignore from directories");
		params.addFlagParam("-nodiffignore", v -> FolderDiff.loadGitIgnore = v, "do not load diff.ignore from directories");
		params.addStrParam("-i", v -> { Ignore.defaultIgnore = Ignore.load(new File(v), null, null); }, "global diff.ignore file");
		if(!params.parseParams(args))
			System.exit(-1);
		
		SwingFrame frame = new SwingFrame(SwingWindowFactory.use(),
				"DiffView - "+(folder ? "Directory" : "File"), 960, 720, true, false) {
			@Override
			public boolean onClosing() {
				confirmClosing();
				return false;
			}
		};
		if(folder)
			new FolderDiffBase(frame.getContainer()).fileSel.setPaths(pathA, pathB);
		else
			new FileDiffBase(frame.getContainer()).fileSel.setPaths(pathA, pathB);
		frame.show();
	}

}
