/*
	BSD 3-Clause License
	
	Copyright (c) 2023, Ashur Rafiev
	Copyright (c) 2023, Matthias Hertel
	
	Redistribution and use in source and binary forms, with or without
	modification, are permitted provided that the following conditions are met:
	
	1. Redistributions of source code must retain the above copyright notice, this
	   list of conditions and the following disclaimer.
	
	2. Redistributions in binary form must reproduce the above copyright notice,
	   this list of conditions and the following disclaimer in the documentation
	   and/or other materials provided with the distribution.
	
	3. Neither the name of the copyright holder nor the names of its
	   contributors may be used to endorse or promote products derived from
	   this software without specific prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
	AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
	IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
	FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
	DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
	SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
	CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
	OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
	OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.xrbpowered.diff;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This Class implements the Difference Algorithm published in "An O(ND)
 * Difference Algorithm and its Variations" by Eugene Myers Algorithmica Vol. 1
 * No. 2, 1986, p 251.
 * 
 * Java implementation is derived from the following C# implementation under a
 * BSD-3 lisense: https://github.com/mathertel/Diff/blob/main/Diff.cs
 */
public class Diff {

	public enum DiffType {
		notChanged, deleted, inserted;
	}

	/**
	 * details of one difference
	 */
	public static class DiffChunk {
		public final DiffType type;
		public final int startA, startB;
		public final int length;

		public DiffChunk(DiffType type, int startA, int startB, int length) {
			this.type = type;
			this.startA = startA;
			this.startB = startB;
			this.length = length;
		}
	}

	/**
	 * Shortest Middle Snake Return Data
	 */
	private static class MiddleSnakeData {
		public int x, y;
	}

	/**
	 * Find the difference in 2 text documents, comparing by textlines. The
	 * algorithm itself is comparing 2 arrays of numbers so when comparing 2 text
	 * documents each line is converted into a (hash) number. This hash-value is
	 * computed by storing all textlines into a common hashtable so i can find
	 * duplicates in there, and generating a new number each time a new textline is
	 * inserted.
	 * 
	 * @param linesA      A-version of the text (usually the old one)
	 * @param linesB      B-version of the text (usually the new one)
	 * @param trimSpace   When set to true, all leading and trailing whitespace
	 *                    characters are stripped out before the comparision is
	 *                    done.
	 * @param ignoreSpace When set to true, all whitespace characters are converted
	 *                    to a single space character before the comparision is
	 *                    done.
	 * @param ignoreCase  When set to true, all characters are converted to their
	 *                    lowercase equivalence before the comparision is done.
	 * @return Returns a array of Items that describe the differences.
	 */
	public static ArrayList<DiffChunk> diffText(String[] linesA, String[] linesB) {
		// prepare the input-text and convert to comparable numbers.
		HashMap<String, Integer> h = new HashMap<>(linesA.length+linesB.length);

		// The A-Version of the data (original data) to be compared.
		DiffData dataA = new DiffData(diffCodes(linesA, h));

		// The B-Version of the data (modified data) to be compared.
		DiffData dataB = new DiffData(diffCodes(linesB, h));

		// free up hashtable memory (maybe)
		h.clear();

		int max = dataA.length+dataB.length+1;
		// vector for the (0,0) to (x,y) search
		int[] downVector = new int[2*max+2];
		// vector for the (u,v) to (N,M) search
		int[] upVector = new int[2*max+2];

		lcs(dataA, 0, dataA.length, dataB, 0, dataB.length, downVector, upVector);

		optimize(dataA);
		optimize(dataB);
		return createDiffs(dataA, dataB);
	}

	/**
	 * If a sequence of modified lines starts with a line that contains the same
	 * content as the line that appends the changes, the difference sequence is
	 * modified so that the appended line and not the starting line is marked as
	 * modified. This leads to more readable diff sequences when comparing text
	 * files.
	 * 
	 * @param data A Diff data buffer containing the identified changes.
	 */
	private static void optimize(DiffData data) {
		int startPos, endPos;

		startPos = 0;
		while(startPos<data.length) {
			while((startPos<data.length) && (data.modified[startPos]==false))
				startPos++;
			endPos = startPos;
			while((endPos<data.length) && (data.modified[endPos]==true))
				endPos++;

			if((endPos<data.length) && (data.data[startPos]==data.data[endPos])) {
				data.modified[startPos] = false;
				data.modified[endPos] = true;
			}
			else {
				startPos = endPos;
			}
		}
	}

