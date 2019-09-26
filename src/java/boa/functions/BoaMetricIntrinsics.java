/*
 * Copyright 2014, Hridesh Rajan, Robert Dyer, 
 *                 and Iowa State University of Science and Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package boa.functions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.sun.org.apache.bcel.internal.generic.NEW;

import boa.runtime.BoaAbstractVisitor;
import boa.types.Ast.*;
import boa.types.Ast.Expression.ExpressionKind;

/**
 * Boa domain-specific functions for computing software engineering metrics.
 * 
 * @author rdyer
 */
public class BoaMetricIntrinsics {
	////////////////////////////////
	// Number of Attributes (NOA) //
	////////////////////////////////

	private static class BoaNOAVisitor extends BoaCountingVisitor {
		@Override
		public boolean preVisit(final Declaration node) {
			if (node.getKind() == TypeKind.CLASS)
				count += node.getFieldsCount();
			return true;
		}
	}

	private static BoaNOAVisitor noaVisitor = new BoaNOAVisitor();

	/**
	 * Computes the Number of Attributes (NOA) metric for a node.
	 * 
	 * @param node the node to compute NOA for
	 * @return the NOA value for decl
	 */
	@FunctionSpec(name = "get_metric_noa", returnType = "int", formalParameters = { "Declaration" })
	public static long getMetricNOA(final Declaration node) throws Exception {
		noaVisitor.initialize().visit(node);
		return noaVisitor.count;
	}

	////////////////////////////////
	// Number of Operations (NOO) //
	////////////////////////////////

	private static class BoaNOOVisitor extends BoaCountingVisitor {
		@Override
		public boolean preVisit(final Declaration node) {
			if (node.getKind() == TypeKind.CLASS)
				count += node.getMethodsCount();
			return true;
		}
	}

	private static BoaNOOVisitor nooVisitor = new BoaNOOVisitor();

	/**
	 * Computes the Number of Operations (NOO) metric for a node.
	 * 
	 * @param node the node to compute NOO for
	 * @return the NOO value for decl
	 */
	@FunctionSpec(name = "get_metric_noo", returnType = "int", formalParameters = { "Declaration" })
	public static long getMetricNOO(final Declaration node) throws Exception {
		nooVisitor.initialize().visit(node);
		return nooVisitor.count;
	}

	////////////////////////////////////
	// Number of Public Methods (NPM) //
	////////////////////////////////////

	private static class BoaNPMVisitor extends BoaCountingVisitor {
		@Override
		public boolean preVisit(final Method node) {
			if (BoaModifierIntrinsics.hasModifierPublic(node))
				count++;
			return true;
		}
	}

	private static BoaNPMVisitor npmVisitor = new BoaNPMVisitor();

	/**
	 * Computes the Number of Public Methods (NPM) metric for a node.
	 * 
	 * @param node the node to compute NPM for
	 * @return the NPM value for decl
	 */
	@FunctionSpec(name = "get_metric_npm", returnType = "int", formalParameters = { "Declaration" })
	public static long getMetricNPM(final Declaration node) throws Exception {
		npmVisitor.initialize().visit(node);
		return npmVisitor.count;
	}

	/////////////////////////////////////
	// Weighted Methods per Class(WMC) //
	/////////////////////////////////////

	private static class BoaWMCVisitor extends BoaCountingVisitor {
		
		private int methodCC;
		
		private BoaAbstractVisitor visitor = new BoaAbstractVisitor() {
			@Override
			public boolean preVisit(final Method node) throws Exception {
				methodCC = 0;
				for (Statement s : node.getStatementsList()) {
					// check for, do, while, if, case, catch
					switch (s.getKind()) {
						case FOR:
						case DO:
						case WHILE:
						case IF:
						case CASE:
						case CATCH:
							methodCC++;
							break;
						default:
							break;
					}
					visit(s);
				}
				methodCC++;
				return false;
			}
			
			@Override
			public boolean preVisit(final Expression node) {
				// check ||
				if (node.getKind() == ExpressionKind.LOGICAL_OR)
					methodCC++;
				return true;
			}
		};
		
		@Override
		public boolean preVisit(final Declaration node) throws Exception {
			for (Method m : node.getMethodsList()) {
				visitor.visit(m);
				count += methodCC;
			}
			return false;
		}
		
		
	}

	private static BoaWMCVisitor wmcVisitor = new BoaWMCVisitor();

	/**
	 * Compute the complexity of a class as the sum of the McCabe’s cyclomatic complexity of
	 * its methods
	 * 
	 * @param node the node to compute DIT for
	 * @return the WMC value for node
	 */
	@FunctionSpec(name = "get_metric_wmc", returnType = "int", formalParameters = { "Declaration" })
	public static long getMetricWMC(final Declaration node) throws Exception {
		wmcVisitor.initialize().visit(node);
		return wmcVisitor.count;
	}

	/////////////////////////////////////
	// Depth of Inheritance Tree (DIT) //
	/////////////////////////////////////

