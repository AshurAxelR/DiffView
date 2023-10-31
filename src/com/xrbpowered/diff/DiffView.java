package com.xrbpowered.diff;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.xrbpowered.diff.ui.FileDiffBase;
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
		try {
			String text = loadString(path, true);
			return (text==null) ? old : text.replace("\r", "").split("\n");
		}
		catch (IOException e) {
			UIMessageBox.show("Error", e.getMessage(),
					UIMessageBox.iconError, new MessageResult[] {MessageResult.ok}, null);
			return old;
		}
	}
	
	public static void main(String[] args) {
		SwingFrame frame = new SwingFrame(SwingWindowFactory.use(), "File DiffView", 960, 720, true, false) {
			@Override
			public boolean onClosing() {
				confirmClosing();
				return false;
			}
		};
		new FileDiffBase(frame.getContainer());
		frame.show();
	}

}
