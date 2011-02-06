package de.psi.alloy4smt.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.ConstList.TempList;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorSyntax;
import edu.mit.csail.sdg.alloy4compiler.ast.Attr;
import edu.mit.csail.sdg.alloy4compiler.ast.Command;
import edu.mit.csail.sdg.alloy4compiler.ast.CommandScope;
import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprBinary;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprCall;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprConstant;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprITE;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprLet;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprList;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprQt;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprUnary;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprVar;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.Field;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.PrimSig;
import edu.mit.csail.sdg.alloy4compiler.ast.VisitReturn;
import edu.mit.csail.sdg.alloy4compiler.parser.CompModule;


public class IntRefPreprocessor {
    public final ConstList<Sig> sigs;
    public final Sig.PrimSig intref;
    public final ConstList<Command> commands;
	public final Expr facts;
	public final ConstList<String> hysatExprs;
	public final ConstList<ConstList<String>> intrefAtoms;

    
    public static interface SigBuilder {
    	public Sig makeSig() throws Err;
    	
    	public void addFactor(Sig factor);
    }
        
    private IntRefPreprocessor(Computer computer, FactRewriter rewriter) {
    	sigs = computer.sigs.makeConst();
    	intref = computer.intref;
    	commands = computer.commands.makeConst();
    	facts = rewriter.getFacts();
    	hysatExprs = rewriter.getHysatExprs();
    	
    	final ConstList<String> factIntRefAtoms = rewriter.getIntExprAtoms();
    	TempList<ConstList<String>> atoms = new TempList<ConstList<String>>();
    	for (int i = 0; i < commands.size(); ++i) {
    		TempList<String> l = new TempList<String>();
    		l.addAll(computer.intrefAtoms.get(i));
    		l.addAll(factIntRefAtoms);
    		atoms.add(l.makeConst());
    	}
    	intrefAtoms = atoms.makeConst();
    }
    
    private IntRefPreprocessor(CompModule module) {
    	sigs = module.getAllReachableSigs();
    	intref = null;
    	commands = module.getAllCommands();
    	facts = module.getAllReachableFacts();
    	hysatExprs = null;
    	intrefAtoms = null;
    }
    
    private static class Computer implements SigBuilder {
    	private ConstList<Command> oldcommands;
    	private Sig currentSig;
    	private Sig.Field currentField;
    	private Sig.Field lastfield = null;
    	private int fieldcnt = 0;
    	private Map<Command, Integer> factors;
    	private Map<Command, List<CommandScope>> newscopes;
    	private Map<Command, TempList<String>> tmpIntrefAtoms;
    	private List<Sig> newintrefs;
    	
    	public TempList<Sig> sigs;
    	public Sig.PrimSig intref;
    	public TempList<Command> commands;
    	public TempList<ConstList<String>> intrefAtoms;
    	
    	public Computer(CompModule module, Sig.PrimSig intref) throws Err {
    		this.intref = intref;
    		sigs = new TempList<Sig>();
    		oldcommands = module.getAllCommands();
    		commands = new TempList<Command>();
    		factors = new HashMap<Command, Integer>();
    		newscopes = new HashMap<Command, List<CommandScope>>();
    		newintrefs = new Vector<Sig>();
    		tmpIntrefAtoms = new HashMap<Command, TempList<String>>();
    		intrefAtoms = new TempList<ConstList<String>>();
    		
    		for (Command c: oldcommands) {
    			newscopes.put(c, new Vector<CommandScope>());
    			tmpIntrefAtoms.put(c, new TempList<String>());
    		}
    		    		
    		for (Sig s: module.getAllReachableSigs()) {
    			if (s.builtin) {
    				sigs.add(s);
    			} else {
    				sigs.add(convertSig(s));
    			}
    		}
    		
    		for (Command c: oldcommands) {
    			TempList<CommandScope> scopes = new TempList<CommandScope>();
    			scopes.addAll(c.scope);
    			scopes.addAll(newscopes.get(c));
    			commands.add(c.change(scopes.makeConst()));
    			intrefAtoms.add(tmpIntrefAtoms.get(c).makeConst());
    		}
    	}
    	
