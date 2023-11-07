package com.xrbpowered.diff;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Ignore {

	public static Ignore defaultIgnore = create(".git/");
	
	public final Ignore parent;
	
	private Pattern ignore;
	private Pattern ignoreDir;
	private Pattern allow;
	private Pattern allowDir;
	
	public Ignore(Ignore parent) {
		this.parent = parent;
	}
	
	private boolean match(Pattern p, String s) {
		return p!=null && p.matcher(s).matches();
	}

	public boolean match(String s, boolean isDir) {
		if(isDir && match(allowDir, s))
			return false;
		if(match(allow, s))
			return false;
		if(isDir && match(ignoreDir, s))
			return true;
		if(match(ignore, s))
			return true;
		
		if(parent!=null)
			return parent.match(s, isDir);
		
		return false;
	}

	public boolean match(Path path, boolean isDir) {
		return match(path.toString().replace(File.separator, "/"), isDir);
	}
	
	private static class Parser {
		public String prefix;
		public ArrayList<String> ignore = new ArrayList<>();
		public ArrayList<String> ignoreDir = new ArrayList<>();
		public ArrayList<String> allow = new ArrayList<>();
		public ArrayList<String> allowDir = new ArrayList<>();
		
		public Parser(String prefix) {
			this.prefix = prefix;
		}
		
		public void addLine(String line) {
			if(line.isEmpty() || line.startsWith("#"))
				return;
			
			if(line.startsWith("\\"))
				line = line.substring(1);
			if(line.endsWith("\\"))
				line = line.substring(0, line.length()-1);
			boolean not = false;
			if(line.startsWith("!")) {
				not = true;
				line = line.substring(1);
			}
			boolean dir = false;
			if(line.endsWith("/")) {
				dir = true;
				line = line.substring(0, line.length()-1);
			}
			boolean abs = false;
			if(line.contains("/")) {
				abs = true;
				if(line.startsWith("/"))
					line = line.substring(1);
			}
			if(line.contains("*")) {
				// TODO ignore pattern **
				line = line.replace("*", "\\E[^/]*\\Q");
			}
			line = line.replace("?", "\\E[^/]\\Q");
			if(!abs)
				line = "\\E(?:.*/)?\\Q"+line;
			if(prefix!=null)
				line = prefix + line;
			
			line = (String)("\\Q"+line+"\\E").replace("\\Q\\E", "");
			
			(dir ? (not ? allowDir : ignoreDir) : (not ? allow : ignore)).add(line);
		}
		
		private Pattern compile(ArrayList<String> list) {
			if(list.isEmpty())
				return null;
			StringBuilder sb = new StringBuilder();
			sb.append("^");
			boolean first = true;
			for(String s : list) {
				if(!first)
					sb.append("|");
				first = false;
				sb.append("(?:");
				sb.append(s);
				sb.append(")");
			}
			sb.append("$");
			return Pattern.compile(sb.toString());
		}
		
		public Ignore finish(Ignore parent) {
			Ignore res = new Ignore(parent);
			res.ignore = compile(ignore);
			res.ignoreDir = compile(ignoreDir);
			res.allow = compile(allow);
			res.allowDir = compile(allowDir);
			return res;
		}
	}
	
	public static Ignore load(File file, Path root, Ignore parent) {
		try(
			Scanner in = new Scanner(file);
		) {
			String prefix = null;
			if(root!=null) {
				Path p = root.relativize(file.toPath()).getParent();
				if(p!=null)
					prefix = p.toString().replace(File.separator, "/")+"/";
			}

			Parser parser = new Parser(prefix);
			while(in.hasNext()) {
				String line = in.nextLine().trim();
				parser.addLine(line);
			}
			return parser.finish(parent);
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
			return parent;
		}
	}

	public static Ignore create(String... lines) {
		Parser parser = new Parser(null);
		for(String line : lines) {
			parser.addLine(line);
		}
		return parser.finish(null);
	}
	
}
