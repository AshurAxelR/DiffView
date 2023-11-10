package com.xrbpowered.diff;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.TreeSet;

import com.xrbpowered.utils.TaskInterruptedException;

public class FolderDiff {

	public static boolean loadGitIgnore = false;
	public static boolean loadDiffIgnore = true;
	
	public static final int limitCompareFiles = 128*1024;
	
	public class DiffItem {
		public DiffType type;
		public Path path;
		public boolean isDir = false;
		public int size = 1;

		public DiffItem(DiffType type, Path p) {
			this.type = type;
			this.path = p;
		}

		public DiffItem(Path p, boolean inA, boolean inB) {
			if(inA && inB)
				this.type = DiffType.notChanged;
			else if(!inA)
				this.type = DiffType.inserted;
			else if(!inB)
				this.type = DiffType.deleted;
			else
				throw new InvalidParameterException();
			
			this.path = p;
		}
		
		public DiffItem setDir(Path root, File dir, Ignore ignore) {
			this.isDir = true;
			this.size = countFiles(root, dir, ignore);
			return this;
		}
	}
	
	public final Path rootA, rootB;
	public final ArrayList<DiffItem> res = new ArrayList<>();

	public int progress = 0;
	public String currentDir = "";
	
	public FolderDiff(String pathA, String pathB) {
		this.rootA = FolderDiff.makeRoot(pathA);
		this.rootB = FolderDiff.makeRoot(pathB);
	}
	
	public static Path makeRoot(String path) {
		return new File(path).toPath().toAbsolutePath().normalize();
	}
	
	private static String relativeName(Path root, File f) {
		String s = root.relativize(f.toPath()).toString().replace(File.separator, "/");
		return s.isEmpty() ? "." : s;
	}
	
	private int countFiles(Path root, File dir, Ignore ignore) {
		TaskInterruptedException.check();
		currentDir = relativeName(root, dir);
		
		String[] list = (dir==null) ? null : dir.list();
		if(list==null)
			return 0;
		
		ignore = expandIgnore(root, dir, ignore);
		
		int sum = 0;
		for(String name : list) {
			if(name.equals(".") || name.equals(".."))
				continue;
			File f = new File(dir, name);
			boolean isDir = f.isDirectory();
			if(ignore!=null) {
				Path p = root.relativize(f.toPath());
				if(ignore.match(p, f.isDirectory()))
					continue;
			}
			if(isDir)
				sum += countFiles(root, f, ignore);
			else
				sum++;
			progress++;
		}
		return sum;
	}
	
	private static TreeSet<Path> listPaths(Path root, File dir, Ignore ignore) {
		TreeSet<Path> res = new TreeSet<>();
		String[] list = (dir==null) ? null : dir.list();
		if(list==null)
			return res;
		for(String name : list) {
			if(name.equals(".") || name.equals(".."))
				continue;
			File f = new File(dir, name);
			Path p = root.relativize(f.toPath());
			if(ignore!=null && ignore.match(p, f.isDirectory()))
				continue;
			res.add(p);
		}
		return res;
	}
	
	private static boolean isModified(File fA, File fB) {
		try(
			FileInputStream inA = new FileInputStream(fA);
			FileInputStream inB = new FileInputStream(fB);
			DataInputStream dataA = new DataInputStream(new BufferedInputStream(inA, limitCompareFiles));
			DataInputStream dataB = new DataInputStream(new BufferedInputStream(inB, limitCompareFiles));
		) {
			if(inA.available()!=inB.available())
				return true;
			int bytes = 0;
			while(dataA.available()>0) {
				if(dataA.readByte()!=dataB.readByte())
					return true;
				bytes++;
				if(bytes>limitCompareFiles)
					break;
			}
			return false;
		}
		catch(IOException e) {
			System.err.println(e);
			return false;
		}
	}
	
	private static Ignore expandIgnore(Path root, File dir, Ignore ignore) {
		if(loadGitIgnore) {
			File ignoreFile = new File(dir, ".gitignore");
			if(ignoreFile.exists())
				ignore = Ignore.load(ignoreFile, root, ignore);
		}
		if(loadDiffIgnore) {
			File ignoreFile = new File(dir, "diff.ignore");
			if(ignoreFile.exists())
				ignore = Ignore.load(ignoreFile, root, ignore);
		}
		return ignore;
	}

	public void compareFolders(File dirA, File dirB, Ignore ignore) {
		TaskInterruptedException.check();
		currentDir = relativeName(rootB, dirB);

		ignore = expandIgnore(rootB, dirB, ignore);
		
		TreeSet<Path> setA = listPaths(rootA, dirA, ignore);
		TreeSet<Path> setB = listPaths(rootB, dirB, ignore);
		
		TreeSet<Path> union = new TreeSet<>();
		union.addAll(setA);
		union.addAll(setB);
		
		for(Path p : union) {
			DiffItem i = new DiffItem(p, setA.contains(p), setB.contains(p));
			
			String name = p.toFile().getName();
			File fA = new File(dirA, name);
			File fB = new File(dirB, name);

			if(i.type==DiffType.notChanged) {
				boolean isDirA = fA.isDirectory();
				boolean isDirB = fB.isDirectory();
				if(isDirA && isDirB) {
					compareFolders(fA, fB, ignore);
					continue;
				}
				else if(isDirA) {
					res.add(new DiffItem(DiffType.deleted, p).setDir(rootA, fA, ignore));
					res.add(new DiffItem(DiffType.inserted, p));
				}
				else if(isDirB) {
					res.add(new DiffItem(DiffType.deleted, p));
					res.add(new DiffItem(DiffType.inserted, p).setDir(rootB, fB, ignore));
				}
				else {
					if(isModified(fA, fB))
						i.type = DiffType.modified;
				}
			}
			else if(i.type==DiffType.deleted && fA.isDirectory()) {
				i.setDir(rootA, fA, ignore);
			}
			else if(i.type==DiffType.inserted && fB.isDirectory()) {
				i.setDir(rootB, fB, ignore);
			}
			
			progress++;
			if(i.type!=DiffType.notChanged)
				res.add(i);
		}
	}
	
	public void compareFolders(Ignore ignore) {
		progress = 0;
		currentDir = "";
		res.clear();
		compareFolders(rootA.toFile(), rootB.toFile(), ignore);
	}

}