    	@Override
    	public void addFactor(Sig factor) {
    		for (Command c: oldcommands) {
    			CommandScope scope = c.getScope(factor);
    			int mult;
    			if (scope != null) {
    				mult = c.getScope(factor).endingScope;
    			} else if (factor.isOne != null || factor.isLone != null) {
    				mult = 1;
    			} else {
    				mult = c.overall < 0 ? 1 : c.overall;
    			}
    			factors.put(c, factors.get(c) * mult);
    		}
    	}
    	
    	private void resetFactors() {
    		for (Command c: oldcommands) {
    			factors.put(c, 1);
    		}
    	}

    	@Override
    	public Sig makeSig() throws Err {
    		if (lastfield != currentField) {
    			fieldcnt = 0;
    			lastfield = currentField;
    		} else {
    			fieldcnt++;
    		}
    		String label = currentSig.label + "$" + currentField.label + "$IntRef" + fieldcnt;
    		Sig sig = new Sig.PrimSig(label, intref);
			newintrefs.add(sig);
			
			return sig;
    	}
    	
    	private static String atomize(Sig sig, int id) {
    		String label = sig.label;
    		if (label.startsWith("this/"))
    			label = label.substring(5);
    		return label + "$" + id;
    	}
    	
    	private void integrateNewIntRefSigs() throws ErrorSyntax {
    		for (Sig sig: newintrefs) {
	    		for (Command c: oldcommands) {
	    			final int scope = factors.get(c);
	    			newscopes.get(c).add(new CommandScope(sig, false, scope));
	    			for (int i = 0; i < scope; ++i) {
	    				tmpIntrefAtoms.get(c).add(atomize(sig, i));
	    			}
	    		}
	    		sigs.add(sig);
    		}
    		newintrefs.clear();
    	}
    	
        private Sig convertSig(Sig sig) throws Err {
        	boolean newSigNeeded = false;
        	Attr[] attrs = new Attr[1];
        	Sig newSig = new Sig.PrimSig(sig.label, sig.attributes.toArray(attrs));        	
        	
        	currentSig = sig;
        	
        	for (Sig.Field field: sig.getFields()) {
            	resetFactors();
            	addFactor(sig);
            	
        		currentField = field;
        		Expr oldExpr = field.decl().expr;
        		Expr newExpr = convertExpr(oldExpr, this);
        		newSig.addTrickyField(field.pos, field.isPrivate, null, null, field.isMeta, 
        				              new String[] { field.label }, newExpr);
        		if (oldExpr != newExpr)
        			newSigNeeded = true;
        		
        		integrateNewIntRefSigs();
        	}
        	
        	return newSigNeeded ? newSig : sig;
        }
    }
    
    private static interface IntexprSigBuilder {
    	public Sig.PrimSig make() throws Err;
    }
    
    private static class IntExprHandler extends VisitReturn<String> {
    	
    	private List<Expr> facts;
    	private Sig.Field aqclass;
    	private IntexprSigBuilder builder;
    	
    	public IntExprHandler(Sig.Field aqclass, IntexprSigBuilder isb) {
			this.facts = new Vector<Expr>();
			this.aqclass = aqclass;
			this.builder = isb;
		}
    	
    	public Expr getFacts() {
    		return ExprList.make(null, null, ExprList.Op.AND, facts);
    	}
    	
    	private void throwUnsupportedOperator(Expr x) throws Err {
    		throw new ErrorSyntax(x.pos, "HySAT does not support this operator.");
    	}

		@Override
		public String visit(ExprUnary x) throws Err {
			String result;
			
			if (x.op == ExprUnary.Op.CAST2INT) {
				final Sig.PrimSig exprsig = builder.make();
				final Expr a = ExprBinary.Op.JOIN.make(null, null, exprsig, aqclass);
				final Expr b = ExprBinary.Op.JOIN.make(null, null, x.sub, aqclass);
				facts.add(ExprBinary.Op.EQUALS.make(null, null, a, b));
				result = exprsig.label;				
			} else {	
				final String sub = visitThis(x.sub);
				switch (x.op) {
				case NOT: result = "!" + sub; break;
				case NOOP: result = sub; break;
				default:
					throw new AssertionError();
				}
			}

			return result;
		}

