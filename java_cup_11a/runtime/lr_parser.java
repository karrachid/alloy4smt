				    
package java_cup_11a.runtime;

import java.util.Stack;

/** This class implements a skeleton table driven LR parser.  In general,
 *  LR parsers are a form of bottom up shift-reduce parsers.  Shift-reduce
 *  parsers act by shifting input onto a parse stack until the Symbols 
 *  matching the right hand side of a production appear on the top of the 
 *  stack.  Once this occurs, a reduce is performed.  This involves removing
 *  the Symbols corresponding to the right hand side of the production
 *  (the so called "handle") and replacing them with the non-terminal from
 *  the left hand side of the production.  <p>
 *
 *  To control the decision of whether to shift or reduce at any given point, 
 *  the parser uses a state machine (the "viable prefix recognition machine" 
 *  built by the parser generator).  The current state of the machine is placed
 *  on top of the parse stack (stored as part of a Symbol object representing
 *  a terminal or non terminal).  The parse action table is consulted 
 *  (using the current state and the current lookahead Symbol as indexes) to 
 *  determine whether to shift or to reduce.  When the parser shifts, it 
 *  changes to a new state by pushing a new Symbol (containing a new state) 
 *  onto the stack.  When the parser reduces, it pops the handle (right hand 
 *  side of a production) off the stack.  This leaves the parser in the state 
 *  it was in before any of those Symbols were matched.  Next the reduce-goto 
 *  table is consulted (using the new state and current lookahead Symbol as 
 *  indexes) to determine a new state to go to.  The parser then shifts to 
 *  this goto state by pushing the left hand side Symbol of the production 
 *  (also containing the new state) onto the stack.<p>
 *
 *  This class actually provides four LR parsers.  The methods parse() and 
 *  debug_parse() provide two versions of the main parser (the only difference 
 *  being that debug_parse() emits debugging trace messages as it parses).  
 *  In addition to these main parsers, the error recovery mechanism uses two 
 *  more.  One of these is used to simulate "parsing ahead" in the input 
 *  without carrying out actions (to verify that a potential error recovery 
 *  has worked), and the other is used to parse through buffered "parse ahead" 
 *  input in order to execute all actions and re-synchronize the actual parser 
 *  configuration.<p>
 *
 *  This is an abstract class which is normally filled out by a subclass
 *  generated by the JavaCup parser generator.  In addition to supplying
 *  the actual parse tables, generated code also supplies methods which 
 *  invoke various pieces of user supplied code, provide access to certain
 *  special Symbols (e.g., EOF and error), etc.  Specifically, the following
 *  abstract methods are normally supplied by generated code:
 *  <dl compact>
 *  <dt> short[][] production_table()
 *  <dd> Provides a reference to the production table (indicating the index of
 *       the left hand side non terminal and the length of the right hand side
 *       for each production in the grammar).
 *  <dt> short[][] action_table()
 *  <dd> Provides a reference to the parse action table.
 *  <dt> short[][] reduce_table()
 *  <dd> Provides a reference to the reduce-goto table.
 *  <dt> int start_state()      
 *  <dd> Indicates the index of the start state.
 *  <dt> int start_production() 
 *  <dd> Indicates the index of the starting production.
 *  <dt> int EOF_sym() 
 *  <dd> Indicates the index of the EOF Symbol.
 *  <dt> int error_sym() 
 *  <dd> Indicates the index of the error Symbol.
 *  <dt> Symbol do_action() 
 *  <dd> Executes a piece of user supplied action code.  This always comes at 
 *       the point of a reduce in the parse, so this code also allocates and 
 *       fills in the left hand side non terminal Symbol object that is to be 
 *       pushed onto the stack for the reduce.
 *  <dt> void init_actions()
 *  <dd> Code to initialize a special object that encapsulates user supplied
 *       actions (this object is used by do_action() to actually carry out the 
 *       actions).
 *  </dl>
 *  
 *  In addition to these routines that <i>must</i> be supplied by the 
 *  generated subclass there are also a series of routines that <i>may</i> 
 *  be supplied.  These include:
 *  <dl>
 *  <dt> Symbol scan()
 *  <dd> Used to get the next input Symbol from the scanner.
 *  <dt> Scanner getScanner()
 *  <dd> Used to provide a scanner for the default implementation of
 *       scan().
 *  <dt> int error_sync_size()
 *  <dd> This determines how many Symbols past the point of an error 
 *       must be parsed without error in order to consider a recovery to 
 *       be valid.  This defaults to 3.  Values less than 2 are not 
 *       recommended.
 *  <dt> void report_error(String message, Object info)
 *  <dd> This method is called to report an error.  The default implementation
 *       simply prints a message to System.err and where the error occurred.
 *       This method is often replaced in order to provide a more sophisticated
 *       error reporting mechanism.
 *  <dt> void report_fatal_error(String message, Object info)
 *  <dd> This method is called when a fatal error that cannot be recovered from
 *       is encountered.  In the default implementation, it calls 
 *       report_error() to emit a message, then throws an exception.
 *  <dt> void syntax_error(Symbol cur_token)
 *  <dd> This method is called as soon as syntax error is detected (but
 *       before recovery is attempted).  In the default implementation it 
 *       invokes: report_error("Syntax error", null);
 *  <dt> void unrecovered_syntax_error(Symbol cur_token)
 *  <dd> This method is called if syntax error recovery fails.  In the default
 *       implementation it invokes:<br> 
 *         report_fatal_error("Couldn't repair and continue parse", null);
 *  </dl>
 *
 * @see     java_cup_11a.runtime.Symbol
 * @see     java_cup_11a.runtime.Symbol
 * @see     java_cup_11a.runtime.virtual_parse_stack
 * @version last updated: 7/3/96
 * @author  Frank Flannery
 */

