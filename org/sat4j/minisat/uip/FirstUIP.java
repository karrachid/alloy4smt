/*******************************************************************************
* SAT4J: a SATisfiability library for Java Copyright (C) 2004-2008 Daniel Le Berre
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Alternatively, the contents of this file may be used under the terms of
* either the GNU Lesser General Public License Version 2.1 or later (the
* "LGPL"), in which case the provisions of the LGPL are applicable instead
* of those above. If you wish to allow use of your version of this file only
* under the terms of the LGPL, and not to allow others to use your version of
* this file under the terms of the EPL, indicate your decision by deleting
* the provisions above and replace them with the notice and other provisions
* required by the LGPL. If you do not delete the provisions above, a recipient
* may use your version of this file under the terms of the EPL or the LGPL.
* 
* Based on the original MiniSat specification from:
* 
* An extensible SAT solver. Niklas Een and Niklas Sorensson. Proceedings of the
* Sixth International Conference on Theory and Applications of Satisfiability
* Testing, LNCS 2919, pp 502-518, 2003.
*
* See www.minisat.se for the original solver in C++.
* 
*******************************************************************************/
package org.sat4j.minisat.uip;

import java.io.Serializable;

import org.sat4j.minisat.core.AssertingClauseGenerator;
import org.sat4j.specs.IConstr;

/**
 * FirstUIP scheme introduced in Chaff. Here the generator stops when a
 * syntactical criteria is met: only one literal in the current decision level
 * appears in the generated clause. The computation is done by counting the
 * literals appearing in the current decision level and decrementing that
 * counter when a resolution step is done.
 * 
 * @author leberre
 */
public class FirstUIP implements AssertingClauseGenerator, Serializable {

    private static final long serialVersionUID = 1L;

    private int counter;

    public void initAnalyze() {
        counter = 0;
    }

    public void onCurrentDecisionLevelLiteral(int p) {
        counter++;
    }

    public boolean clauseNonAssertive(IConstr reason) {
        return --counter > 0;
    }

    @Override
    public String toString() {
        return "Stops conflict analysis at the first Unique Implication Point";
    }

}