		@Override
		public String visit(ExprBinary x) throws Err {
			final String left = visitThis(x.left);
			final String right = visitThis(x.right);
			String op = null;
			
			switch (x.op) {
			case GT:         op = ">"; break;
			case LT:         op = "<"; break;
			case GTE:        op = ">="; break;
			case LTE:        op = "<="; break;
			case EQUALS:     op = "="; break;
			case MINUS:      op = "-"; break;
			case MUL:        op = "*"; break;
			case NOT_EQUALS: op = "!="; break;
			case NOT_GT:     op = "<="; break;
			case NOT_LT:     op = ">="; break;
			case NOT_GTE:    op = "<"; break;
			case NOT_LTE:    op = ">"; break;
			case PLUS:       op = "+"; break;
			
			default:
				throwUnsupportedOperator(x);
			}

			return "(" + left + " " + op + " " + right + ")";
		}

		@Override
		public String visit(ExprConstant x) throws Err {
			if (x.op == ExprConstant.Op.NUMBER) {
				return String.valueOf(x.num);
			} else {
				throw new ErrorSyntax(x.pos, "Constant not convertible to HySAT");
			}
		}

		@Override
		public String visit(ExprList x) throws Err {
			throw new AssertionError();
		}

		@Override
		public String visit(ExprCall x) throws Err {
			throw new AssertionError();
		}

		@Override
		public String visit(ExprITE x) throws Err {
			throw new AssertionError();
		}

		@Override
		public String visit(ExprLet x) throws Err {
			throw new AssertionError();
		}

		@Override
		public String visit(ExprQt x) throws Err {
			throw new AssertionError();
		}

		@Override
		public String visit(ExprVar x) throws Err {
			throw new AssertionError();
		}

		@Override
		public String visit(Sig x) throws Err {
			throw new AssertionError();
		}

		@Override
		public String visit(Field x) throws Err {
			throw new AssertionError();
		}

    }
    
    private static class FactRewriter extends VisitReturn<Expr> {
    	
    	private final Sig.PrimSig intref;
    	private final Sig.Field aqclass;
    	private final IntexprSigBuilder intexprBuilder;
    	private List<Sig.PrimSig> intexprs;
    	
    	private Expr rewritten;
    	private TempList<String> hysatexprs;
    	
    	private FactRewriter(Sig.PrimSig intref_) {
    		intref = intref_;
    		aqclass = Helpers.getFieldByName(intref.getFields(), "aqclass");
    		intexprs = new Vector<Sig.PrimSig>();
    		hysatexprs = new TempList<String>();
    		intexprBuilder = new IntexprSigBuilder() {
    			private int id = 0;
				@Override
				public PrimSig make() throws Err {
					final PrimSig result = new PrimSig("intexpr_" + id++, intref, Attr.ONE);
					intexprs.add(result);
					return result;
				}
			};
		}
    	
    	public static FactRewriter rewrite(Expr expr, Sig.PrimSig intref) throws Err {
    		FactRewriter rewriter = new FactRewriter(intref);
    		rewriter.rewritten = rewriter.visitThis(expr);
    		return rewriter;
    	}
    	
    	public Expr getFacts() {
    		return rewritten;
    	}
    	
    	public ConstList<String> getHysatExprs() {
    		return hysatexprs.makeConst();
    	}
    	
    	public ConstList<String> getIntExprAtoms() {
    		TempList<String> result = new TempList<String>();
    		for (Sig.PrimSig sig : intexprs) {
    			result.add(sig.label + "$0");
    		}
    		return result.makeConst();
    	}