@SuppressWarnings("unchecked")
public abstract class lr_parser {
    /*-----------------------------------------------------------*/
    /*--- Constructor(s) ----------------------------------------*/
    /*-----------------------------------------------------------*/

    /** 
     * Simple constructor. 
     */
    public lr_parser() {
    }
    
    /** 
     * Constructor that sets the default scanner. [CSA/davidm] 
     */
    public lr_parser(Scanner s) {
        this(s,new SymbolFactory()); // TUM 20060327 old cup v10 Symbols as default
    }
    /** 
     * Constructor that sets the default scanner and a SymbolFactory
     */
    public lr_parser(Scanner s, SymbolFactory symfac) {
        this(); // in case default constructor someday does something
        symbolFactory = symfac;
        setScanner(s);
    }
    public SymbolFactory symbolFactory;// = new DefaultSymbolFactory();
    /**
     * Whenever creation of a new Symbol is necessary, one should use this factory.
     */
    public SymbolFactory getSymbolFactory(){
        return symbolFactory;
    }
  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /** The default number of Symbols after an error we much match to consider 
   *  it recovered from. 
   */
  protected final static int _error_sync_size = 3;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The number of Symbols after an error we much match to consider it 
   *  recovered from. 
   */
  protected int error_sync_size() {return _error_sync_size; }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** Table of production information (supplied by generated subclass).
   *  This table contains one entry per production and is indexed by 
   *  the negative-encoded values (reduce actions) in the action_table.  
   *  Each entry has two parts, the index of the non-terminal on the 
   *  left hand side of the production, and the number of Symbols 
   *  on the right hand side. 
   */
  public abstract short[][] production_table();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The action table (supplied by generated subclass).  This table is
   *  indexed by state and terminal number indicating what action is to
   *  be taken when the parser is in the given state (i.e., the given state 
   *  is on top of the stack) and the given terminal is next on the input.  
   *  States are indexed using the first dimension, however, the entries for 
   *  a given state are compacted and stored in adjacent index, value pairs 
   *  which are searched for rather than accessed directly (see get_action()).  
   *  The actions stored in the table will be either shifts, reduces, or 
   *  errors.  Shifts are encoded as positive values (one greater than the 
   *  state shifted to).  Reduces are encoded as negative values (one less 
   *  than the production reduced by).  Error entries are denoted by zero. 
   * 
   * @see java_cup_11a.runtime.lr_parser#get_action
   */
  public abstract short[][] action_table();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The reduce-goto table (supplied by generated subclass).  This
   *  table is indexed by state and non-terminal number and contains
   *  state numbers.  States are indexed using the first dimension, however,
   *  the entries for a given state are compacted and stored in adjacent
   *  index, value pairs which are searched for rather than accessed 
   *  directly (see get_reduce()).  When a reduce occurs, the handle 
   *  (corresponding to the RHS of the matched production) is popped off 
   *  the stack.  The new top of stack indicates a state.  This table is 
   *  then indexed by that state and the LHS of the reducing production to 
   *  indicate where to "shift" to. 
   *
   * @see java_cup_11a.runtime.lr_parser#get_reduce
   */
  public abstract short[][] reduce_table();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The index of the start state (supplied by generated subclass). */
  public abstract int start_state();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The index of the start production (supplied by generated subclass). */
  public abstract int start_production();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The index of the end of file terminal Symbol (supplied by generated 
   *  subclass). 
   */
  public abstract int EOF_sym();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The index of the special error Symbol (supplied by generated subclass). */
  public abstract int error_sym();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Internal flag to indicate when parser should quit. */
  protected boolean _done_parsing = false;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** This method is called to indicate that the parser should quit.  This is 
   *  normally called by an accept action, but can be used to cancel parsing 
   *  early in other circumstances if desired. 
   */
  public void done_parsing()
    {
      _done_parsing = true;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/
  /* Global parse state shared by parse(), error recovery, and 
   * debugging routines */
  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Indication of the index for top of stack (for use by actions). */
  protected int tos;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The current lookahead Symbol. */
  protected Symbol cur_token;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The parse stack itself. */
  protected Stack stack = new Stack();

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Direct reference to the production table. */ 
  protected short[][] production_tab;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Direct reference to the action table. */
  protected short[][] action_tab;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Direct reference to the reduce-goto table. */
  protected short[][] reduce_tab;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** This is the scanner object used by the default implementation
   *  of scan() to get Symbols.  To avoid name conflicts with existing
   *  code, this field is private. [CSA/davidm] */
  private Scanner _scanner;

  /**
   * Simple accessor method to set the default scanner.
   */
  public void setScanner(Scanner s) { _scanner = s; }

  /**
   * Simple accessor method to get the default scanner.
   */
  public Scanner getScanner() { return _scanner; }

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Perform a bit of user supplied action code (supplied by generated 
   *  subclass).  Actions are indexed by an internal action number assigned
   *  at parser generation time.
   *
   * @param act_num   the internal index of the action to be performed.
   * @param parser    the parser object we are acting for.
   * @param stack     the parse stack of that object.
   * @param top       the index of the top element of the parse stack.
   */
  public abstract Symbol do_action(
    int       act_num, 
    lr_parser parser, 
    Stack     stack, 
    int       top) 
    throws java.lang.Exception;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** User code for initialization inside the parser.  Typically this 
   *  initializes the scanner.  This is called before the parser requests
   *  the first Symbol.  Here this is just a placeholder for subclasses that 
   *  might need this and we perform no action.   This method is normally
   *  overridden by the generated code using this contents of the "init with"
   *  clause as its body.
   */
  public void user_init() throws java.lang.Exception { }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Initialize the action object.  This is called before the parser does
   *  any parse actions. This is filled in by generated code to create
   *  an object that encapsulates all action code. 
   */ 
  protected abstract void init_actions() throws java.lang.Exception;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Get the next Symbol from the input (supplied by generated subclass).
   *  Once end of file has been reached, all subsequent calls to scan 
   *  should return an EOF Symbol (which is Symbol number 0).  By default
   *  this method returns getScanner().next_token(); this implementation
   *  can be overriden by the generated parser using the code declared in
   *  the "scan with" clause.  Do not recycle objects; every call to
   *  scan() should return a fresh object.
   */
  public Symbol scan() throws java.lang.Exception {
    Symbol sym = getScanner().next_token();
    return (sym!=null) ? sym : getSymbolFactory().newSymbol("END_OF_FILE",EOF_sym(),null);
  }


  /** This method is called when a syntax error has been detected and recovery 
   *  is about to be invoked.  Here in the base class we just emit a 
   *  "Syntax error" error message.  
   *
   * @param cur_token the current lookahead Symbol.
   */
  public void syntax_error(Symbol cur_token) throws Exception { }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** This method is called if it is determined that syntax error recovery 
   *  has been unsuccessful.  Here in the base class we report a fatal error. 
   *
   * @param cur_token the current lookahead Symbol.
   */
  public void unrecovered_syntax_error(Symbol cur_token) throws java.lang.Exception { }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Fetch an action from the action table.  The table is broken up into
   *  rows, one per state (rows are indexed directly by state number).  
   *  Within each row, a list of index, value pairs are given (as sequential
   *  entries in the table), and the list is terminated by a default entry 
   *  (denoted with a Symbol index of -1).  To find the proper entry in a row 
   *  we do a linear or binary search (depending on the size of the row).  
   *
   * @param state the state index of the action being accessed.
   * @param sym   the Symbol index of the action being accessed.
   */
  protected final short get_action(int state, int sym)
    {
      short tag;
      int first, last, probe;
      short[] row = action_tab[state];

      /* linear search if we are < 10 entries */
      if (row.length < 20)
        for (probe = 0; probe < row.length; probe++)
	  {
	    /* is this entry labeled with our Symbol or the default? */
	    tag = row[probe++];
	    if (tag == sym || tag == -1)
	      {
	        /* return the next entry */
	        return row[probe];
	      }
	  }
      /* otherwise binary search */
      else
	{
	  first = 0; 
	  last = (row.length-1)/2 - 1;  /* leave out trailing default entry */
	  while (first <= last)
	    {
	      probe = (first+last)/2;
	      if (sym == row[probe*2])
		return row[probe*2+1];
	      else if (sym > row[probe*2])
		first = probe+1;
	      else
	        last = probe-1;
	    }

	  /* not found, use the default at the end */
	  return row[row.length-1];
	}

      /* shouldn't happened, but if we run off the end we return the 
	 default (error == 0) */
      return 0;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Fetch a state from the reduce-goto table.  The table is broken up into
   *  rows, one per state (rows are indexed directly by state number).  
   *  Within each row, a list of index, value pairs are given (as sequential
   *  entries in the table), and the list is terminated by a default entry 
   *  (denoted with a Symbol index of -1).  To find the proper entry in a row 
   *  we do a linear search.  
   *
   * @param state the state index of the entry being accessed.
   * @param sym   the Symbol index of the entry being accessed.
   */
  protected final short get_reduce(int state, int sym)
    {
      short tag;
      short[] row = reduce_tab[state];

      /* if we have a null row we go with the default */
      if (row == null)
        return -1;

      for (int probe = 0; probe < row.length; probe++)
	{
	  /* is this entry labeled with our Symbol or the default? */
	  tag = row[probe++];
	  if (tag == sym || tag == -1)
	    {
	      /* return the next entry */
	      return row[probe];
	    }
	}
      /* if we run off the end we return the default (error == -1) */
      return -1;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** This method provides the main parsing routine.  It returns only when 
   *  done_parsing() has been called (typically because the parser has 
   *  accepted, or a fatal error has been reported).  See the header 
   *  documentation for the class regarding how shift/reduce parsers operate
   *  and how the various tables are used.
   */
public Symbol parse() throws java.lang.Exception
    {
      /* the current action code */
      int act;

      /* the Symbol/stack element returned by a reduce */
      Symbol lhs_sym = null;

      /* information about production being reduced with */
      short handle_size, lhs_sym_num;

      /* set up direct reference to tables to drive the parser */

      production_tab = production_table();
      action_tab     = action_table();
      reduce_tab     = reduce_table();

      /* initialize the action encapsulation object */
      init_actions();

      /* do user initialization */
      user_init();

      /* get the first token */
      cur_token = scan(); 

      /* push dummy Symbol with start state to get us underway */
      stack.removeAllElements();
      stack.push(getSymbolFactory().startSymbol("START", 0, start_state()));
      tos = 0;

      /* continue until we are told to stop */
      for (_done_parsing = false; !_done_parsing; )
	{
	  /* Check current token for freshness. */
	  if (cur_token.used_by_parser)
	    throw new Error("Symbol recycling detected (fix your scanner).");

	  /* current state is always on the top of the stack */

	  /* look up action out of the current state with the current input */
	  act = get_action(((Symbol)stack.peek()).parse_state, cur_token.sym);

	  /* decode the action -- > 0 encodes shift */
	  if (act > 0)
	    {
	      /* shift to the encoded state by pushing it on the stack */
	      cur_token.parse_state = act-1;
	      cur_token.used_by_parser = true;
	      stack.push(cur_token);
	      tos++;

	      /* advance to the next Symbol */
	      cur_token = scan();
	    }
	  /* if its less than zero, then it encodes a reduce action */
	  else if (act < 0)
	    {
	      /* perform the action for the reduce */
	      lhs_sym = do_action((-act)-1, this, stack, tos);

	      /* look up information about the production */
	      lhs_sym_num = production_tab[(-act)-1][0];
	      handle_size = production_tab[(-act)-1][1];

	      /* pop the handle off the stack */
	      for (int i = 0; i < handle_size; i++)
		{
		  stack.pop();
		  tos--;
		}
	      
	      /* look up the state to go to from the one popped back to */
	      act = get_reduce(((Symbol)stack.peek()).parse_state, lhs_sym_num);

	      /* shift to that state */
	      lhs_sym.parse_state = act;
	      lhs_sym.used_by_parser = true;
	      stack.push(lhs_sym);
	      tos++;
	    }
	  /* finally if the entry is zero, we have an error */
	  else if (act == 0)
	    {
	      /* call user syntax error reporting routine */
	      syntax_error(cur_token);

		  /* just in case that wasn't fatal enough, end parse */
		  done_parsing();
	    }
	}
      return lhs_sym;
    }


  /** Utility function: unpacks parse tables from strings */
  protected static short[][] unpackFromStrings(String[] sa)
    {
      // Concatanate initialization strings.
      StringBuffer sb = new StringBuffer(sa[0]);
      for (int i=1; i<sa.length; i++)
	sb.append(sa[i]);
      int n=0; // location in initialization string
      int size1 = (((int)sb.charAt(n))<<16) | ((int)sb.charAt(n+1)); n+=2;
      short[][] result = new short[size1][];
      for (int i=0; i<size1; i++) {
        int size2 = (((int)sb.charAt(n))<<16) | ((int)sb.charAt(n+1)); n+=2;
        result[i] = new short[size2];
        for (int j=0; j<size2; j++)
          result[i][j] = (short) (sb.charAt(n++)-2);
      }
      return result;
    }
}