	/**
	 * This function converts all textlines of the text into unique numbers for
	 * every unique textline so further work can work only with simple numbers.
	 * 
	 * @param lines the input text
	 * @param h     This externally initialized hashtable is used for storing all
	 *              ever used textlines.
	 * @return an array of integers.
	 */
	private static int[] diffCodes(String[] lines, HashMap<String, Integer> h) {
		// get all codes of the text
		int[] codes = new int[lines.length];
		int lastUsedCode = h.size();
		for(int i = 0; i<lines.length; ++i) {
			String s = lines[i];
			Integer code = h.get(s);
			if(code==null) {
				lastUsedCode++;
				code = lastUsedCode;
				h.put(s, code);
			}
			codes[i] = code;
		}
		return (codes);
	}

	/**
	 * This is the algorithm to find the Shortest Middle Snake (SMS).
	 * 
	 * @param dataA      sequence A
	 * @param lowerA     lower bound of the actual range in DataA
	 * @param upperA     upper bound of the actual range in DataA (exclusive)
	 * @param dataB      sequence B
	 * @param lowerB     lower bound of the actual range in DataB
	 * @param upperB     upper bound of the actual range in DataB (exclusive)
	 * @param downVector a vector for the (0,0) to (x,y) search. Passed as a
	 *                   parameter for speed reasons.
	 * @param upVector   a vector for the (u,v) to (N,M) search. Passed as a
	 *                   parameter for speed reasons.
	 * @return a MiddleSnakeData record containing x,y and u,v
	 */
	private static MiddleSnakeData sms(DiffData dataA, int lowerA, int upperA, DiffData dataB, int lowerB, int upperB,
			int[] downVector, int[] upVector) {

		MiddleSnakeData ret = new MiddleSnakeData();
		int max = dataA.length+dataB.length+1;

		int downK = lowerA-lowerB; // the k-line to start the forward search
		int upK = upperA-upperB; // the k-line to start the reverse search

		int delta = (upperA-lowerA)-(upperB-lowerB);
		boolean oddDelta = (delta & 1)!=0;

		// The vectors in the publication accepts negative indexes. the vectors
		// implemented here are 0-based
		// and are access using a specific offset: UpOffset UpVector and DownOffset for
		// DownVector
		int downOffset = max-downK;
		int upOffset = max-upK;

		int maxD = ((upperA-lowerA+upperB-lowerB)/2)+1;

		// Debug.Write(2, "SMS", String.Format("Search the box: A[{0}-{1}] to
		// B[{2}-{3}]", LowerA, UpperA, LowerB, UpperB));

		// init vectors
		downVector[downOffset+downK+1] = lowerA;
		upVector[upOffset+upK-1] = upperA;

		for(int d = 0; d<=maxD; d++) {
			// Extend the forward path.
			for(int k = downK-d; k<=downK+d; k += 2) {
				// Debug.Write(0, "SMS", "extend forward path " + k.ToString());

				// find the only or better starting point
				int x, y;
				if(k==downK-d) {
					x = downVector[downOffset+k+1]; // down
				}
				else {
					x = downVector[downOffset+k-1]+1; // a step to the right
					if((k<downK+d) && (downVector[downOffset+k+1]>=x))
						x = downVector[downOffset+k+1]; // down
				}
				y = x-k;

				// find the end of the furthest reaching forward D-path in diagonal k.
				while((x<upperA) && (y<upperB) && (dataA.data[x]==dataB.data[y])) {
					x++;
					y++;
				}
				downVector[downOffset+k] = x;

				// overlap ?
				if(oddDelta && (upK-d<k) && (k<upK+d)) {
					if(upVector[upOffset+k]<=downVector[downOffset+k]) {
						ret.x = downVector[downOffset+k];
						ret.y = downVector[downOffset+k]-k;
						// ret.u = UpVector[UpOffset + k]; // 2002.09.20: no need for 2 points
						// ret.v = UpVector[UpOffset + k] - k;
						return (ret);
					}
				}
			}

			// Extend the reverse path.
			for(int k = upK-d; k<=upK+d; k += 2) {
				// Debug.Write(0, "SMS", "extend reverse path " + k.ToString());

				// find the only or better starting point
				int x, y;
				if(k==upK+d) {
					x = upVector[upOffset+k-1]; // up
				}
				else {
					x = upVector[upOffset+k+1]-1; // left
					if((k>upK-d) && (upVector[upOffset+k-1]<x))
						x = upVector[upOffset+k-1]; // up
				}
				y = x-k;

				while((x>lowerA) && (y>lowerB) && (dataA.data[x-1]==dataB.data[y-1])) {
					x--;
					y--; // diagonal
				}
				upVector[upOffset+k] = x;

				// overlap ?
				if(!oddDelta && (downK-d<=k) && (k<=downK+d)) {
					if(upVector[upOffset+k]<=downVector[downOffset+k]) {
						ret.x = downVector[downOffset+k];
						ret.y = downVector[downOffset+k]-k;
						// ret.u = UpVector[UpOffset + k]; // 2002.09.20: no need for 2 points
						// ret.v = UpVector[UpOffset + k] - k;
						return (ret);
					}
				}
			}
		}

		throw new RuntimeException("the algorithm should never come here.");
	}