		@Override
		public Expr visit(ExprBinary x) throws Err {
			Expr result = null;
			
			if (x.left.type().is_int && x.right.type().is_int) {
				final IntExprHandler ieh = new IntExprHandler(aqclass, intexprBuilder);
				final String hexpr = ieh.visitThis(x);
				hysatexprs.add(hexpr);
				result = ieh.getFacts();
			} else {
				final Expr left = visitThis(x.left);
				final Expr right = visitThis(x.right);
				result = x.op.make(x.pos, x.closingBracket, left, right);					
			}
			
			return result;
		}

		@Override
		public Expr visit(ExprList x) throws Err {
			TempList<Expr> args = new TempList<Expr>();
			for (Expr e: x.args) {
				args.add(visitThis(e));
			}
			return ExprList.make(x.pos, x.closingBracket, x.op, args.makeConst());
		}

		@Override
		public Expr visit(ExprCall x) throws Err {
			TempList<Expr> args = new TempList<Expr>();
			for (Expr e: x.args) {
				args.add(visitThis(e));
			}
			return ExprCall.make(x.pos, x.closingBracket, x.fun, args.makeConst(), x.extraWeight);
		}

		@Override
		public Expr visit(ExprConstant x) throws Err {
			return x;
		}

		@Override
		public Expr visit(ExprITE x) throws Err {
			Expr cond = visitThis(x.cond);
			Expr left = visitThis(x.left);
			Expr right = visitThis(x.right);
			return ExprITE.make(x.pos, cond, left, right);
		}

		@Override
		public Expr visit(ExprLet x) throws Err {
			Expr sub = visitThis(x.sub);
			return ExprLet.make(x.pos, x.var, x.expr, sub);
		}

		@Override
		public Expr visit(ExprQt x) throws Err {
			Expr sub = visitThis(x.sub);
			return x.op.make(x.pos, x.closingBracket, x.decls, sub);
		}

		@Override
		public Expr visit(ExprUnary x) throws Err {
			final Expr sub = visitThis(x.sub);
			if (x.op == ExprUnary.Op.CAST2INT) {
				throw new AssertionError();
//				Sig.PrimSig exprsig = new Sig.PrimSig("intexpr_0", intref, Attr.ONE);
//				intexprs.add(exprsig);
//				return ExprBinary.Op.JOIN.make(null, null, sub, aqclass);
			} else {
				return x.op.make(x.pos, sub);
			}
		}

		@Override
		public Expr visit(ExprVar x) throws Err {
			return x;
		}

		@Override
		public Expr visit(Sig x) throws Err {
			return x;
		}

		@Override
		public Expr visit(Field x) throws Err {
			return x;
		}
    	
    }
    

    public static IntRefPreprocessor processModule(CompModule module) throws Err {
    	final Sig.PrimSig intref = (Sig.PrimSig) Helpers.getSigByName(module.getAllReachableSigs(), "intref/IntRef");
    	if (intref != null) {
    		final Computer computer = new Computer(module, intref);
    		final FactRewriter rewriter = FactRewriter.rewrite(module.getAllReachableFacts(), intref);
    		return new IntRefPreprocessor(computer, rewriter);
    	} else {
    		return new IntRefPreprocessor(module);
    	}
    }

    public static Expr convertExpr(Expr expr, SigBuilder builder) throws Err {
        Expr result = expr;

        if (expr == Sig.SIGINT) {
            result = builder.makeSig();
        } else if (expr instanceof ExprUnary) {
            ExprUnary unary = (ExprUnary) expr;
            Expr newSub = convertExpr(unary.sub, builder);
            if (newSub != unary.sub) {
                result = unary.op.make(unary.pos, newSub);
            }
        } else if (expr instanceof ExprBinary) {
            ExprBinary binary = (ExprBinary) expr;
            Expr newLeft = convertExpr(binary.left, builder);
            Expr newRight = convertExpr(binary.right, builder);
            if (newLeft != binary.left || newRight != binary.right) {
                result = binary.op.make(binary.pos, binary.closingBracket, newLeft, newRight);
            }
        } else if (expr instanceof Sig) {
        	builder.addFactor((Sig) expr);
        }

        return result;
    }
}
