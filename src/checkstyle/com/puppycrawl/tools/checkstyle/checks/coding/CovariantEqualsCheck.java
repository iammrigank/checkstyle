////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2003  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle.checks.coding;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * <p>Checks that if a class defines a covariant method equals,
 * then it defines method equals(java.lang.Object).
 * Inspired by findbugs,
 * http://www.cs.umd.edu/~pugh/java/bugs/docs/findbugsPaper.pdf
 * </p>
 * <p>
 * An example of how to configure the check is:
 * </p>
 * <pre>
 * &lt;module name="CovariantEquals"/&gt;
 * </pre>
 * @author Rick Giles
 * @version 1.0
 */
public class CovariantEqualsCheck extends Check
{
    /** Set of equals method definitions */
    private Set mEqualsMethods = new HashSet();

    /** true if class defines method equals(java.lang.Object) */
    private boolean mHasEqualsObject;

    /** @see com.puppycrawl.tools.checkstyle.api.Check */
    public int[] getDefaultTokens()
    {
        return new int[] {TokenTypes.CLASS_DEF, TokenTypes.LITERAL_NEW, };
    }

    /** @see com.puppycrawl.tools.checkstyle.api.Check */
    public int[] getRequiredTokens()
    {
        return getDefaultTokens();
    }

    /** @see com.puppycrawl.tools.checkstyle.api.Check */
    public void visitToken(DetailAST aAST)
    {
        mEqualsMethods.clear();
        mHasEqualsObject = false;

        // examine method definitions for equals methods
        final DetailAST objBlock = aAST.findFirstToken(TokenTypes.OBJBLOCK);
        if (objBlock != null) {
            DetailAST child = (DetailAST) objBlock.getFirstChild();
            while (child != null) {
                if (child.getType() == TokenTypes.METHOD_DEF) {
                    if (isEqualsMethod(child)) {
                        if (hasObjectParameter(child)) {
                            mHasEqualsObject = true;
                        }
                        else {
                            mEqualsMethods.add(child);
                        }
                    }
                }
                child = (DetailAST) child.getNextSibling();
            }

            // report equals method definitions
            if (!mHasEqualsObject) {
                final Iterator it = mEqualsMethods.iterator();
                while (it.hasNext()) {
                    final DetailAST equalsAST = (DetailAST) it.next();
                    final DetailAST nameNode =
                        equalsAST.findFirstToken(TokenTypes.IDENT);
                    log(
                        nameNode.getLineNo(),
                        nameNode.getColumnNo(),
                        "covariant.equals");
                }
            }
        }
    }

    /**
     * Tests whether a method definition AST defines an equals covariant.
     * @param aAST the method definition AST to test.
     * Precondition: aAST is a TokenTypes.METHOD_DEF node.
     * @return true if aAST defines an equals covariant.
     */
    private boolean isEqualsMethod(DetailAST aAST)
    {
        // non-static, non-abstract?
        final DetailAST modifiers = aAST.findFirstToken(TokenTypes.MODIFIERS);
        if (modifiers.branchContains(TokenTypes.LITERAL_STATIC)
            || modifiers.branchContains(TokenTypes.ABSTRACT))
        {
            return false;
        }

        // named "equals"?
        final DetailAST nameNode = aAST.findFirstToken(TokenTypes.IDENT);
        final String name = nameNode.getText();
        if (!name.equals("equals")) {
            return false;
        }

        // one parameter?
        final DetailAST paramsNode = aAST.findFirstToken(TokenTypes.PARAMETERS);
        return (paramsNode.getChildCount() == 1);
    }

    /**
     * Tests whether a method definition AST has exactly one
     * parameter of type Object.
     * @param aAST the method definition AST to test.
     * Precondition: aAST is a TokenTypes.METHOD_DEF node.
     * @return true if aAST has exactly one parameter of type Object.
     */
    private boolean hasObjectParameter(DetailAST aAST)
    {
        // one parameter?
        final DetailAST paramsNode = aAST.findFirstToken(TokenTypes.PARAMETERS);
        if (paramsNode.getChildCount() != 1) {
            return false;
        }

        // parameter type "Object"?
        final DetailAST paramNode =
            paramsNode.findFirstToken(TokenTypes.PARAMETER_DEF);
        final DetailAST typeNode = paramNode.findFirstToken(TokenTypes.TYPE);
        final FullIdent fullIdent = FullIdent.createFullIdentBelow(typeNode);
        final String name = fullIdent.getText();
        return (name.equals("Object") || name.equals("java.lang.Object"));
    }
}