	/**
	 * This is the divide-and-conquer implementation of the longest
	 * common-subsequence (LCS) algorithm. The published algorithm passes
	 * recursively parts of the A and B sequences. To avoid copying these arrays the
	 * lower and upper bounds are passed while the sequences stay constant.
	 * 
	 * @param dataA      sequence A
	 * @param lowerA     lower bound of the actual range in DataA
	 * @param upperA     upper bound of the actual range in DataA (exclusive)
	 * @param dataB      sequence B
	 * @param lowerB     lower bound of the actual range in DataB
	 * @param upperB     upper bound of the actual range in DataB (exclusive)
	 * @param downVector a vector for the (0,0) to (x,y) search. Passed as a
	 *                   parameter for speed reasons.
	 * @param upVector   a vector for the (u,v) to (N,M) search. Passed as a
	 *                   parameter for speed reasons.
	 */
	private static void lcs(DiffData dataA, int lowerA, int upperA, DiffData dataB, int lowerB, int upperB,
			int[] downVector, int[] upVector) {
		// Debug.Write(2, "LCS", String.Format("Analyse the box: A[{0}-{1}] to
		// B[{2}-{3}]", LowerA, UpperA, LowerB, UpperB));

		// Fast walkthrough equal lines at the start
		while(lowerA<upperA && lowerB<upperB && dataA.data[lowerA]==dataB.data[lowerB]) {
			lowerA++;
			lowerB++;
		}

		// Fast walkthrough equal lines at the end
		while(lowerA<upperA && lowerB<upperB && dataA.data[upperA-1]==dataB.data[upperB-1]) {
			--upperA;
			--upperB;
		}

		if(lowerA==upperA) {
			// mark as inserted lines.
			while(lowerB<upperB)
				dataB.modified[lowerB++] = true;

		}
		else if(lowerB==upperB) {
			// mark as deleted lines.
			while(lowerA<upperA)
				dataA.modified[lowerA++] = true;

		}
		else {
			// Find the middle snake and length of an optimal path for A and B
			MiddleSnakeData smsrd = sms(dataA, lowerA, upperA, dataB, lowerB, upperB, downVector, upVector);
			// Debug.Write(2, "MiddleSnakeData", String.Format("{0},{1}", smsrd.x,
			// smsrd.y));

			// The path is from LowerX to (x,y) and (x,y) to UpperX
			lcs(dataA, lowerA, smsrd.x, dataB, lowerB, smsrd.y, downVector, upVector);
			lcs(dataA, smsrd.x, upperA, dataB, smsrd.y, upperB, downVector, upVector);
		}
	}

	/**
	 * Scan the tables of which lines are inserted and deleted, producing an edit
	 * script in forward order.
	 * 
	 * @return dynamic array
	 */
	private static ArrayList<DiffChunk> createDiffs(DiffData dataA, DiffData dataB) {
		ArrayList<DiffChunk> a = new ArrayList<>();

		int startA, startB;
		int lineA, lineB;

		lineA = 0;
		lineB = 0;
		while(lineA<dataA.length || lineB<dataB.length) {
			// equal lines
			startA = lineA;
			startB = lineB;
			while((lineA<dataA.length) && (!dataA.modified[lineA])
					&& (lineB<dataB.length) && (!dataB.modified[lineB])) {
				lineA++;
				lineB++;
			}
			if((startA<lineA) || (startB<lineB)) {
				a.add(new DiffChunk(DiffType.notChanged, startA, startB, lineA-startA));
			}

			// deleted lines
			startA = lineA;
			startB = lineB;
			while(lineA<dataA.length && (lineB>=dataB.length || dataA.modified[lineA])) {
				lineA++;
			}
			if((startA<lineA) || (startB<lineB)) {
				a.add(new DiffChunk(DiffType.deleted, startA, startB, lineA-startA));
			}

			// inserted lines
			startA = lineA;
			startB = lineB;
			while(lineB<dataB.length && (lineA>=dataA.length || dataB.modified[lineB])) {
				lineB++;
			}
			if((startA<lineA) || (startB<lineB)) {
				a.add(new DiffChunk(DiffType.inserted, startA, startB, lineB-startB));
			}
		}

		return a;
	}

	/**
	 * Data on one input file being compared.
	 */
	private static class DiffData {
		// Number of elements (lines).
		public int length;

		// Buffer of numbers that will be compared.
		public int[] data;

		/**
		 * Array of booleans that flag for modified data. This is the result of the
		 * diff. This means deletedA in the first Data or inserted in the second Data.
		 */
		public boolean[] modified;

		/**
		 * Initialize the Diff-Data buffer.
		 * 
		 * @param initData reference to the buffer
		 */
		public DiffData(int[] initData) {
			data = initData;
			length = initData.length;
			modified = new boolean[length+2];
		}
	}

}
