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

public class FolderDiff {

	public static class DiffItem {
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
		
		public DiffItem setDir(File dir) {
			this.isDir = true;
			this.size = countFiles(dir);
			return this;
		}
	}
	
	private static int countFiles(File dir) {
		int sum = 0;
		for(String name : dir.list()) {
			if(name.equals(".") || name.equals(".."))
				continue;
			File f = new File(dir, name);
			if(f.isDirectory())
				sum += countFiles(f);
			else
				sum++;
		}
		return sum;
	}
	
	private static TreeSet<Path> listPaths(Path root, File dir) {
		TreeSet<Path> res = new TreeSet<>();
		for(String name : dir.list()) {
			if(name.equals(".") || name.equals(".."))
				continue;
			Path p = root.relativize(new File(dir, name).toPath());
			res.add(p);
		}
		return res;
	}
	
	private static boolean isModified(File fA, File fB) {
		try(
			FileInputStream inA = new FileInputStream(fA);
			FileInputStream inB = new FileInputStream(fB);
			DataInputStream dataA = new DataInputStream(new BufferedInputStream(inA));
			DataInputStream dataB = new DataInputStream(new BufferedInputStream(inB));
		) {
			if(inA.available()!=inB.available())
				return true;
			while(dataA.available()>0) {
				if(dataA.readByte()!=dataB.readByte())
					return true;
			}
			return false;
		}
		catch(IOException e) {
			System.err.println(e);
			return false;
		}
	}

	public static void compareFolders(Path rootA, File dirA, Path rootB, File dirB, ArrayList<DiffItem> res) {
		TreeSet<Path> setA = listPaths(rootA, dirA);
		TreeSet<Path> setB = listPaths(rootB, dirB);
		
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
					compareFolders(rootA, fA, rootB, fB, res);
					continue;
				}
				else if(isDirA) {
					res.add(new DiffItem(DiffType.deleted, p).setDir(fA));
					res.add(new DiffItem(DiffType.inserted, p));
				}
				else if(isDirB) {
					res.add(new DiffItem(DiffType.deleted, p));
					res.add(new DiffItem(DiffType.inserted, p).setDir(fB));
				}
				else {
					if(isModified(fA, fB))
						i.type = DiffType.modified;
				}
			}
			else if(i.type==DiffType.deleted && fA.isDirectory()) {
				i.setDir(fA);
			}
			else if(i.type==DiffType.inserted && fB.isDirectory()) {
				i.setDir(fB);
			}
			
			if(i.type!=DiffType.notChanged)
				res.add(i);
		}
	}
	
}