	private static class BoaDITVisitor extends BoaCountingVisitor {
		// TODO
	}

	private static BoaDITVisitor ditVisitor = new BoaDITVisitor();

	/**
	 * Computes the Depth of Inheritance Tree (DIT) metric for a node.
	 * 
	 * @param node the node to compute DIT for
	 * @return the DIT value for node
	 */
	@FunctionSpec(name = "get_metric_dit", returnType = "int", formalParameters = { "Declaration" })
	public static long getMetricDIT(final Declaration node) throws Exception {
		ditVisitor.initialize().visit(node);
		return ditVisitor.count;
	}

	////////////////////////////////
	// Number of Children (NOC) //
	////////////////////////////////

	private static class BoaNOCVisitor extends BoaCollectingVisitor<String, Long> {
		private String ns;

		@Override
		protected boolean preVisit(Namespace node) throws Exception {
			this.ns = node.getName();
			return super.preVisit(node);
		}

		@Override
		protected boolean preVisit(Declaration node) throws Exception {
			for (final Type t : node.getParentsList()) {
				final String key = ns + "." + t.getName();
				final long val = map.containsKey(key) ? map.get(key) : 0;
				map.put(key, val + 1);
			}
			return super.preVisit(node);
		}
	}

	private static BoaNOCVisitor nocVisitor = new BoaNOCVisitor();

	/**
	 * (Partially) Computes the Number of Children (NOC) metric.
	 * 
	 * @param node the node to compute NOC for
	 * @return a map containing partial computation of the NOC metric
	 */
	@FunctionSpec(name = "get_metric_noc", returnType = "map[string] of int", formalParameters = { "ASTRoot" })
	public static HashMap<String, Long> getMetricNOC(final ASTRoot node) throws Exception {
		nocVisitor.initialize(new HashMap<String, Long>()).visit(node);
		return nocVisitor.map;
	}

	////////////////////////////////
	// Response For a Class (RFC) //
	////////////////////////////////

	private static class BoaRFCVisitor extends BoaCountingVisitor {
		
		private HashSet<String> methodSet;
		
		private BoaAbstractVisitor visitor = new BoaAbstractVisitor() {
			@Override
			public boolean preVisit(final Expression node) {
				if (node.getKind() == ExpressionKind.METHODCALL)
					methodSet.add(node.getMethod() + " " + node.getMethodArgsCount());
				if (node.getKind() == ExpressionKind.NEW)
					methodSet.add(node.getNewType().getName() + " " + node.getMethodArgsCount());
				return true;
			}
		};
		
		@Override
		public boolean preVisit(final Declaration node) throws Exception {
			methodSet = new HashSet<String>();
			for (Variable v : node.getFieldsList())
				visitor.visit(v);
			for (Method m : node.getMethodsList())
				visitor.visit(m);
			count = methodSet.size();
			methodSet.clear();
			return false;
		}

	}

	private static BoaRFCVisitor rfcVisitor = new BoaRFCVisitor();

	/**
	 * Computes the number of distinct methods and constructors invoked by a class 
	 * 
	 * @param node the node to compute RFC for
	 * @return the RFC value for node
	 */
	@FunctionSpec(name = "get_metric_rfc", returnType = "int", formalParameters = { "Declaration" })
	public static long getMetricRFC(final Declaration node) throws Exception {
		rfcVisitor.initialize().visit(node);
		return rfcVisitor.count;
	}

	////////////////////////////////////
	// Coupling Between Object (CBO) //
	////////////////////////////////////

	private static class BoaCBOVisitor extends BoaCollectingVisitor<String, Long> {
		
		private String curFQN;
		private HashMap<String, Declaration> declMap;
		private HashMap<String, HashSet<String>> references;
		private HashMap<String, HashSet<String>> referenced;
		
		private BoaAbstractVisitor visitor = new BoaAbstractVisitor() {
			private String getReference(String reference) {
				if (declMap.containsKey(reference))
					return declMap.get(reference).getFullyQualifiedName();
				int idx = reference.lastIndexOf('.');
				if (idx > 0) {
					String suffix = reference.substring(idx + 1);
					if (declMap.containsKey(suffix))
						return declMap.get(suffix).getFullyQualifiedName();
				}
				return null;
			}
			
			private void updateMaps(String reference) {
				if (!references.containsKey(curFQN))
					references.put(curFQN, new HashSet<String>());
				references.get(curFQN).add(reference);
				if (!referenced.containsKey(reference))
					referenced.put(reference, new HashSet<String>());
				referenced.get(reference).add(curFQN);
			}
			
			@Override
			public boolean preVisit(final Expression node) {
				if (node.getKind() == ExpressionKind.VARACCESS) {
					String reference = getReference(node.getVariable());
					if (reference != null)
						updateMaps(reference);
				}
				return true;
			}
			
			@Override
			public boolean preVisit(final Variable node) {
				String reference = getReference(node.getName());
				if (reference != null)
					updateMaps(reference);
				return true;
			}
			
			@Override
			public boolean preVisit(final Type node) {
				String reference = getReference(node.getName());
				if (reference != null)
					updateMaps(reference);
				return true;
			}
		};
		
