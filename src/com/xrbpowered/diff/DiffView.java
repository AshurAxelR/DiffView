package com.xrbpowered.diff;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.xrbpowered.diff.ui.FileDiffBase;
import com.xrbpowered.diff.ui.FolderDiffBase;
import com.xrbpowered.zoomui.std.UIMessageBox;
import com.xrbpowered.zoomui.std.UIMessageBox.MessageResult;
import com.xrbpowered.zoomui.swing.SwingFrame;
import com.xrbpowered.zoomui.swing.SwingWindowFactory;

public class DiffView {

	private static final int testEncodingLimit = 4096;
	
	public static String loadString(String path, boolean testEncoding) throws IOException {
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
				throw new IOException("Not a valid UTF-8 text");
		}
		
		return s;
	}
	
	public static String[] loadLines(String path, String[] old) {
		if(path==null || path.isEmpty())
			return new String[] {};
		try {
			String text = loadString(path, true);
			return (text==null) ? old : text.replace("\r", "").split("\n");
		}
		catch (IOException e) {
			String msg = e.getMessage().replaceAll("[\\\\\\/]", "$0<span></span>"); // <wbr> not working
			UIMessageBox.show("Error", msg,
					UIMessageBox.iconError, new MessageResult[] {MessageResult.ok}, null);
			return old;
		}
	}

	public static void main(String[] args) {
		boolean folder = false;
		String pathA = null;
		String pathB = null;
		
		for(int i=0; i<args.length; i++) {
			switch(args[i]) {
				case "-r":
					folder = true;
					break;
				default:
					if(pathA==null)
						pathA = args[i];
					else if(pathB==null)
						pathB = args[i];
					else {
						System.err.println("Too many arguments");
						System.exit(-1);
					}
					break;
			}
		}
		
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
