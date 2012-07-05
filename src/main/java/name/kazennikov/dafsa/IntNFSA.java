package name.kazennikov.dafsa;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.DataInputStream;
import java.io.IOException;




public class IntNFSA {
	TIntObjectHashMap<TIntSet> finals = new TIntObjectHashMap<TIntSet>();
	
	// map (current, inChar) -> (index, length)
	final TLongLongHashMap stateHash = new TLongLongHashMap();
	
	// using 2 arrays instead of array of pairs (outChar, nextState)
	final TIntArrayList outChars = new TIntArrayList();
	final TIntArrayList nextStates = new TIntArrayList();
	
	
//	@Override
//	public void addFinals(int state, int[] finals) {
//		this.finals.add(finals);
//	}


	public void addTransition(int srcState, char input, char output, int destState) {
		long key = key(srcState, input);
		stateHash.adjustOrPutValue(key, 1, ((long)outChars.size() << 32) + 1);
		outChars.add(output);
		nextStates.add(destState);
	}
	
	long key(int state, int input){
		return ((long)input << 32) + state;
	}
	

	
	/**
	 * Get final features of a state
	 * @return null, if state doesn't have final features
	 */
	public TIntSet getFinals(int state) {
		return finals.get(state);
	}
	
	/**
	 * Add a final feature to the state
	 * @param state state number
	 * @param fin final feature
	 */
	public void addFinal(int state, int fin) {
		TIntSet stateFinals = finals.get(state);
		
		if(stateFinals == null) {
			stateFinals = new TIntHashSet();
			finals.put(state, stateFinals);
		}
		
		stateFinals.add(fin);
	}
	
//	public long getKey(int srcState, int label) {
//		long key = srcState;
//		key <<= 32;
//		key += label;
//		return key;
//	}
	
//	public char getInputLabel(long key) {
//		return (char)(key & 0xffffL);
//	}
//	
//	public char getOutputLabel(long key) {
//		return (char)( (key & 0xffffffffL) >>> 16);
//	}

	
	public int getState(long key) {
		return (int)(key >>> 32);
	}
	
	public long getTransitionsInfo(int state, int in) {
		return stateHash.get(key(state, in));
	}
	
	public int getTransitionsStart(long key) {
		return (int)(key >>> 32);
	}
	
	public int getTransitionsLength(long key) {
		return (int)(key & 0xFFFFFFFFL);
	}
	
	public int getTransitionOut(int index) {
		return outChars.get(index);
	}
	
	public int getTransitionNext(int index) {
		return nextStates.get(index);
	}



	
	
	
	
	
	public static class IntNFSABuilder implements IntTrie.Builder<IntNFSA> {
		IntNFSA nfsa;
		int state = 0;
		

		@Override
		public void states(int states) throws IOException {
			nfsa = new IntNFSA();
		}

		@Override
		public void state(int state) throws IOException {
			this.state = state;
		}

		@Override
		public void finals(int n) throws IOException {
		}

		@Override
		public void stateFinal(int fin) throws IOException {
			nfsa.addFinal(state, fin);
		}

		@Override
		public void transitions(int n) throws IOException {
		}

		@Override
		public void transition(int input, int dest) throws IOException {
			char in = (char)( input >> 16);
			char out = (char) (input & 0xffff);
			nfsa.addTransition(state, in, out, dest);
		}

		@Override
		public IntNFSA build() {
			return nfsa;
		}

		@Override
		public void startState() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endState() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startFinals() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endFinals() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startTransitions() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endTransitions() {
			// TODO Auto-generated method stub
			
		}
		
	}


	public class Walker implements CharFSTWalker {
		
		protected void walkIterativeInternal(CharSequence s, StringBuilder sb, int startIndex, int endIndex, int currentIndex, 
				int state, char ch, Processor parseProcessor) {
			long value = getTransitionsInfo(state, ch);
			int start = getTransitionsStart(value);
			int end = start + getTransitionsLength(value);
			
			while(start < end) {
				
				char outCh = (char) getTransitionOut(start);
				int nextState = getTransitionNext(start);
				if(outCh != 0)
					sb.append(outCh);
				int nextIndex = currentIndex;
				// do not jump to next char on null char walk
				if(ch != 0) {
					nextIndex = currentIndex != endIndex? currentIndex + 1: endIndex;
				}
				walkIterative(s, sb, startIndex, endIndex, nextIndex, nextState, parseProcessor);
				
				
				if(outCh != 0)
					sb.deleteCharAt(sb.length() - 1);

				start++;
			}
		}
		
		public void walkIterative(CharSequence s, StringBuilder sb, int startIndex, int endIndex, int currentIndex, 
				int state, Processor parseProcessor) {
			TIntSet fin = getFinals(state);

				if(fin != null && !fin.isEmpty()) {
					System.out.printf("state %d: ", state);
					parseProcessor.parse(s, sb, startIndex, currentIndex, fin);
				}
				
				char ch = currentIndex < endIndex? s.charAt(currentIndex) : 0;
								
				walkIterativeInternal(s, sb, startIndex, endIndex, currentIndex, state, ch, parseProcessor);
				
				char toUpper = Character.toUpperCase(ch);
				if(toUpper != ch) {
					walkIterativeInternal(s, sb, startIndex, endIndex, currentIndex, state, toUpper, parseProcessor);
				}
				
				// for single-pass morphan on fst only
				walkIterativeInternal(s, sb, startIndex, endIndex, currentIndex, state, (char)0, parseProcessor);
		}


		@Override
		public void walk(CharSequence src, int start, int end, Processor proc) {
			walkIterative(src, new StringBuilder(), start, end, start, 1, proc);
		}
		
	}
	
	public CharFSTWalker makeFSTWalker() {
		return new Walker();
	}
}