		public void process(HashMap<String, Declaration> decls) throws Exception {
			declMap = decls;
			references = new HashMap<String, HashSet<String>>();
			referenced = new HashMap<String, HashSet<String>>();
			for (Declaration node : decls.values()) {
				curFQN = node.getFullyQualifiedName();
				for (Variable v : node.getFieldsList())
					visitor.visit(v);
				for (Method m : node.getMethodsList())
					visitor.visit(m);
			}
			for (Declaration node : decls.values()) {
				String fqn = node.getFullyQualifiedName();
				HashSet<String> union = new HashSet<String>();
				if (references.containsKey(fqn))
					union.addAll(references.get(fqn));
				if (referenced.containsKey(fqn))
					union.addAll(referenced.get(fqn));
				map.put(fqn, (long) union.size());
			}
		}
		
	}

	private static BoaCBOVisitor cboVisitor = new BoaCBOVisitor();

	/**
	 * Computes the number of classes to which a class is coupled.
	 * 
	 * @param node the node to compute CBO for
	 * @return the CBO value for node
	 */
	@FunctionSpec(name = "get_metric_cbo", returnType = "map[string] of int", formalParameters = { "map[string] of Declaration" })
	public static HashMap<String, Long> getMetricCBO(final HashMap<String, Declaration> decls) throws Exception {
		cboVisitor.initialize(new HashMap<String, Long>());
		cboVisitor.process(decls);
		return cboVisitor.map;
	}

	///////////////////////////////////////////
	// Lack of Cohesion in Methods (LCOM) //
	///////////////////////////////////////////

	private static class BoaLCOMVisitor extends BoaCountingVisitor {
		
		private HashSet<String> declarationVars;
		private HashSet<String> methodVars;
		private double numAccesses;
		private double lcom;

		private BoaAbstractVisitor methodVisitor = new BoaAbstractVisitor() {
			@Override
			public boolean preVisit(final Method node) throws Exception {
				for (Statement s : node.getStatementsList())
					visit(s);
				return false;
			}
			
			@Override
			public boolean preVisit(final Variable node) {
				if (declarationVars.contains(node.getName()))
					methodVars.add(node.getName());
				return true;
			}
			
			@Override
			public boolean preVisit(final Expression node) throws Exception {
				if (node.getKind() == ExpressionKind.VARACCESS && declarationVars.contains(node.getVariable()))		
					methodVars.add(node.getVariable());
				return true;
			}
		};
		
		public double getLCOM() {
			return lcom;
		}
		
		@Override
		public boolean preVisit(final Declaration node) throws Exception {
			double fieldsCount = node.getFieldsCount();
			double methodsCount = node.getMethodsCount();
			if (fieldsCount == 0 || methodsCount < 2) {
				lcom = 0.0;
			} else {
				declarationVars = new HashSet<String>();
				methodVars = new HashSet<String>();
				numAccesses = 0;
				for (Variable v : node.getFieldsList())
					declarationVars.add(v.getName());
				for (Method m : node.getMethodsList()) {
					methodVisitor.visit(m);
					numAccesses += methodVars.size();
					methodVars.clear();
				}
				lcom = (methodsCount - numAccesses / fieldsCount) / (methodsCount - 1.0);
				declarationVars.clear();
				methodVars.clear();
			}
			return false;
		}

	}

	private static BoaLCOMVisitor lcooVisitor = new BoaLCOMVisitor();

	/**
	 * Computes the Lack of Cohesion in Methods (LCOM) metric for a node.
	 * The higher the pairs of methods in a class sharing at least a field,
	 * the higher its cohesion
	 * 
	 * @param node the node to compute LCOM for
	 * @return the LCOM value for node
	 */
	@FunctionSpec(name = "get_metric_lcom", returnType = "float", formalParameters = { "Declaration" })
	public static double getMetricLCOM(final Declaration node) throws Exception {
		lcooVisitor.initialize().visit(node);
		return lcooVisitor.getLCOM();
	}

	////////////////////////////
	// Afferent Coupling (CA) //
	////////////////////////////

	private static class BoaCAVisitor extends BoaCountingVisitor {
		// TODO
	}

	private static BoaCAVisitor caVisitor = new BoaCAVisitor();

	/**
	 * Computes the Afferent Coupling (CA) metric for a node.
	 * 
	 * @param node the node to compute CA for
	 * @return the CA value for node
	 */
	@FunctionSpec(name = "get_metric_ca", returnType = "int", formalParameters = { "Declaration" })
	public static long getMetricCA(final Declaration node) throws Exception {
		caVisitor.initialize().visit(node);
		return caVisitor.count;
	}
}
